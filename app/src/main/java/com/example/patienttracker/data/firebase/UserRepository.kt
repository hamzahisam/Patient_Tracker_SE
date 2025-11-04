package com.example.patienttracker.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class AppUser(
    val uid: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val humanId: String
)

object UserRepository {
    private val db = Firebase.firestore

    private fun zeroPad(n: Long, width: Int = 6) = n.toString().padStart(width, '0')

    private suspend fun nextHumanId(role: String): String {
        val ref = db.collection("counters").document(role)
        val id = db.runTransaction { tx ->
            val snap = tx.get(ref)
            val next = (snap.getLong("next") ?: 1L)
            tx.set(ref, mapOf("next" to next + 1), com.google.firebase.firestore.SetOptions.merge())
            next
        }.await()
        return zeroPad(id)
    }

    suspend fun createUserProfile(
        uid: String,
        role: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String
    ): AppUser {
        val humanId = nextHumanId(role) // "000001"
        val doc = mapOf(
            "role" to role,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phone" to phone,
            "humanId" to humanId,
            "createdAt" to Timestamp.now()
        )
        db.collection("users").document(uid).set(doc).await()
        return AppUser(uid, role, firstName, lastName, email, phone, humanId)
    }

    suspend fun getUserByUid(uid: String): AppUser? {
        val snap = db.collection("users").document(uid).get().await()
        if (!snap.exists()) return null
        return AppUser(
            uid = uid,
            role = snap.getString("role") ?: "",
            firstName = snap.getString("firstName") ?: "",
            lastName = snap.getString("lastName") ?: "",
            email = snap.getString("email") ?: "",
            phone = snap.getString("phone") ?: "",
            humanId = snap.getString("humanId") ?: ""
        )
    }

    // Allow “ID login” UX: find user with humanId + role, then verify with email/pass in Auth
    suspend fun getUserByHumanId(humanId: String, role: String): AppUser? {
        val q = db.collection("users")
            .whereEqualTo("humanId", humanId)
            .whereEqualTo("role", role)
            .limit(1)
            .get().await()
        val d = q.documents.firstOrNull() ?: return null
        return AppUser(
            uid = d.id,
            role = role,
            firstName = d.getString("firstName") ?: "",
            lastName = d.getString("lastName") ?: "",
            email = d.getString("email") ?: "",
            phone = d.getString("phone") ?: "",
            humanId = d.getString("humanId") ?: ""
        )
    }
}