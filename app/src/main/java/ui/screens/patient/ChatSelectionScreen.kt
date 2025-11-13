package com.example.patienttracker.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape

@Composable
fun ChatSelectionScreen(navController: NavController, context: Context) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD)))
    
    // Get doctors from CSV
    val doctors = remember { readDoctorCsv(context) }
    
    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select Doctor to Chat",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8FC)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(doctors) { doctor ->
                DoctorChatCard(doctor) {
                    navController.currentBackStackEntry?.savedStateHandle?.set("selectedDoctor", doctor)
                    navController.navigate("chat_screen/${doctor.id}")
                }
            }
        }
    }
}

@Composable
fun DoctorChatCard(doctor: DoctorFull, onChatClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Doctor avatar/icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE9F3F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${doctor.firstName.first()}${doctor.lastName.first()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4CB7C2)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C3D5A)
                )
                Text(
                    text = doctor.speciality,
                    color = Color(0xFF4CB7C2),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Available: ${doctor.days}",
                    color = Color(0xFF6AA8B0),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Chat icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CB7C2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💬",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}