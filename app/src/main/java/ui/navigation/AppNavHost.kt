package com.example.patienttracker.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.patienttracker.ui.screens.auth.LoginScreen
import com.example.patienttracker.ui.screens.auth.RegisterPatientScreen
import com.example.patienttracker.ui.screens.patient.PatientHomeScreen
import com.example.patienttracker.ui.screens.admin.AdminHomeScreen
import com.example.patienttracker.ui.screens.doctor.DoctorHomeScreen
import com.example.patienttracker.ui.screens.common.RoleSelectorScreen

@Composable
fun AppNavHost(context: Context) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "role") {

        composable("role") { RoleSelectorScreen { navController.navigate("login") } }
        composable("login") {
            LoginScreen(
                onLogin = { navController.navigate("role") },
                onSignUp = { navController.navigate("register_patient") }
            )
        }
        composable("register_patient") { RegisterPatientScreen(navController, context) }
        composable("patient_home") { PatientHomeScreen(navController, context) }
        composable("doctor_home") { DoctorHomeScreen(navController, context) }
        composable("admin_home") { AdminHomeScreen(navController, context) }
    }
}