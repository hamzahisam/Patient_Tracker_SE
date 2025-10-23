package com.example.patienttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.patienttracker.ui.navigation.AppNavHost
import com.example.patienttracker.ui.theme.PatientTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PatientTrackerTheme {
                // Pass applicationContext so Storage/Session can use it safely
                AppNavHost(applicationContext)
            }
        }
    }
}