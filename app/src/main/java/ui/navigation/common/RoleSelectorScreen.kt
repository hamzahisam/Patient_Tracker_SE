package com.example.patienttracker.ui.screens.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.patienttracker.R

@Composable
fun RoleSelectorScreen(onRoleSelected: (String) -> Unit) {
    val accentBlue = Color(0xFF4CB7C2)
    val cardBackground = Color(0xFF14171C)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_healthtrack_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(192.dp)
                    .padding(bottom = 24.dp)
            )
            Text(
                text = "Welcome to Patient Tracker",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = accentBlue
            )

            Spacer(Modifier.height(40.dp))

            // --- Patient Card ---
            RoleSelectorCard(
                title = "Patient",
                iconRes = R.drawable.ic_patient,
                accentBlue = accentBlue,
                cardBackground = cardBackground,
                onClick = { onRoleSelected("patient") }
            )

            Spacer(Modifier.height(20.dp))

            // --- Doctor Card ---
            RoleSelectorCard(
                title = "Doctor",
                iconRes = R.drawable.ic_doctor,
                accentBlue = accentBlue,
                cardBackground = cardBackground,
                onClick = { onRoleSelected("doctor") }
            )
        }
    }
}

@Composable
private fun RoleSelectorCard(
    title: String,
    iconRes: Int,
    accentBlue: Color,
    cardBackground: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(72.dp)
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        border = BorderStroke(2.dp, accentBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "$title Icon",
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE5E5E5)
            )
        }
    }
}
