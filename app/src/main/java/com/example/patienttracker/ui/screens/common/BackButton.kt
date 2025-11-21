package com.example.patienttracker.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.R
import androidx.compose.foundation.shape.CircleShape

@Composable
fun BackButton(navController: NavController, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clickable { navController.popBackStack() },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_back_arrow),
            contentDescription = "Back",
            tint = Color(0xFF4CB7C2),
            modifier = Modifier.size(28.dp)
        )
    }
}