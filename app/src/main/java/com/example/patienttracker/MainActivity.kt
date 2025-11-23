package com.example.patienttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.patienttracker.data.LanguageManager
import com.example.patienttracker.ui.navigation.AppNavHost
import com.example.patienttracker.ui.theme.PatientTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved language before setting content
        val context = LanguageManager.setAppLanguage(this,
            LanguageManager.getSavedLanguage(this))

        setContent {
            PatientTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(context = context)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-apply language in case it was changed from settings
        // This ensures language changes take effect when returning to activity
        LanguageManager.setAppLanguage(this, 
            LanguageManager.getSavedLanguage(this))
    }
}