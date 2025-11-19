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
            val doc = db.collection("users").document(currentUser.uid).get().await()

            val days = (doc.get("days") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val timings = (doc.get("timings") as? List<*>)?.mapNotNull {
                when (it) {
                    is Long -> it.toInt()
                    is Int -> it
                    else -> null
                }
            } ?: emptyList()

            UserProfile(
                uid = currentUser.uid,
                role = doc.getString("role") ?: "",
                firstName = doc.getString("firstName") ?: "",
                lastName = doc.getString("lastName") ?: "",
                email = doc.getString("email") ?: "",
                humanId = doc.getString("humanId") ?: "",
                speciality = doc.getString("speciality"),
                days = days,
                timings = timings
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
    val humanId: String,
    val speciality: String? = null,       // doctor only
    val days: List<String> = emptyList(), // doctor only
    val timings: List<Int> = emptyList()  // doctor only
)