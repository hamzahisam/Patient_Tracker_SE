package com.example.patienttracker.ui.screens.auth

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.DoctorAccountStorage
import kotlinx.coroutines.delay
import com.example.patienttracker.data.DoctorAccount


@Composable
fun DoctorLoginScreen(
    navController: NavController,
    context: Context
) {
    var doctorId by remember { mutableStateOf("") }
    var loggedInDoctor by remember { mutableStateOf<DoctorAccount?>(null) }
    var password by remember { mutableStateOf("") }
    var showWelcome by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf("") }
    var doctorIdDisplay by remember { mutableStateOf("") }

    if (showWelcome) {
        LaunchedEffect(Unit) {
            delay(2500)
            loggedInDoctor?.let {
                navController.navigate("doctor_welcome/${it.firstName}/${it.lastName}/${it.id}")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0F7FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome Dr. $doctorName",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00796B)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "DoctorID# $doctorIdDisplay",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

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
                onClick = {
                    val doctor = DoctorAccountStorage.validateLogin(context, doctorId, password)
                    if (doctor != null) {
                        loggedInDoctor = doctor
                        doctorName = "${doctor.firstName} ${doctor.lastName}"
                        doctorIdDisplay = doctor.id
                        showWelcome = true
                    } else {
                        Toast.makeText(context, "Invalid Doctor ID or Password", Toast.LENGTH_SHORT).show()
                    }

                },
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
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Please contact admin.", Toast.LENGTH_SHORT).show()
                },
                textAlign = TextAlign.Center
            )
        }
    }
}
