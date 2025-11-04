package com.example.patienttracker

import android.os.Bundle
import android.util.Log
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.patienttracker.ui.navigation.AppNavHost
import com.example.patienttracker.ui.theme.PatientTrackerTheme
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Optional Firestore smoke test (debug builds only, without BuildConfig)
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            val db = Firebase.firestore
            db.collection("debug")
                .add(
                    mapOf(
                        "hello" to "world",
                        "launchedAt" to FieldValue.serverTimestamp()
                    )
                )
                .addOnSuccessListener { Log.d(TAG, "✅ Firestore write OK") }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Firestore write failed", e) }
        }

        setContent {
            PatientTrackerTheme {
                AppNavHost(context = this)
            }
        }
    }

    companion object {
        private const val TAG = "Firestore"
    }
}