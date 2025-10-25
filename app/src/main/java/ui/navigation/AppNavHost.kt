package com.example.patienttracker.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.patienttracker.ui.screens.auth.LoginScreen
import com.example.patienttracker.ui.screens.auth.RegisterPatientScreen
import com.example.patienttracker.ui.screens.patient.PatientHomeScreen
import com.example.patienttracker.ui.screens.auth.PatientAccountCreatedScreen
import com.example.patienttracker.ui.screens.admin.AdminHomeScreen
import com.example.patienttracker.ui.screens.doctor.DoctorHomeScreen
import com.example.patienttracker.ui.screens.common.RoleSelectorScreen
import com.example.patienttracker.ui.screens.auth.PatientLoginScreen
import com.example.patienttracker.ui.screens.auth.DoctorLoginScreen
import com.example.patienttracker.ui.screens.auth.PatientWelcomeScreen
import com.example.patienttracker.ui.screens.auth.DoctorWelcomeScreen

private object Route {
    const val ROLE = "role"
    const val PATIENT_PORTAL = "patient_portal"
    const val PATIENT_LOGIN = "patient_login"
    const val REGISTER_PATIENT = "register_patient"
    const val ACCOUNT_CREATED = "account_created"
    const val ACCOUNT_CREATED_ARG = "patientId"
    const val PATIENT_WELCOME = "patient_welcome"
    const val DOCTOR_LOGIN = "doctor_login"
    const val DOCTOR_WELCOME = "doctor_welcome"

    // Home routes
    const val PATIENT_HOME = "patient_home" // legacy (no args)
    const val PATIENT_HOME_ARGS = "patient_home/{firstName}/{lastName}" // preferred
    const val DOCTOR_HOME = "doctor_home"
    const val ADMIN_HOME = "admin_home"
}

@Composable
fun AppNavHost(context: Context) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.ROLE
    ) {

        // Role selection
        composable(Route.ROLE) {
            RoleSelectorScreen { role ->
                when (role) {
                    "patient" -> navController.navigate(Route.PATIENT_PORTAL) {
                        launchSingleTop = true
                        restoreState = true
                    }
                    "doctor" -> navController.navigate(Route.DOCTOR_LOGIN) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }

        // Patient entry screen (choice: Login / SignUp)
        composable(Route.PATIENT_PORTAL) {
            LoginScreen(
                onLogin = {
                    navController.navigate(Route.PATIENT_LOGIN) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSignUp = {
                    navController.navigate(Route.REGISTER_PATIENT) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Patient actual login
        composable(Route.PATIENT_LOGIN) {
            PatientLoginScreen(navController, context)
        }

        // Patient welcome (kept as-is)
        composable("patient_welcome/{firstName}/{lastName}/{patientId}") { backStackEntry ->
            val first = backStackEntry.arguments?.getString("firstName") ?: ""
            val last = backStackEntry.arguments?.getString("lastName") ?: ""
            val id = backStackEntry.arguments?.getString("patientId") ?: ""
            PatientWelcomeScreen(navController, first, last, id)
        }

        // Doctor login
        composable(Route.DOCTOR_LOGIN) {
            DoctorLoginScreen(navController, context)
        }

        // Doctor welcome (kept as-is)
        composable("doctor_welcome/{firstName}/{lastName}/{doctorId}") { backStackEntry ->
            val first = backStackEntry.arguments?.getString("firstName") ?: ""
            val last = backStackEntry.arguments?.getString("lastName") ?: ""
            val id = backStackEntry.arguments?.getString("doctorId") ?: ""
            DoctorWelcomeScreen(navController, first, last, id)
        }

        // Patient registration
        composable(Route.REGISTER_PATIENT) {
            RegisterPatientScreen(navController, context)
        }

        // Account created confirmation
        composable(
            route = "${Route.ACCOUNT_CREATED}/{${Route.ACCOUNT_CREATED_ARG}}",
            arguments = listOf(navArgument(Route.ACCOUNT_CREATED_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString(Route.ACCOUNT_CREATED_ARG) ?: ""
            PatientAccountCreatedScreen(navController, patientId)
        }

        // Patient home (preferred route with name args)
        composable(
            route = Route.PATIENT_HOME_ARGS,
            arguments = listOf(
                navArgument("firstName") { type = NavType.StringType; defaultValue = "" },
                navArgument("lastName") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            PatientHomeScreen(navController, context)
        }

        // Legacy patient home (no args) — kept so old navigations don’t crash
        composable(Route.PATIENT_HOME) {
            PatientHomeScreen(navController, context)
        }

        // Other homes
        composable(Route.DOCTOR_HOME) { DoctorHomeScreen(navController, context) }
        composable(Route.ADMIN_HOME) { AdminHomeScreen(navController, context) }
    }
}