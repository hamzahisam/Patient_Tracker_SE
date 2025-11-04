package com.example.patienttracker.ui.screens.auth.model

data class AppUser(
    val uid: String = "",
    val role: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val humanId: String = ""   // your human-friendly ID (e.g., 000001)
)