package com.example.patienttracker.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.R
import com.example.patienttracker.ui.screens.common.BackButton


@Composable
fun SpecialtiesScreen(navController: NavController) {

    val specialties = listOf(
        Spec("Cardiology", com.example.patienttracker.R.drawable.ic_cardiology),
        Spec("Dermatology", com.example.patienttracker.R.drawable.ic_dermatology),
        Spec("General Medicine", com.example.patienttracker.R.drawable.ic_general_medicine),
        Spec("Gynecology", com.example.patienttracker.R.drawable.ic_gynecology),
        Spec("Odontology", com.example.patienttracker.R.drawable.ic_odontology),
        Spec("Oncology", R.drawable.ic_oncology),
    )

    Scaffold(
        bottomBar = {
            PatientBottomBar(navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top row: back + title
            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(
                    navController = navController
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Specialties",
                    color = Color(0xFF4CB7C2),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3 rows, 2 icons per row
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpecialtyIcon(
                        spec = specialties[0],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 4.dp)
                    ) { navController.navigate("doctor_list/Cardiology") }

                    SpecialtyIcon(
                        spec = specialties[1],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 4.dp)
                    ) { navController.navigate("doctor_list/Dermatology") }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpecialtyIcon(
                        spec = specialties[2],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 4.dp)
                    ) { navController.navigate("doctor_list/General Medicine") }

                    SpecialtyIcon(
                        spec = specialties[3],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 4.dp)
                    ) { navController.navigate("doctor_list/Gynecology") }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpecialtyIcon(
                        spec = specialties[4],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 4.dp)
                    ) { navController.navigate("doctor_list/Odontology") }

                    SpecialtyIcon(
                        spec = specialties[5],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 4.dp)
                    ) { navController.navigate("doctor_list/Oncology") }
                }
            }
        }
    }
}

@Composable
private fun SpecialtyIcon(
    spec: Spec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = spec.iconRes),
            contentDescription = spec.title,
            modifier = Modifier.fillMaxSize()
        )
    }
}