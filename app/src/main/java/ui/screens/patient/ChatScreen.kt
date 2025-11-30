package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.InsertDriveFile
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
import com.example.patienttracker.ui.screens.patient.PatientBottomBar

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: Date,
    val isSentByMe: Boolean,
    val status: MessageStatus = MessageStatus.SENT,
    // Document upload fields
    val isDocumentUpload: Boolean = false,
    val documentName: String = "",
    val uploadedBy: String = "",
    val uploadCollection: String = "",
    val patientId: String = "",
    val doctorId: String = ""
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
    val keyboardController = LocalSoftwareKeyboardController.current

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

                        // Document upload metadata
                        val isDocumentUpload = doc.getBoolean("isDocumentUpload") ?: false
                        val documentName = doc.getString("documentName") ?: ""
                        val uploadedBy = doc.getString("uploadedBy") ?: ""
                        val uploadCollection = doc.getString("uploadCollection") ?: ""
                        val msgPatientId = doc.getString("patientId") ?: ""
                        val msgDoctorId = doc.getString("doctorId") ?: ""

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
                                status = status,
                                isDocumentUpload = isDocumentUpload,
                                documentName = documentName,
                                uploadedBy = uploadedBy,
                                uploadCollection = uploadCollection,
                                patientId = msgPatientId,
                                doctorId = msgDoctorId
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
                    .padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            keyboardController?.hide()
                        })
                    },
                state = scrollState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageBubble(
                        message = message,
                        navController = navController,
                        currentUserRole = currentUserRole,
                        doctorName = doctorUser?.let { "${it.firstName} ${it.lastName}" } ?: "Doctor"
                    )
                }
            }

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload button - navigates to Reports screen for patients
                IconButton(
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("fromChat", true)
                        navController.currentBackStackEntry?.savedStateHandle?.set("chatDoctorId", doctorId)
                        navController.currentBackStackEntry?.savedStateHandle?.set("chatPatientId", patientId)
                        navController.currentBackStackEntry?.savedStateHandle?.set("chatConversationId", conversationId)
                        val doctorName = doctorUser?.let { "${it.firstName} ${it.lastName}" } ?: "Doctor"
                        val encodedName = java.net.URLEncoder.encode(doctorName, "UTF-8")
                        navController.navigate("patient_reports_screen/$doctorId/$encodedName")
                    },
                    enabled = patientId.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Upload document",
                        tint = if (patientId.isNotEmpty())
                            Color(0xFF4CB7C2)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Message input field
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    cursorBrush = SolidColor(Color(0xFF4CB7C2)),
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(25.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF4CB7C2),
                            shape = RoundedCornerShape(25.dp)
                        )
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
fun ChatMessageBubble(
    message: ChatMessage,
    navController: NavController,
    currentUserRole: String,
    doctorName: String = "Doctor"
) {
    val isMyMessage = message.isSentByMe
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }
    
    val bubbleShape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isMyMessage) 80.dp else 0.dp,
                end = if (isMyMessage) 0.dp else 80.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isMyMessage) {
                            Color(0xFF0EA5B8) // sent messages: solid teal bubble
                        } else {
                            MaterialTheme.colorScheme.background // incoming messages: dark background
                        },
                        shape = bubbleShape
                    )
                    .then(
                        if (!isMyMessage) {
                            Modifier.border(
                                width = 1.dp,
                                color = Color(0xFF4CB7C2), // teal border for received messages
                                shape = bubbleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = if (isMyMessage) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // If this is a document upload message, show a clickable "View Document" button
                    if (message.isDocumentUpload && message.documentName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    // Navigate based on who uploaded and current user role
                                    if (message.uploadCollection == "prescriptions") {
                                        // Prescription uploaded by doctor
                                        if (currentUserRole == "patient") {
                                            // Patient viewing doctor's prescription
                                            val encodedName = java.net.URLEncoder.encode(doctorName, "UTF-8")
                                            navController.navigate("patient_prescriptions_screen/${message.doctorId}/$encodedName")
                                        } else {
                                            // Doctor viewing their own prescription
                                            navController.navigate("doctor_patient_prescriptions_screen/${message.patientId}/Patient")
                                        }
                                    } else {
                                        // Report uploaded by patient
                                        if (currentUserRole == "patient") {
                                            // Patient viewing their own report
                                            val encodedName = java.net.URLEncoder.encode(doctorName, "UTF-8")
                                            navController.navigate("patient_reports_screen/${message.doctorId}/$encodedName")
                                        } else {
                                            // Doctor viewing patient's report
                                            navController.navigate("doctor_patient_reports_screen/${message.patientId}/Patient")
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMyMessage) Color.White.copy(alpha = 0.2f) else Color(0xFF4CB7C2).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "View document",
                                    tint = if (isMyMessage) Color.White else Color(0xFF4CB7C2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "View Document",
                                    color = if (isMyMessage) Color.White else Color(0xFF4CB7C2),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Time + read receipts BELOW the bubble (matching doctor chat style)
            Spacer(Modifier.height(2.dp))
            
            val ticks = if (isMyMessage) {
                when (message.status) {
                    MessageStatus.SENT -> "✓"
                    MessageStatus.DELIVERED, MessageStatus.READ -> "✓✓"
                }
            } else {
                ""
            }
            
            val tickColor = if (isMyMessage && message.status == MessageStatus.READ) {
                Color(0xFF4CB7C2) // teal when read
            } else {
                Color.LightGray
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatter.format(message.timestamp),
                    color = if (isMyMessage) Color(0xFFE0F7FA) else Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
                if (ticks.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = ticks,
                        color = tickColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}