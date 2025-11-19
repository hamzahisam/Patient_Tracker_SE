package com.example.patienttracker.data.firebase

import com.google.firebase.Timestamp
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
    val humanId: String,
    val speciality: String? = null,              // doctor-only
    val days: List<String> = emptyList(),        // doctor-only
    val timings: List<Int> = emptyList()         // doctor-only (e.g. [1800, 2100])
)

object UserRepository {
    private val db = Firebase.firestore

    private fun zeroPad(n: Long, width: Int = 6) = n.toString().padStart(width, '0')

    private suspend fun nextHumanId(role: String): String {
        val ref = db.collection("counters").document(role)
        val id = db.runTransaction { tx ->
            val snap = tx.get(ref)
            val next = (snap.getLong("next") ?: 1L)
            tx.set(
                ref,
                mapOf("next" to next + 1),
                com.google.firebase.firestore.SetOptions.merge()
            )
            next
        }.await()
        return zeroPad(id)
    }

    /**
     * For patients you can call this with defaults:
     *   createUserProfile(uid, "patient", first, last, email, phone)
     *
     * For doctors:
     *   createUserProfile(uid, "doctor", first, last, email, phone, speciality, days, timings)
     */
    suspend fun createUserProfile(
        uid: String,
        role: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        speciality: String? = null,
        days: List<String> = emptyList(),
        timings: List<Int> = emptyList()
    ): AppUser {
        val humanId = nextHumanId(role) // e.g. "000001"

        // Use hashMapOf<String, Any?> to avoid weird type inference issues
        val doc = hashMapOf<String, Any?>(
            "role" to role,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phone" to phone,
            "humanId" to humanId,
            "createdAt" to Timestamp.now()
        )

        // Only store schedule info if this is a doctor
        if (role == "doctor") {
            speciality?.let { doc["speciality"] = it }
            if (days.isNotEmpty()) doc["days"] = days
            if (timings.isNotEmpty()) doc["timings"] = timings
        }

        db.collection("users").document(uid).set(doc).await()

        return AppUser(
            uid = uid,
            role = role,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            humanId = humanId,
            speciality = speciality,
            days = days,
            timings = timings
        )
    }

    suspend fun getUserByUid(uid: String): AppUser? {
        val snap = db.collection("users").document(uid).get().await()
        if (!snap.exists()) return null

        val days = snap.get("days") as? List<*> ?: emptyList<Any?>()
        val timingsRaw = snap.get("timings") as? List<*> ?: emptyList<Any?>()

        return AppUser(
            uid = uid,
            role = snap.getString("role") ?: "",
            firstName = snap.getString("firstName") ?: "",
            lastName = snap.getString("lastName") ?: "",
            email = snap.getString("email") ?: "",
            phone = snap.getString("phone") ?: "",
            humanId = snap.getString("humanId") ?: "",
            speciality = snap.getString("speciality"),
            days = days.mapNotNull { it as? String },
            timings = timingsRaw.mapNotNull {
                when (it) {
                    is Long -> it.toInt()
                    is Int -> it
                    else -> null
                }
            }
        )
    }

    // Allow “ID login” UX: find user with humanId + role, then verify with email/pass in Auth
    suspend fun getUserByHumanId(humanId: String, role: String): AppUser? {
        val q = db.collection("users")
            .whereEqualTo("humanId", humanId)
            .whereEqualTo("role", role)
            .limit(1)
            .get()
            .await()

        val d = q.documents.firstOrNull() ?: return null

        val days = d.get("days") as? List<*> ?: emptyList<Any?>()
        val timingsRaw = d.get("timings") as? List<*> ?: emptyList<Any?>()

        return AppUser(
            uid = d.id,
            role = role,
            firstName = d.getString("firstName") ?: "",
            lastName = d.getString("lastName") ?: "",
            email = d.getString("email") ?: "",
            phone = d.getString("phone") ?: "",
            humanId = d.getString("humanId") ?: "",
            speciality = d.getString("speciality"),
            days = days.mapNotNull { it as? String },
            timings = timingsRaw.mapNotNull {
                when (it) {
                    is Long -> it.toInt()
                    is Int -> it
                    else -> null
                }
            }
        )
    }
}