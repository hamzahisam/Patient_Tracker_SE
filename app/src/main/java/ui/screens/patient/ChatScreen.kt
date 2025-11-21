package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.auth.AuthManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.example.patienttracker.data.firebase.AppUser
import com.example.patienttracker.data.firebase.UserRepository

// Firebase imports
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: Date,
    val isSentByMe: Boolean,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus {
    SENT, DELIVERED, READ
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    context: Context,
    doctorId: String,
    currentUserRole: String = "patient" // "patient" when called from patient side, "doctor" from doctor side
) {
    // Doctor info for the header – loaded via UserRepository
    var doctorUser by remember { mutableStateOf<AppUser?>(null) }

    var patientId by remember { mutableStateOf("") }

    // Load current patient profile
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            patientId = profile?.humanId ?: ""
        } catch (e: Exception) {
            patientId = ""
        }
    }

    // Load doctor profile using their humanId (doctorId) and role = "doctor"
    LaunchedEffect(doctorId) {
        if (doctorId.isNotBlank()) {
            try {
                doctorUser = UserRepository.getUserByHumanId(
                    humanId = doctorId,
                    role = "doctor"
                )
            } catch (e: Exception) {
                Log.w("ChatScreen", "Failed to load doctor user", e)
            }
        }
    }

    // Shared conversation ID (same for patient + doctor side)
    // Ensures deterministic ordering so both use the same document path
    val conversationId = remember(doctorId, patientId) {
        if (patientId.isNotBlank()) {
            // ✅ doctorId first, so it matches DoctorChatScreen's "${doctorId}_${patientId}"
            "${doctorId}_${patientId}"
        } else {
            ""
        }
    }

    // Chat state
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var messageText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 🔁 Live Firestore listener – keeps chat in sync
    DisposableEffect(conversationId) {
        if (conversationId.isBlank()) {
            // Patient ID not loaded yet → don't attach a listener
            onDispose { }
        } else {
            val db = Firebase.firestore
            val query = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp")

            val registration = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatScreen", "listen:error", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    chatMessages.clear()
                    for (doc in snapshot.documents) {
                        val text = doc.getString("text") ?: ""
                        val senderRole = doc.getString("senderRole") ?: "patient"
                        val ts = doc.getTimestamp("timestamp")?.toDate() ?: Date()
                        val statusStr = (doc.getString("status") ?: "SENT").uppercase()
                        var status = runCatching { MessageStatus.valueOf(statusStr) }.getOrElse { MessageStatus.SENT }

                        val isSentByMe = (senderRole == currentUserRole)

                        // 1) Messages I sent: if they are still SENT, mark them as DELIVERED (two grey ticks).
                        if (isSentByMe && status == MessageStatus.SENT) {
                            doc.reference.update("status", MessageStatus.DELIVERED.name)
                            status = MessageStatus.DELIVERED
                        }

                        // 2) Messages from the other user: when viewing this screen, mark them as READ.
                        if (!isSentByMe && status != MessageStatus.READ) {
                            doc.reference.update("status", MessageStatus.READ.name)
                            status = MessageStatus.READ
                        }

                        chatMessages.add(
                            ChatMessage(
                                id = doc.id,
                                text = text,
                                timestamp = ts,
                                isSentByMe = isSentByMe,
                                status = status
                            )
                        )
                    }
                }
            }

            onDispose {
                registration.remove()
            }
        }
    }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            scrollState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            val accent = Color(0xFF4CB7C2)

            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = accent,
                    titleContentColor = accent
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    val fullName = doctorUser?.let {
                        listOf(it.firstName, it.lastName)
                            .filter { part -> part.isNotBlank() }
                            .joinToString(" ")
                    }

                    Column {
                        Text(
                            text = fullName?.let { "Dr. $it" } ?: "Doctor",
                            color = accent,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!doctorUser?.speciality.isNullOrBlank()) {
                            Text(
                                text = doctorUser?.speciality ?: "",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Global patient bottom bar
            PatientBottomBar(navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = scrollState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageBubble(message)
                }
            }

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Message input field
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    cursorBrush = SolidColor(Color(0xFF4CB7C2)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(25.dp))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF4CB7C2),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (messageText.isEmpty()) {
                            Text(
                                "Type a message...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )

                // Send button → writes to Firestore
                IconButton(
                    onClick = {
                        val trimmed = messageText.trim()
                        if (trimmed.isNotEmpty() && patientId.isNotEmpty() && conversationId.isNotBlank()) {
                            val db = Firebase.firestore
                            val msgData = hashMapOf(
                                "text" to trimmed,
                                "senderId" to patientId,
                                "senderRole" to currentUserRole,
                                "doctorId" to doctorId,
                                "patientId" to patientId,
                                "timestamp" to Timestamp.now(),
                                "status" to "SENT"
                            )

                            db.collection("conversations")
                                .document(conversationId)
                                .collection("messages")
                                .add(msgData)
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                                }

                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank() && patientId.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message",
                        tint = if (messageText.isNotBlank() && patientId.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isMyMessage = message.isSentByMe
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMyMessage) 16.dp else 4.dp,
                            bottomEnd = if (isMyMessage) 4.dp else 16.dp
                        )
                    )
                    .then(
                        if (!isMyMessage) {
                            Modifier.border(
                                width = 1.dp,
                                color = Color(0xFF4CB7C2),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMyMessage) 16.dp else 4.dp,
                                    bottomEnd = if (isMyMessage) 4.dp else 16.dp
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        if (isMyMessage) Color(0xFF4CB7C2)
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isMyMessage) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Message status and timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMyMessage) {
                    Text(
                        text = when (message.status) {
                            MessageStatus.SENT -> "✓"
                            MessageStatus.DELIVERED -> "✓✓"
                            MessageStatus.READ -> "✓✓"
                        },
                        color = when (message.status) {
                            MessageStatus.SENT -> MaterialTheme.colorScheme.onSurfaceVariant
                            MessageStatus.DELIVERED -> MaterialTheme.colorScheme.onSurfaceVariant
                            MessageStatus.READ -> MaterialTheme.colorScheme.primary
                        },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                Text(
                    text = timeFormatter.format(message.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}