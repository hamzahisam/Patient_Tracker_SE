package com.example.patienttracker.data

import java.util.*

data class ChatMessage(
    val id: String,
    val patientId: String,
    val doctorId: String,
    val message: String,
    val timestamp: Date,
    val isSentByPatient: Boolean,
    val status: MessageStatus,
    val attachmentUrl: String? = null
)

enum class MessageStatus {
    SENT, DELIVERED, READ
}