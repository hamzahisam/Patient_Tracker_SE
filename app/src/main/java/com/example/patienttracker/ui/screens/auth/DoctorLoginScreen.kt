package com.example.patienttracker.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DoctorLoginScreen(
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var doctorId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7FBFB)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Doctor Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF005F73)
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = doctorId,
                onValueChange = { doctorId = it },
                label = { Text("Doctor ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onLogin,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A9396)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Log In", color = Color.White, fontSize = 18.sp)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Forgot ID / Password?",
                color = Color(0xFF0077B6),
                fontSize = 14.sp,
                modifier = Modifier.clickable { onForgotPassword() },
                textAlign = TextAlign.Center
            )
        }
    }
}
