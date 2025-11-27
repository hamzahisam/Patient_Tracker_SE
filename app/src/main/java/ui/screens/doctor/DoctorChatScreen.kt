package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.patienttracker.ui.screens.common.BackButton
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import com.example.patienttracker.ui.screens.doctor.DoctorBottomBar

private const val TAG = "DoctorChatScreen"

data class DoctorChatMessage(
    val id: String,
    val text: String,
    val timestamp: Date?,
    val isSentByMe: Boolean,
    val status: String,
    // Document upload fields
    val isDocumentUpload: Boolean = false,
    val documentName: String = "",
    val uploadedBy: String = "",
    val uploadCollection: String = "",
    val patientId: String = "",
    val doctorId: String = ""
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
    patientName: String = ""
) {
    val scope = rememberCoroutineScope()

    var doctorId by remember { mutableStateOf<String?>(null) }
    var doctorFirstName by remember { mutableStateOf("") }
    var doctorLastName by remember { mutableStateOf("") }

    // Messages for this conversation
    val messages = remember { mutableStateListOf<DoctorChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

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

                        // Normalize status to upper-case to match the patient side (SENT / DELIVERED / READ)
                        var status = (doc.getString("status") ?: "SENT").uppercase(Locale.getDefault())

                        val isSentByMe = senderRole == "doctor"

                        // Document upload metadata
                        val isDocumentUpload = doc.getBoolean("isDocumentUpload") ?: false
                        val documentName = doc.getString("documentName") ?: ""
                        val uploadedBy = doc.getString("uploadedBy") ?: ""
                        val uploadCollection = doc.getString("uploadCollection") ?: ""
                        val msgPatientId = doc.getString("patientId") ?: ""
                        val msgDoctorId = doc.getString("doctorId") ?: ""

                        // 1) Messages sent by the doctor: once they appear in this snapshot, they are at least DELIVERED.
                        if (isSentByMe && status == "SENT") {
                            doc.reference.update("status", "DELIVERED")
                            status = "DELIVERED"
                        }

                        // 2) Messages from the patient: when the doctor is viewing this screen, mark them as READ.
                        if (!isSentByMe && status != "READ") {
                            doc.reference.update("status", "READ")
                            status = "READ"
                        }

                        DoctorChatMessage(
                            id = doc.id,
                            text = text,
                            timestamp = ts.toDate(),
                            isSentByMe = isSentByMe,
                            status = status,
                            isDocumentUpload = isDocumentUpload,
                            documentName = documentName,
                            uploadedBy = uploadedBy,
                            uploadCollection = uploadCollection,
                            patientId = msgPatientId,
                            doctorId = msgDoctorId
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

    // Auto-scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
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
            "status" to "SENT"
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
                    val displayName = if (patientName.isNotBlank()) patientName else "Patient"
                    Column {
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = Color(0xFF4CB7C2)
                        )
                        Text(
                            text = "Patient chat",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    BackButton(
                        navController = navController,
                        modifier = Modifier
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = Color(0xFF4CB7C2),
                    titleContentColor = Color(0xFF4CB7C2)
                )
            )
        },
        bottomBar = {
            DoctorBottomBar(navController, selectedTab = 1) // Chat tab
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            keyboardController?.hide()
                        })
                    },
                state = scrollState,
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    MessageRow(
                        message = msg,
                        navController = navController,
                        patientId = patientId,
                        patientName = patientName
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload button - navigates to Prescriptions screen for doctors
                IconButton(
                    onClick = {
                        val dId = doctorId
                        if (!dId.isNullOrBlank() && patientId.isNotBlank()) {
                            // Set flag to indicate we came from chat
                            navController.currentBackStackEntry?.savedStateHandle?.set("fromChat", true)
                            navController.currentBackStackEntry?.savedStateHandle?.set("chatDoctorId", dId)
                            navController.currentBackStackEntry?.savedStateHandle?.set("chatPatientId", patientId)
                            navController.currentBackStackEntry?.savedStateHandle?.set("chatConversationId", conversationId)
                            // Navigate to doctor prescriptions screen
                            navController.navigate("doctor_patient_prescriptions_screen/$patientId/$patientName")
                        }
                    },
                    enabled = !doctorId.isNullOrBlank() && patientId.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Upload prescription",
                        tint = if (!doctorId.isNullOrBlank() && patientId.isNotBlank())
                            Color(0xFF4CB7C2)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    cursorBrush = SolidColor(Color(0xFF4CB7C2)),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
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
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Type a message...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
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
private fun MessageRow(
    message: DoctorChatMessage,
    navController: NavController,
    patientId: String,
    patientName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (message.isSentByMe) 80.dp else 16.dp,
                end = if (message.isSentByMe) 16.dp else 80.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
        horizontalArrangement = if (message.isSentByMe) Arrangement.End else Arrangement.Start
    ) {
        // Wrap bubble + metadata so time/checks are BELOW bubble
        Column(
            horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start
        ) {
            val bubbleShape = RoundedCornerShape(18.dp)

            // Message bubble
            Box(
                modifier = Modifier
                    .background(
                        color = if (message.isSentByMe) {
                            Color(0xFF0EA5B8) // doctor messages: solid teal bubble
                        } else {
                            MaterialTheme.colorScheme.background // incoming messages: dark background
                        },
                        shape = bubbleShape
                    )
                    .then(
                        if (!message.isSentByMe) {
                            Modifier.border(
                                width = 1.dp,
                                color = Color(0xFF4CB7C2), // blue border for other person's messages
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
                        color = if (message.isSentByMe) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // If this is a document upload message, show a clickable "View Document" button
                    if (message.isDocumentUpload && message.documentName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    // Navigate based on who uploaded
                                    // Doctor is viewing - currentUserRole is "doctor"
                                    if (message.uploadCollection == "prescriptions") {
                                        // Prescription uploaded by doctor (themselves)
                                        val encodedName = java.net.URLEncoder.encode(patientName.ifBlank { "Patient" }, "UTF-8")
                                        navController.navigate("doctor_patient_prescriptions_screen/${patientId}/$encodedName")
                                    } else {
                                        // Report uploaded by patient
                                        val encodedName = java.net.URLEncoder.encode(patientName.ifBlank { "Patient" }, "UTF-8")
                                        navController.navigate("doctor_patient_reports_screen/${patientId}/$encodedName")
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (message.isSentByMe) Color.White.copy(alpha = 0.2f) else Color(0xFF4CB7C2).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "View document",
                                    tint = if (message.isSentByMe) Color.White else Color(0xFF4CB7C2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "View Document",
                                    color = if (message.isSentByMe) Color.White else Color(0xFF4CB7C2),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Time + read receipts BELOW the bubble
            if (message.timestamp != null) {
                Spacer(Modifier.height(2.dp))

                val ticks = if (message.isSentByMe) {
                    when (message.status.uppercase(Locale.getDefault())) {
                        "SENT" -> "✓"
                        "DELIVERED", "READ" -> "✓✓"
                        else -> ""
                    }
                } else {
                    ""
                }

                val tickColor =
                    if (message.isSentByMe &&
                        message.status.uppercase(Locale.getDefault()) == "READ"
                    ) {
                        Color(0xFF4CB7C2) // teal when read
                    } else {
                        Color.LightGray
                    }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        color = if (message.isSentByMe) Color(0xFFE0F7FA) else Color.Gray,
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
}