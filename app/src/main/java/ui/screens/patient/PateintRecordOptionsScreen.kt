package ui.screens.patient

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import com.example.patienttracker.R
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.ui.screens.common.BackButton
import com.example.patienttracker.ui.screens.patient.PatientBottomBar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


data class HealthSummary(
    val gender: String = "",
    val bloodGroup: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val bloodPressure: String = "",
    val heartRate: String = "",
    val bloodSugar: String = "",
    val lastUpdated: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
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

    val db = remember { Firebase.firestore }
    val scope = rememberCoroutineScope()
    
    var healthSummary by remember { mutableStateOf(HealthSummary()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load health summary from Firestore
    LaunchedEffect(Unit) {
        try {
            val uid = AuthManager.getCurrentUserId()
            if (uid != null) {
                val doc = db.collection("users").document(uid).get().await()
                healthSummary = HealthSummary(
                    gender = doc.getString("gender") ?: "",
                    bloodGroup = doc.getString("bloodGroup") ?: "",
                    age = doc.getString("age") ?: "",
                    height = doc.getString("height") ?: "",
                    weight = doc.getString("weight") ?: "",
                    bloodPressure = doc.getString("bloodPressure") ?: "",
                    heartRate = doc.getString("heartRate") ?: "",
                    bloodSugar = doc.getString("bloodSugar") ?: "",
                    lastUpdated = doc.getLong("healthSummaryLastUpdated") ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e("PatientRecords", "Error loading health summary", e)
        } finally {
            isLoading = false
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        HealthSummaryEditDialog(
            healthSummary = healthSummary,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                scope.launch {
                    try {
                        val uid = AuthManager.getCurrentUserId()
                        if (uid != null) {
                            val updates = mapOf(
                                "gender" to updated.gender,
                                "bloodGroup" to updated.bloodGroup,
                                "age" to updated.age,
                                "height" to updated.height,
                                "weight" to updated.weight,
                                "bloodPressure" to updated.bloodPressure,
                                "heartRate" to updated.heartRate,
                                "bloodSugar" to updated.bloodSugar,
                                "healthSummaryLastUpdated" to System.currentTimeMillis()
                            )
                            db.collection("users").document(uid).update(updates).await()
                            healthSummary = updated.copy(lastUpdated = System.currentTimeMillis())
                        }
                    } catch (e: Exception) {
                        Log.e("PatientRecords", "Error saving health summary", e)
                    }
                }
                showEditDialog = false
            }
        )
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
                    .padding(top = 72.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Health Summary Card ---
                HealthSummaryCard(
                    healthSummary = healthSummary,
                    isLoading = isLoading,
                    isEditable = true,
                    onEditClick = { showEditDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- Prescriptions & Diagnosis Card ---
                RecordOptionCard(
                    title = "Prescriptions & Diagnosis",
                    iconRes = R.drawable.ic_doctor,
                    subtitle = null
                ) {
                    val encodedName = java.net.URLEncoder.encode(decodedDoctorName, "UTF-8")
                    navController.navigate("patient_prescriptions_screen/$doctorKey/$encodedName")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Reports Card ---
                RecordOptionCard(
                    title = "Reports",
                    iconRes = R.drawable.ic_record,
                    subtitle = "Upload here"
                ) {
                    val encodedName = java.net.URLEncoder.encode(decodedDoctorName, "UTF-8")
                    navController.navigate("patient_reports_screen/$doctorKey/$encodedName")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RecordOptionCard(
    title: String,
    iconRes: Int,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
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
                        .size(56.dp)
                        .padding(bottom = 12.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CB7C2)
                    )
                }
            }
        }
    }
}

@Composable
fun HealthSummaryCard(
    healthSummary: HealthSummary,
    isLoading: Boolean,
    isEditable: Boolean,
    onEditClick: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val lastUpdatedText = if (healthSummary.lastUpdated > 0) {
        "Last updated: ${dateFormat.format(Date(healthSummary.lastUpdated))}"
    } else {
        "Not yet updated"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color(0xFF4CB7C2),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF4CB7C2).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Health",
                        tint = Color(0xFF4CB7C2),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Health Summary",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CB7C2))
                }
            } else {
                // Row 1: Gender & Blood Type
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HealthStatRow(
                        label = "Gender:",
                        value = healthSummary.gender.ifBlank { "Not set" },
                        modifier = Modifier.weight(1f)
                    )
                    HealthStatRow(
                        label = "Blood Type:",
                        value = healthSummary.bloodGroup.ifBlank { "Not set" },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFF4CB7C2).copy(alpha = 0.2f))

                // Row 2: Age & Weight
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HealthStatRow(
                        label = "Age:",
                        value = if (healthSummary.age.isNotBlank()) "${healthSummary.age} Years" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                    HealthStatRow(
                        label = "Weight:",
                        value = if (healthSummary.weight.isNotBlank()) "${healthSummary.weight} Kg" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFF4CB7C2).copy(alpha = 0.2f))

                // Row 3: Height & BP
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HealthStatRow(
                        label = "Height:",
                        value = if (healthSummary.height.isNotBlank()) "${healthSummary.height} cm" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                    HealthStatRow(
                        label = "BP:",
                        value = if (healthSummary.bloodPressure.isNotBlank()) "${healthSummary.bloodPressure}" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFF4CB7C2).copy(alpha = 0.2f))

                // Row 4: Heart Rate & Blood Sugar
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HealthStatRow(
                        label = "Heart Rate:",
                        value = if (healthSummary.heartRate.isNotBlank()) "${healthSummary.heartRate} bpm" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                    HealthStatRow(
                        label = "Sugar:",
                        value = if (healthSummary.bloodSugar.isNotBlank()) "${healthSummary.bloodSugar} mg/dL" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            
            // Bottom Row - Last Updated on left, Edit button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last Updated Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF4CB7C2).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = lastUpdatedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CB7C2),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                // Edit Button (only for patient)
                if (isEditable) {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CB7C2)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF4CB7C2)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthSummaryEditDialog(
    healthSummary: HealthSummary,
    onDismiss: () -> Unit,
    onSave: (HealthSummary) -> Unit
) {
    var gender by remember { mutableStateOf(healthSummary.gender) }
    var bloodGroup by remember { mutableStateOf(healthSummary.bloodGroup) }
    var age by remember { mutableStateOf(healthSummary.age) }
    var height by remember { mutableStateOf(healthSummary.height) }
    var weight by remember { mutableStateOf(healthSummary.weight) }
    var bloodPressure by remember { mutableStateOf(healthSummary.bloodPressure) }
    var heartRate by remember { mutableStateOf(healthSummary.heartRate) }
    var bloodSugar by remember { mutableStateOf(healthSummary.bloodSugar) }
    var genderExpanded by remember { mutableStateOf(false) }
    var bloodExpanded by remember { mutableStateOf(false) }
    
    val genders = listOf("Male", "Female", "Other")
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Health Summary",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF4CB7C2)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Gender Dropdown
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CB7C2),
                            focusedLabelColor = Color(0xFF4CB7C2),
                            cursorColor = Color(0xFF4CB7C2)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        genders.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    gender = g
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                // Blood Group Dropdown
                ExposedDropdownMenuBox(
                    expanded = bloodExpanded,
                    onExpandedChange = { bloodExpanded = !bloodExpanded }
                ) {
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Blood Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CB7C2),
                            focusedLabelColor = Color(0xFF4CB7C2),
                            cursorColor = Color(0xFF4CB7C2)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = bloodExpanded,
                        onDismissRequest = { bloodExpanded = false }
                    ) {
                        bloodGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group) },
                                onClick = {
                                    bloodGroup = group
                                    bloodExpanded = false
                                }
                            )
                        }
                    }
                }

                // Age
                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) age = it },
                    label = { Text("Age (years)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CB7C2),
                        focusedLabelColor = Color(0xFF4CB7C2),
                        cursorColor = Color(0xFF4CB7C2)
                    ),
                    singleLine = true
                )

                // Height
                OutlinedTextField(
                    value = height,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) height = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CB7C2),
                        focusedLabelColor = Color(0xFF4CB7C2),
                        cursorColor = Color(0xFF4CB7C2)
                    ),
                    singleLine = true
                )

                // Weight
                OutlinedTextField(
                    value = weight,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CB7C2),
                        focusedLabelColor = Color(0xFF4CB7C2),
                        cursorColor = Color(0xFF4CB7C2)
                    ),
                    singleLine = true
                )

                // Blood Pressure
                OutlinedTextField(
                    value = bloodPressure,
                    onValueChange = { 
                        // Allow digits and forward slash for BP format like 120/80
                        if (it.all { c -> c.isDigit() || c == '/' } && it.length <= 7) bloodPressure = it 
                    },
                    label = { Text("Blood Pressure (e.g., 120/80)") },
                    placeholder = { Text("120/80") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CB7C2),
                        focusedLabelColor = Color(0xFF4CB7C2),
                        cursorColor = Color(0xFF4CB7C2)
                    ),
                    singleLine = true
                )

                // Heart Rate
                OutlinedTextField(
                    value = heartRate,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) heartRate = it },
                    label = { Text("Heart Rate (bpm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CB7C2),
                        focusedLabelColor = Color(0xFF4CB7C2),
                        cursorColor = Color(0xFF4CB7C2)
                    ),
                    singleLine = true
                )

                // Blood Sugar
                OutlinedTextField(
                    value = bloodSugar,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) bloodSugar = it },
                    label = { Text("Blood Sugar (mg/dL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CB7C2),
                        focusedLabelColor = Color(0xFF4CB7C2),
                        cursorColor = Color(0xFF4CB7C2)
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(HealthSummary(
                        gender = gender,
                        bloodGroup = bloodGroup,
                        age = age,
                        height = height,
                        weight = weight,
                        bloodPressure = bloodPressure,
                        heartRate = heartRate,
                        bloodSugar = bloodSugar
                    ))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CB7C2)
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF4CB7C2))
            }
        }
    )
}