package com.example.patienttracker.data.firebase

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object AuthRepository {
    private val auth = Firebase.auth

    suspend fun signIn(email: String, password: String) =
        auth.signInWithEmailAndPassword(email.trim(), password).await().user

    suspend fun signUp(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email.trim(), password).await().user

    fun currentUser() = auth.currentUser

    fun signOut() = auth.signOut()
}