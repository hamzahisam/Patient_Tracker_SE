package com.example.patienttracker.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object AuthManager {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    suspend fun getCurrentUserRole(): String? {
        val currentUser = auth.currentUser ?: return null
        
        return try {
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            userDoc.getString("role")
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val currentUser = auth.currentUser ?: return null
        
        return try {
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            UserProfile(
                uid = currentUser.uid,
                role = userDoc.getString("role") ?: "",
                firstName = userDoc.getString("firstName") ?: "",
                lastName = userDoc.getString("lastName") ?: "",
                email = userDoc.getString("email") ?: "",
                humanId = userDoc.getString("humanId") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class UserProfile(
    val uid: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val humanId: String
)