package com.example.patienttracker.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.ui.screens.common.SplashScreen
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
import com.example.patienttracker.ui.screens.patient.DoctorListScreen
import com.example.patienttracker.ui.screens.patient.DoctorFull
import com.example.patienttracker.ui.screens.patient.BookAppointmentScreen
import com.example.patienttracker.ui.screens.patient.FullScheduleScreen
import com.example.patienttracker.ui.screens.patient.PatientProfileScreen
import com.example.patienttracker.ui.screens.doctor.DoctorProfileScreen
import com.example.patienttracker.ui.screens.doctor.DoctorPatientsScreen
import com.example.patienttracker.ui.screens.patient.ChatSelectionScreen
import com.example.patienttracker.ui.screens.patient.ChatScreen
// import com.example.patienttracker.ui.screens.patient.PatientRecordOptionsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorChatInboxScreen
import com.example.patienttracker.ui.screens.doctor.DoctorChatScreen
import ui.screens.patient.PatientRecordOptionsScreen
import ui.screens.patient.PatientReportsScreen
import ui.screens.patient.RecordViewerScreen
import ui.screens.patient.MedicalRecord
import com.example.patienttracker.ui.screens.doctor.DoctorPatientRecordOptionsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorPatientReportsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorPatientPrescriptionsScreen
import com.example.patienttracker.ui.screens.patient.EnhancedProfileScreen
import com.example.patienttracker.ui.screens.patient.SettingsScreen
import ui.screens.doctor.DoctorEnhancedProfileScreen

object Route {
    const val SPLASH = "splash"
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
    const val PATIENT_HOME = "patient_home"
    const val PATIENT_HOME_ARGS = "patient_home/{firstName}/{lastName}"
    const val DOCTOR_HOME = "doctor_home"
    const val DOCTOR_HOME_ARGS = "doctor_home/{firstName}/{lastName}/{doctorId}"
    const val ADMIN_HOME = "admin_home"
}

