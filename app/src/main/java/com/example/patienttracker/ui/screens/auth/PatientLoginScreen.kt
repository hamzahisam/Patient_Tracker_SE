package com.example.patienttracker.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.patienttracker.ui.screens.auth.model.AppUser
import com.example.patienttracker.R

@Composable
fun PatientLoginScreen(
    navController: NavController,
    context: Context,
    onForgotPassword: () -> Unit = {}
) {
    var idOrEmail by remember { mutableStateOf("") }     // accepts PatientID or email
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Google Sign-In setup
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                scope.launch {
                    isLoading = true
                    try {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        val authResult = Firebase.auth.signInWithCredential(credential).await()
                        val user = authResult.user ?: throw IllegalStateException("Auth failed")
                        
                        // Check if user exists in Firestore
                        val existingProfile = fetchUserProfile(user.uid)
                        
                        if (existingProfile != null) {
                            // User exists - check if they're a patient
                            if (existingProfile.role != "patient") {
                                Firebase.auth.signOut()
                                googleSignInClient.signOut()
                                throw IllegalStateException("This account is registered as a doctor. Please use doctor login.")
                            }
                            // Navigate to home
                            navController.navigate("patient_home/\${existingProfile.firstName}/\${existingProfile.lastName}") {
                                popUpTo("patient_login") { inclusive = true }
                            }
                        } else {
                            // New user - create patient profile
                            val names = user.displayName?.split(" ") ?: listOf("Patient")
                            val firstName = names.firstOrNull() ?: "Patient"
                            val lastName = if (names.size > 1) names.drop(1).joinToString(" ") else ""
                            val humanId = generateNextPatientId()
                            
                            val profileData = hashMapOf(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "email" to (user.email ?: ""),
                                "role" to "patient",
                                "humanId" to humanId,
                                "authProvider" to "google"
                            )
                            
                            Firebase.firestore.collection("users").document(user.uid)
                                .set(profileData).await()
                            
                            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            navController.navigate("patient_home/\$firstName/\$lastName") {
                                popUpTo("patient_login") { inclusive = true }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Google sign-in failed", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    keyboardController?.hide()
                })
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Patient Login",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CB7C2)
                )

                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = idOrEmail,
                    onValueChange = { idOrEmail = it },
                    label = { Text("Patient ID or Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF11151A),
                        unfocusedContainerColor = Color(0xFF11151A),
                        disabledContainerColor = Color(0xFF11151A),
                        errorContainerColor = Color(0xFF11151A),

                        focusedIndicatorColor = Color(0xFF4CB7C2),
                        unfocusedIndicatorColor = Color(0xFF4CB7C2).copy(alpha = 0.5f),
                        cursorColor = Color(0xFF4CB7C2),

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,

                        focusedLabelColor = Color(0xFF4CB7C2),
                        unfocusedLabelColor = Color(0xFF9CA3AF)
                    )
                )

                Spacer(Modifier.height(16.dp))

                var passwordVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        val desc = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc, tint = Color(0xFF4CB7C2))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF11151A),
                        unfocusedContainerColor = Color(0xFF11151A),
                        disabledContainerColor = Color(0xFF11151A),
                        errorContainerColor = Color(0xFF11151A),

                        focusedIndicatorColor = Color(0xFF4CB7C2),
                        unfocusedIndicatorColor = Color(0xFF4CB7C2).copy(alpha = 0.5f),
                        cursorColor = Color(0xFF4CB7C2),

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,

                        focusedLabelColor = Color(0xFF4CB7C2),
                        unfocusedLabelColor = Color(0xFF9CA3AF)
                    )
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                // 1) Resolve email if user typed a humanId (e.g., 000001), else use the email directly
                                val emailToUse = if (idOrEmail.contains("@")) {
                                    idOrEmail.trim()
                                } else {
                                    // treat as humanId
                                    val u = findPatientByHumanId(idOrEmail.trim())
                                        ?: throw IllegalArgumentException("No patient with this ID")
                                    u.email
                                }

                                // 2) Firebase Auth sign-in
                                val user = Firebase.auth
                                    .signInWithEmailAndPassword(emailToUse, password)
                                    .await()
                                    .user ?: throw IllegalStateException("Auth failed")

                                // 3) Fetch profile from Firestore
                                val profile = fetchUserProfile(user.uid)
                                    ?: throw IllegalStateException("Profile not found")

                                if (profile.role != "patient") {
                                    throw IllegalStateException("This account is not a patient")
                                }

                                // In PatientLoginScreen.kt, find this section and update:
                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                                // In PatientLoginScreen.kt, update the login success section:
                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                                // Store profile data for persistence
                                navController.currentBackStackEntry?.savedStateHandle?.set("firstName", profile.firstName)
                                navController.currentBackStackEntry?.savedStateHandle?.set("lastName", profile.lastName)

                                // Navigate directly to patient home
                                navController.navigate("patient_home/${profile.firstName}/${profile.lastName}") {
                                    popUpTo("patient_login") { inclusive = true }
                                }

                                // 4) Hand off name/ID to the next screen via savedStateHandle
                                // Replace this section in the login button onClick:
                                navController.currentBackStackEntry?.savedStateHandle?.set("firstName", profile.firstName)
                                navController.currentBackStackEntry?.savedStateHandle?.set("lastName", profile.lastName)

                                // Navigate to patient_home with arguments
                                navController.navigate("patient_home/${profile.firstName}/${profile.lastName}") {
                                    popUpTo("patient_login") { inclusive = true }
                                }
                                // If you instead want a welcome screen, use:
                                // navController.navigate("patient_welcome/${profile.firstName}/${profile.lastName}/${profile.humanId}")

                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Invalid ID/email or password", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && idOrEmail.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5B8)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(if (isLoading) "Signing In..." else "Log In", color = Color.White, fontSize = 18.sp)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Forgot ID / Password?",
                    color = Color(0xFF4CB7C2),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        scope.launch {
                            if (idOrEmail.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please enter your Patient ID or Email first",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            isLoading = true
                            try {
                                // 1) Resolve email from either email or humanId
                                val emailToUse = if (idOrEmail.contains("@")) {
                                    idOrEmail.trim()
                                } else {
                                    // Treat as Patient ID (humanId), reuse your helper
                                    val user = findPatientByHumanId(idOrEmail.trim())
                                        ?: throw IllegalArgumentException("No patient with this ID")

                                    user.email
                                }

                                // 2) Ask Firebase to send reset email
                                Firebase.auth
                                    .sendPasswordResetEmail(emailToUse)
                                    .await()

                                Toast.makeText(
                                    context,
                                    "Password reset email sent to $emailToUse",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    e.message ?: "Could not send reset email",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Divider with "OR"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF4CB7C2).copy(alpha = 0.5f)
                    )
                    Text(
                        text = "  OR  ",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF4CB7C2).copy(alpha = 0.5f)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Google Sign-In Button
                OutlinedButton(
                    onClick = {
                        // Sign out first to allow account selection
                        googleSignInClient.signOut().addOnCompleteListener {
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF4CB7C2)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Facebook Login Button (Placeholder)
                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Facebook Login coming soon!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1877F2)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_facebook),
                            contentDescription = "Facebook",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Continue with Facebook",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    color = Color(0xFF0EA5B8)
                )
            }
        }
    }
}

