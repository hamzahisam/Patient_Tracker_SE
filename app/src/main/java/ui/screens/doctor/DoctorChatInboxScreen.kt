package com.example.patienttracker.ui.screens.doctor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.auth.UserProfile
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import com.google.firebase.firestore.Query
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack


private const val TAG = "DoctorChatInbox"

data class DoctorConversation(
    val conversationId: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorChatInboxScreen(
    navController: NavController
) {
    val db = remember { Firebase.firestore }

    var loading by remember { mutableStateOf(true) }
    var conversations by remember { mutableStateOf<List<DoctorConversation>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val profile: UserProfile? = AuthManager.getCurrentUserProfile()
            val doctorId = profile?.humanId

            if (doctorId.isNullOrBlank()) {
                Log.w(TAG, "Doctor profile not found or humanId empty: $profile")
                errorMessage = "Doctor profile not found."
                loading = false
                return@LaunchedEffect
            }

            Log.d(TAG, "Loading doctor inbox for doctorId=$doctorId")

            // Use appointments to determine linked patients
            db.collection("appointments")
                .whereEqualTo("status", "booked")
                .whereEqualTo("doctorId", doctorId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Snapshot error", error)
                        errorMessage = error.message
                        loading = false
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        Log.d(TAG, "No booked appointments for this doctor.")
                        conversations = emptyList()
                        loading = false
                        return@addSnapshotListener
                    }

                    // Map keyed by patientId so we keep one row per patient
                    val map = linkedMapOf<String, DoctorConversation>()

                    for (doc in snapshot.documents) {
                        val patientId = doc.getString("patientId") ?: continue

                        val patientFirst = doc.getString("patientFirstName") ?: ""
                        val patientLast = doc.getString("patientLastName") ?: ""
                        val fullName = listOf(patientFirst, patientLast)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { "Patient $patientId" }

                        // Conversation id must match ChatScreen logic
                        val conversationId =
                            if (patientId <= doctorId) "${patientId}_${doctorId}" else "${doctorId}_${patientId}"

                        // Base row – will be enriched with last message below
                        val base = DoctorConversation(
                            conversationId = conversationId,
                            patientId = patientId,
                            patientName = fullName,
                            lastMessage = "",
                            lastMessageTime = 0L
                        )
                        map[patientId] = base

                        // Listen for the latest message for this conversation
                        db.collection("conversations")
                            .document(conversationId)
                            .collection("messages")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .addSnapshotListener { msgSnap, msgErr ->
                                if (msgErr != null) {
                                    Log.w(TAG, "Message listener error for $conversationId", msgErr)
                                    return@addSnapshotListener
                                }

                                val msgDoc = msgSnap?.documents?.firstOrNull() ?: run {
                                    // no messages yet for this convo – keep base row
                                    conversations = map.values
                                        .sortedWith(
                                            compareByDescending<DoctorConversation> { it.lastMessageTime }
                                                .thenBy { it.patientName }
                                        )
                                    return@addSnapshotListener
                                }

                                val text = msgDoc.getString("text") ?: ""
                                val ts = msgDoc.getTimestamp("timestamp")?.toDate()?.time ?: 0L

                                val existing = map[patientId] ?: base
                                map[patientId] = existing.copy(
                                    lastMessage = text,
                                    lastMessageTime = ts
                                )

                                conversations = map.values
                                    .sortedWith(
                                        compareByDescending<DoctorConversation> { it.lastMessageTime }
                                            .thenBy { it.patientName }
                                    )
                            }
                    }

                    // Initial list (before message listeners fire)
                    conversations = map.values
                        .sortedBy { it.patientName }

                    loading = false
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading doctor inbox", e)
            errorMessage = e.message
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF4CB7C2)
                        )
                    }
                },
                title = {
                    Text(
                        "Patient Chats",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF4CB7C2)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color(0xFF4CB7C2)
                )
            )
        },
            bottomBar = { DoctorBottomBar(navController, selectedTab = 2) },
            contentWindowInsets = WindowInsets.systemBars.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            )

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Error",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                conversations.isEmpty() -> {
                    Text(
                        text = "No patient messages yet.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(conversations) { conv ->
                            PatientChatRow(
                                conversation = conv,
                                onClick = {
                                    // For doctor, ChatScreen expects the OTHER party id → patientHumanId
                                    navController.navigate("chat_doctor/${conv.patientId}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientChatRow(
    conversation: DoctorConversation,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle with initials
            val initials = conversation.patientName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFE0F2F8), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF14597A)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conversation.patientName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (conversation.lastMessage.isNotBlank()) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}