@Composable
fun AppNavHost(context: Context) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.SPLASH
    ) {
        // Splash screen - handles auth check
        composable(Route.SPLASH) {
            SplashScreen()

            LaunchedEffect(Unit) {
                val isLoggedIn = AuthManager.isUserLoggedIn()

                if (!isLoggedIn) {
                    // User is not logged in, go to role selection
                    navController.navigate(Route.ROLE) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                    return@LaunchedEffect
                }

                try {
                    val role = AuthManager.getCurrentUserRole()
                    val profile = AuthManager.getCurrentUserProfile()

                    when (role) {
                        "patient" -> {
                            if (profile != null) {
                                // Navigate directly to patient home with name
                                navController.navigate(
                                    "${Route.PATIENT_HOME}/${profile.firstName}/${profile.lastName}"
                                ) {
                                    popUpTo(Route.SPLASH) { inclusive = true }
                                }
                            } else {
                                // Fallback: go to role selection
                                navController.navigate(Route.ROLE) {
                                    popUpTo(Route.SPLASH) { inclusive = true }
                                }
                            }
                        }

                        "doctor" -> {
                            if (profile != null) {
                                // Navigate directly to doctor home with name + humanId
                                navController.navigate(
                                    "${Route.DOCTOR_HOME}/${profile.firstName}/${profile.lastName}/${profile.humanId}"
                                ) {
                                    popUpTo(Route.SPLASH) { inclusive = true }
                                }
                            } else {
                                // Fallback: go to role selection
                                navController.navigate(Route.ROLE) {
                                    popUpTo(Route.SPLASH) { inclusive = true }
                                }
                            }
                        }

                        "admin" -> {
                            navController.navigate(Route.ADMIN_HOME) {
                                popUpTo(Route.SPLASH) { inclusive = true }
                            }
                        }

                        else -> {
                            // Unknown role, go to role selection
                            navController.navigate(Route.ROLE) {
                                popUpTo(Route.SPLASH) { inclusive = true }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Any error -> go to role selection
                    navController.navigate(Route.ROLE) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                }
            }
        }

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

        // Rest of your existing composables...
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

        composable(Route.PATIENT_LOGIN) {
            PatientLoginScreen(navController, context)
        }

        composable("patient_welcome/{firstName}/{lastName}/{patientId}") { backStackEntry ->
            val first = backStackEntry.arguments?.getString("firstName") ?: ""
            val last = backStackEntry.arguments?.getString("lastName") ?: ""
            val id = backStackEntry.arguments?.getString("patientId") ?: ""
            PatientWelcomeScreen(navController, first, last, id)
        }

        composable(Route.DOCTOR_LOGIN) {
            DoctorLoginScreen(navController, context)
        }

        composable("doctor_welcome/{firstName}/{lastName}/{doctorId}") { backStackEntry ->
            val first = backStackEntry.arguments?.getString("firstName") ?: ""
            val last = backStackEntry.arguments?.getString("lastName") ?: ""
            val id = backStackEntry.arguments?.getString("doctorId") ?: ""
            DoctorWelcomeScreen(navController, first, last, id)
        }

        composable(Route.REGISTER_PATIENT) {
            RegisterPatientScreen(navController, context)
        }

        composable(
            route = "${Route.ACCOUNT_CREATED}/{${Route.ACCOUNT_CREATED_ARG}}",
            arguments = listOf(navArgument(Route.ACCOUNT_CREATED_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString(Route.ACCOUNT_CREATED_ARG) ?: ""
            PatientAccountCreatedScreen(navController, patientId)
        }

        composable(
            route = Route.PATIENT_HOME_ARGS,
            arguments = listOf(
                navArgument("firstName") { type = NavType.StringType; defaultValue = "" },
                navArgument("lastName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            PatientHomeScreen(navController, context)
        }

        composable(Route.PATIENT_HOME) {
            PatientHomeScreen(navController, context)
        }

        composable("doctor_list/{speciality}") { backStackEntry ->
            val speciality = backStackEntry.arguments?.getString("speciality")
            DoctorListScreen(navController, context, speciality)
        }

        composable("book_appointment") {
            val doctor = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<DoctorFull>("selectedDoctor")

            if (doctor != null) {
                BookAppointmentScreen(navController, context, doctor)
            } else {
                DoctorListScreen(navController, context, specialityFilter = "All")
            }
        }

        composable("full_schedule") {
            FullScheduleScreen(navController, context)
        }

        // Patient record entry screens
        composable("patient_record_options") {
            PatientRecordOptionsScreen(navController)
        }

        composable("patient_reports_screen") {
            // Patient viewing their own REPORTS (can upload)
            PatientReportsScreen(
                navController = navController,
                context = context,
                canUpload = true,
                patientIdOverride = null,
                collectionOverride = "records"
            )
        }

        composable("patient_prescriptions_screen") {
            // Patient viewing their own PRESCRIPTIONS (can upload)
            PatientReportsScreen(
                navController = navController,
                context = context,
                canUpload = true,
                patientIdOverride = null,
                collectionOverride = "prescriptions"
            )
        }

        composable("patient_record_viewer") {
            // We expect the previous screen to put a MedicalRecord into the SavedStateHandle
            val record = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<MedicalRecord>("selectedRecord")

            if (record != null) {
                RecordViewerScreen(navController, record)
            } else {
                // Nothing to show, just go back safely
                navController.popBackStack()
            }
        }

        // REPLACED: Patient profile with Enhanced Profile
        composable(
            route = "patient_profile/{firstName}/{lastName}",
            arguments = listOf(
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Instead of PatientProfileScreen, navigate to EnhancedProfileScreen
            EnhancedProfileScreen(navController)
        }

        // PATIENT chat selection – list of doctors this patient can chat with
        composable("chat_selection_patient") {
            ChatSelectionScreen(navController, context)
        }

        // DOCTOR chat inbox – list of patients who have chatted with this doctor
        composable("doctor_chat_inbox") {
            DoctorChatInboxScreen(navController)
        }

        /**
         * PATIENT chat screen
         * - opened when a PATIENT selects a doctor
         * - doctorHumanId = doctor.humanId
         */
        composable(
            route = "chat_patient/{doctorHumanId}",
            arguments = listOf(
                navArgument("doctorHumanId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val doctorHumanId = backStackEntry.arguments?.getString("doctorHumanId").orEmpty()
            ChatScreen(navController, context, doctorHumanId)
        }

        /**
         * DOCTOR chat screen
         * - opened when a DOCTOR selects a patient
         * - patientHumanId = patient.humanId
         */
        composable(
            route = "chat_doctor/{patientHumanId}",
            arguments = listOf(
                navArgument("patientHumanId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val patientHumanId = backStackEntry.arguments?.getString("patientHumanId").orEmpty()
            DoctorChatScreen(navController, context, patientHumanId)
        }

        composable("settings") {
            SettingsScreen(navController)
        }

        composable("account_settings") {
            SettingsScreen(navController)
        }

        composable("enhanced_profile") {
            EnhancedProfileScreen(navController)
        }

        composable(
            route = Route.DOCTOR_HOME_ARGS,
            arguments = listOf(
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("doctorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            DoctorHomeScreen(navController, context, firstName, lastName, doctorId)
        }

        composable(Route.DOCTOR_HOME) {
            DoctorHomeScreen(navController, context)
        }

        composable(Route.ADMIN_HOME) { AdminHomeScreen(navController, context) }

        // Doctor Patients screen
        composable("doctor_patients") {
            DoctorPatientsScreen(navController)
        }

        // Doctor taps a patient → record options for that patient
        composable("doctor_patient_record_options") {
            val patientId = navController.previousBackStackEntry
                ?.savedStateHandle?.get<String>("selectedPatientId").orEmpty()
            val patientName = navController.previousBackStackEntry
                ?.savedStateHandle?.get<String>("selectedPatientName").orEmpty()

            DoctorPatientRecordOptionsScreen(
                navController = navController,
                patientId = patientId,
                patientName = patientName
            )
        }

        composable(
            route = "doctor_patient_reports_screen/{patientId}/{patientName}",
            arguments = listOf(
                navArgument("patientId")   { type = NavType.StringType },
                navArgument("patientName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val patientId   = backStackEntry.arguments?.getString("patientId").orEmpty()
            val patientName = backStackEntry.arguments?.getString("patientName").orEmpty()
            DoctorPatientReportsScreen(navController, patientId, patientName)
        }

        composable(
            route = "doctor_patient_prescriptions_screen/{patientId}/{patientName}",
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("patientName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId").orEmpty()
            val patientName = backStackEntry.arguments?.getString("patientName").orEmpty()

            PatientReportsScreen(
                navController = navController,
                context = context,
                patientIdOverride = patientId,
                canUpload = true,                    // doctor can upload here
                collectionOverride = "prescriptions",
                title = "Prescriptions – $patientName",
                entitySingular = "prescription / diagnosis",
                entityPlural = "prescriptions / diagnoses"
            )
        }

        composable("doctor_profile") {
            DoctorEnhancedProfileScreen(navController)
        }
    }
}