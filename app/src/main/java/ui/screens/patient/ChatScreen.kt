package com.example.patienttracker.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.*

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

@Composable
fun ChatScreen(navController: NavController, context: Context, doctorId: String) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD)))
    
    // Get the selected doctor from saved state
    val doctor = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<DoctorFull>("selectedDoctor")
    
    // Sample chat messages - in real app, you'd fetch from database
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var messageText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            scrollState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Text(
                        text = "←",
                        modifier = Modifier
                            .clickable { navController.popBackStack() }
                            .padding(end = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = doctor?.let { "Dr. ${it.firstName} ${it.lastName}" } ?: "Doctor",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = doctor?.speciality ?: "",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8FC))
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = scrollState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                // Attach file button
                IconButton(
                    onClick = { 
                        // TODO: Implement file attachment
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint = Color(0xFF4CB7C2)
                    )
                }
                
                // Message input field
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (messageText.isEmpty()) {
                            Text(
                                "Type a message...",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
                
                // Send button
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val newMessage = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = messageText,
                                timestamp = Date(),
                                isSentByMe = true,
                                status = MessageStatus.SENT
                            )
                            chatMessages.add(newMessage)
                            messageText = ""
                            
                            // Simulate doctor response after 2 seconds
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(2000)
                                val responseMessage = ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    text = "Thank you for your message. I'll get back to you soon.",
                                    timestamp = Date(),
                                    isSentByMe = false,
                                    status = MessageStatus.READ
                                )
                                chatMessages.add(responseMessage)
                            }
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message",
                        tint = if (messageText.isNotBlank()) Color(0xFF4CB7C2) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isMyMessage = message.isSentByMe
    
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
                    .background(if (isMyMessage) Color(0xFF4CB7C2) else Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isMyMessage) Color.White else Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Message status and timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMyMessage) {
                    // Message status ticks
                    Text(
                        text = when (message.status) {
                            MessageStatus.SENT -> "✓"
                            MessageStatus.DELIVERED -> "✓✓"
                            MessageStatus.READ -> "✓✓"
                        },
                        color = when (message.status) {
                            MessageStatus.SENT -> Color.Gray
                            MessageStatus.DELIVERED -> Color.Gray
                            MessageStatus.READ -> Color(0xFF4CB7C2)
                        },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                
                Text(
                    text = "${message.timestamp.hours}:${message.timestamp.minutes.toString().padStart(2, '0')}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}