package ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import com.example.patienttracker.R
import com.example.patienttracker.ui.screens.common.BackButton


@Composable
fun PatientRecordOptionsScreen(navController: NavController) {
    val gradientPrescriptions = Brush.linearGradient(listOf(Color(0xFF6DD5FA), Color(0xFF2980B9)))
    val gradientReports = Brush.linearGradient(listOf(Color(0xFFB2EBF2), Color(0xFF0097A7)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FC))
            .padding(24.dp)
    ) {
        // --- Back Button Header ---
        BackButton(
            navController = navController,
            modifier = Modifier.padding(start = 6.dp, top = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp), // leave space for header
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Brand Logo ---
            Image(
                painter = painterResource(id = R.drawable.ic_healthtrack_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 32.dp)
            )

            // --- Prescriptions & Diagnosis Card ---
            RecordOptionCard(
                title = "Prescriptions & Diagnosis",
                iconRes = R.drawable.ic_doctor,
                gradient = gradientPrescriptions
            ) {
                navController.navigate("patient_prescriptions_screen")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Reports Card ---
            RecordOptionCard(
                title = "Reports",
                iconRes = R.drawable.ic_records,
                gradient = gradientReports
            ) {
                navController.navigate("patient_reports_screen")
            }
        }
    }
}

@Composable
private fun RecordOptionCard(
    title: String,
    iconRes: Int,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                )
            }
        }
    }
}