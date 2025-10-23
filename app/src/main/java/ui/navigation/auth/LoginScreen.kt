package com.example.patienttracker.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.patienttracker.R

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(12.dp)) // top breathing room

                // Logo + Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_healthtrack_logo),
                        contentDescription = "HealthTrack logo",
                        modifier = Modifier
                            .size(180.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "HealthTrack",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFF05B8C7) // teal-cyan brand
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Track your health, keep records handy, and share securely with your doctor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
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

                    // Secondary (ghost) style to match mock
                    GhostButton(
                        text = "Sign Up",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSignUp
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Brand gradient (cyan → teal)
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF21D4FD), Color(0xFF0EA5B8))
    )

    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues()
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
    val bg = Color(0xFFEFF6F9)
    val fg = Color(0xFF0EA5B8)

    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(),
        // subtle background with no elevation
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg
        ),
        elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = fg
            )
        }
    }
}