package ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.border
import com.example.patienttracker.R
import com.example.patienttracker.ui.screens.common.BackButton
import com.example.patienttracker.ui.screens.patient.PatientBottomBar


@Composable
fun PatientRecordOptionsScreen(
    navController: NavController,
    doctorKey: String,
    doctorName: String = ""
) {
    val decodedDoctorName = try {
        java.net.URLDecoder.decode(doctorName, "UTF-8")
    } catch (e: Exception) {
        doctorName
    }

    Scaffold(
        bottomBar = { PatientBottomBar(navController) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .padding(padding)
        ) {
            // --- Header Row with Back Button and Title ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(
                    navController = navController,
                    modifier = Modifier
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (decodedDoctorName.isNotBlank()) "Records – $decodedDoctorName" else "Records",
                    color = Color(0xFF4CB7C2),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- Prescriptions & Diagnosis Card ---
                RecordOptionCard(
                    title = "Prescriptions & Diagnosis",
                    iconRes = R.drawable.ic_doctor
                ) {
                    val encodedName = java.net.URLEncoder.encode(decodedDoctorName, "UTF-8")
                    navController.navigate("patient_prescriptions_screen/$doctorKey/$encodedName")
                }

                Spacer(modifier = Modifier.height(40.dp))

                // --- Reports Card ---
                RecordOptionCard(
                    title = "Reports",
                    iconRes = R.drawable.ic_record
                ) {
                    val encodedName = java.net.URLEncoder.encode(decodedDoctorName, "UTF-8")
                    navController.navigate("patient_reports_screen/$doctorKey/$encodedName")
                }
            }
        }
    }
}

@Composable
private fun RecordOptionCard(
    title: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(24.dp)
            )
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        .size(72.dp)
                        .padding(bottom = 16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }
        }
    }
}