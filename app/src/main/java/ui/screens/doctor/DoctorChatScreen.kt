package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.auth.AuthManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars

private const val TAG = "DoctorChatScreen"

data class DoctorChatMessage(
    val id: String,
    val text: String,
    val timestamp: Date?,
    val isSentByMe: Boolean,
    val status: String
)

private fun formatTime(date: Date?): String {
    if (date == null) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorChatScreen(
    navController: NavController,
    context: Context,
    patientId: String,
    patientName: String = "Patient"
) {
    val scope = rememberCoroutineScope()

    var doctorId by remember { mutableStateOf<String?>(null) }
    var doctorFirstName by remember { mutableStateOf("") }
    var doctorLastName by remember { mutableStateOf("") }

    // Messages for this conversation
    val messages = remember { mutableStateListOf<DoctorChatMessage>() }
    var inputText by remember { mutableStateOf("") }

    // Load current doctor profile once
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            if (profile != null) {
                doctorId = profile.humanId
                doctorFirstName = profile.firstName
                doctorLastName = profile.lastName
            } else {
                Log.e(TAG, "Doctor profile is null – cannot open chat")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load doctor profile", e)
        }
    }

    // ✅ Shared conversation id (must match patient ChatScreen)
    val conversationId = remember(doctorId, patientId) {
        val dId = doctorId
        if (!dId.isNullOrBlank() && patientId.isNotBlank()) {
            "${dId}_${patientId}"   // same pattern as patient: doctorId_patientId
        } else {
            ""
        }
    }

    // ✅ Attach Firestore listener only when conversationId is valid
    DisposableEffect(conversationId) {
        if (conversationId.isBlank()) {
            onDispose { }
        } else {
            val db = FirebaseFirestore.getInstance()

            Log.d(TAG, "Listening to conversation: $conversationId")

            val registration = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snap, e ->
                    if (e != null) {
                        Log.e(TAG, "Listener error", e)
                        return@addSnapshotListener
                    }
                    if (snap == null) return@addSnapshotListener

                    Log.d(TAG, "Got ${snap.size()} messages for $conversationId")

                    val newList = snap.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: return@mapNotNull null
                        val ts = doc.getTimestamp("timestamp") ?: Timestamp.now()
                        val senderRole = doc.getString("senderRole") ?: "patient"
                        val status = doc.getString("status") ?: "sent"

                        DoctorChatMessage(
                            id = doc.id,
                            text = text,
                            timestamp = ts.toDate(),
                            isSentByMe = senderRole == "doctor",
                            status = status
                        )
                    }

                    messages.clear()
                    messages.addAll(newList)
                }

            onDispose {
                registration.remove()
            }
        }
    }

    // ✅ sendMessage uses same conversationId and checks it's not blank
    fun sendMessage() {
        val dId = doctorId
        if (dId.isNullOrBlank() || patientId.isBlank()) return
        if (conversationId.isBlank()) return
        if (inputText.isBlank()) return

        val db = FirebaseFirestore.getInstance()

        val msgData = hashMapOf(
            "text" to inputText.trim(),
            "timestamp" to Timestamp.now(),
            "senderRole" to "doctor",
            "doctorId" to dId,
            "patientId" to patientId,
            "status" to "sent"
        )

        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .add(msgData)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent from doctor to $conversationId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send message", e)
            }

        inputText = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = patientName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("\u2190", color = Color.White, fontSize = 18.sp)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0EA5B8)
                )
            )
        },
        bottomBar = { DoctorBottomBar(navController, selectedTab = 2) },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF7FBFF))
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    MessageRow(msg)
                }
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(50.dp)),
                    placeholder = { Text("Type a message...") },
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.width(8.dp))

                IconButton(onClick = { scope.launch { sendMessage() } }) {
                    Text(
                        text = "\u27A4",
                        fontSize = 22.sp,
                        color = Color(0xFF0EA5B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: DoctorChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isSentByMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(
                    color = if (message.isSentByMe) Color(0xFF0EA5B8) else Color(0xFFE0F2F8),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = if (message.isSentByMe) Color.White else Color(0xFF1C3D5A),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (message.timestamp != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatTime(message.timestamp),
                        color = if (message.isSentByMe) Color(0xFFE0F7FA) else Color.Gray,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}