package com.example.patienttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.R

@Composable
fun BackButton(navController: NavController, modifier: Modifier) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, top = 16.dp)
            .size(48.dp)
            .shadow(6.dp, CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), CircleShape)
            .clickable { navController.popBackStack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_back_arrow),
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
