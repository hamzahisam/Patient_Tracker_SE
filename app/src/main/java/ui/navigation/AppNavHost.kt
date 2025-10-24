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
import com.example.patienttracker.ui.screens.auth.PatientLoginScreen
import com.example.patienttracker.ui.screens.auth.DoctorLoginScreen
import com.example.patienttracker.ui.screens.auth.PatientWelcomeScreen
import com.example.patienttracker.ui.screens.auth.DoctorWelcomeScreen


@Composable
fun AppNavHost(context: Context) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "role") {
        
        // Role selection
        composable("role") {
            RoleSelectorScreen { role ->
                if (role == "patient") {
                    navController.navigate("patient_portal")
                } else if (role == "doctor") {
                    navController.navigate("doctor_login")
                }
            }
        }

        // Patient entry screen (choice: Login / SignUp)
        composable("patient_portal") {
            LoginScreen(
                onLogin = { navController.navigate("patient_login") },
                onSignUp = { navController.navigate("register_patient") }
            )
        }

        // Patient actual login
        composable("patient_login") {
            PatientLoginScreen(navController, context)
        }


        composable("patient_welcome/{firstName}/{lastName}/{patientId}") { backStackEntry ->
            val first = backStackEntry.arguments?.getString("firstName") ?: ""
            val last = backStackEntry.arguments?.getString("lastName") ?: ""
            val id = backStackEntry.arguments?.getString("patientId") ?: ""
            PatientWelcomeScreen(navController, first, last, id)
        }

        // Doctor login
        composable("doctor_login") {
            DoctorLoginScreen(navController, context)
        }


        composable("doctor_welcome/{firstName}/{lastName}/{doctorId}") { backStackEntry ->
            val first = backStackEntry.arguments?.getString("firstName") ?: ""
            val last = backStackEntry.arguments?.getString("lastName") ?: ""
            val id = backStackEntry.arguments?.getString("doctorId") ?: ""
            DoctorWelcomeScreen(navController, first, last, id)
        }


        composable("register_patient") { RegisterPatientScreen(navController, context) }
        composable("patient_home") { PatientHomeScreen(navController, context) }
        composable("doctor_home") { DoctorHomeScreen(navController, context) }
        composable("admin_home") { AdminHomeScreen(navController, context) }
        
    }
}