/* ------------ Helpers (Firestore) ------------ */

private suspend fun findPatientByHumanId(humanId: String): AppUser? {
    val db = Firebase.firestore
    val snap = db.collection("users")
        .whereEqualTo("humanId", humanId)
        .whereEqualTo("role", "patient")
        .limit(1)
        .get()
        .await()

    val d = snap.documents.firstOrNull() ?: return null
    return AppUser(
        uid = d.id,
        role = d.getString("role") ?: "",
        firstName = d.getString("firstName") ?: "",
        lastName = d.getString("lastName") ?: "",
        email = d.getString("email") ?: "",
        humanId = d.getString("humanId") ?: ""
    )
}

private suspend fun fetchUserProfile(uid: String): AppUser? {
    val db = Firebase.firestore
    val d = db.collection("users").document(uid).get().await()
    if (!d.exists()) return null
    return AppUser(
        uid = uid,
        role = d.getString("role") ?: "",
        firstName = d.getString("firstName") ?: "",
        lastName = d.getString("lastName") ?: "",
        email = d.getString("email") ?: "",
        humanId = d.getString("humanId") ?: ""
    )
}

private suspend fun generateNextPatientId(): String {
    val db = Firebase.firestore
    val snap = db.collection("users")
        .whereEqualTo("role", "patient")
        .get()
        .await()
    
    val maxId = snap.documents.mapNotNull { doc ->
        doc.getString("humanId")?.toIntOrNull()
    }.maxOrNull() ?: 0
    
    return String.format("%06d", maxId + 1)
}