package com.example.patienttracker.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.R
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    navController: NavController? = null,
    context: Context? = null
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    // Google Sign-In setup (only if context is provided)
    val googleSignInClient = remember(context) {
        context?.let {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(it.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(it, gso)
        }
    }
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && context != null && navController != null) {
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
                        val db = Firebase.firestore
                        val doc = db.collection("users").document(user.uid).get().await()
                        
                        if (doc.exists()) {
                            val role = doc.getString("role") ?: ""
                            if (role != "patient") {
                                Firebase.auth.signOut()
                                googleSignInClient?.signOut()
                                throw IllegalStateException("This account is registered as a doctor. Please use doctor login.")
                            }
                            val firstName = doc.getString("firstName") ?: "Patient"
                            val lastName = doc.getString("lastName") ?: ""
                            navController.navigate("patient_home/$firstName/$lastName") {
                                popUpTo("patient_portal") { inclusive = true }
                            }
                        } else {
                            // New user - create patient profile
                            val names = user.displayName?.split(" ") ?: listOf("Patient")
                            val firstName = names.firstOrNull() ?: "Patient"
                            val lastName = if (names.size > 1) names.drop(1).joinToString(" ") else ""
                            val humanId = generateNextPatientIdForLogin()
                            
                            val profileData = hashMapOf(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "email" to (user.email ?: ""),
                                "role" to "patient",
                                "humanId" to humanId,
                                "authProvider" to "google"
                            )
                            
                            db.collection("users").document(user.uid).set(profileData).await()
                            
                            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            navController.navigate("patient_home/$firstName/$lastName") {
                                popUpTo("patient_portal") { inclusive = true }
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

    // Use theme background (dark in your app)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Back button at top-left
            IconButton(
                onClick = { navController?.popBackStack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF4CB7C2)
                )
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(12.dp))

                // Logo + Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_healthtrack_logo),
                        contentDescription = "HealthTrack logo",
                        modifier = Modifier.size(180.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Shifa Track",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        // brand teal/blue
                        color = Color(0xFF4CB7C2)
                    )

                    Spacer(Modifier.height(16.dp))
                }

                // Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GradientButton(
                        text = "Log In",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onLogin
                    )

                    Spacer(Modifier.height(14.dp))

                    GhostButton(
                        text = "Sign Up",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSignUp
                    )
                    
                    // Social Login Section
                    if (context != null && navController != null) {
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
                        
                        Spacer(Modifier.height(20.dp))
                        
                        // Google Sign-In Button
                        OutlinedButton(
                            onClick = {
                                googleSignInClient?.signOut()?.addOnCompleteListener {
                                    googleSignInClient.signInIntent.let { intent ->
                                        googleSignInLauncher.launch(intent)
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
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
                                    color = MaterialTheme.colorScheme.onBackground,
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
                            shape = RoundedCornerShape(26.dp),
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
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = Color(0xFF0EA5B8)
                )
            }
        }
    }
}

private suspend fun generateNextPatientIdForLogin(): String {
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

@Composable
private fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // your blue gradient
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF21D4FD), Color(0xFF0EA5B8))
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = Color(0xFF4CB7C2)
    val bgColor = MaterialTheme.colorScheme.surface

    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = borderColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = bgColor,
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}