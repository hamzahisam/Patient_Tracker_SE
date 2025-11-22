package com.example.patienttracker.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.auth.AuthManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedProfileScreen(navController: NavController) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val accent = Color(0xFF4CB7C2)

    val scope = rememberCoroutineScope()

    // Load current user data
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val profile = AuthManager.getCurrentUserProfile()
        firstName = profile?.firstName ?: ""
        lastName = profile?.lastName ?: ""
        email = Firebase.auth.currentUser?.email ?: ""
        phone = profile?.phoneNumber ?: ""
        role = profile?.role ?: ""
    }

    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        Firebase.auth.signOut()
                        navController.navigate("role") {
                            popUpTo(0)
                        }
                    }
                ) {
                    Text("Confirm", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Account Settings",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val success = AuthManager.updateCurrentUserProfile(
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        phoneNumber = phone.trim()
                                    )
                                    if (success) {
                                        isEditing = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { isEditing = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Profile Picture Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = buildString {
                            if (firstName.isNotBlank()) append(firstName.first().uppercaseChar())
                            if (lastName.isNotBlank()) append(lastName.first().uppercaseChar())
                        }.ifBlank { "P" }
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "$firstName $lastName",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )

                    val roleLabel = when {
                        role.equals("doctor", ignoreCase = true) -> "Doctor"
                        role.equals("patient", ignoreCase = true) -> "Patient"
                        role.isNotBlank() -> role.replaceFirstChar { it.titlecase() }
                        else -> ""
                    }

                    if (roleLabel.isNotEmpty()) {
                        Text(
                            text = roleLabel,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Personal Information Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Personal Information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )

                        // First Name
                        ProfileField(
                            label = "First Name",
                            value = firstName,
                            isEditing = isEditing,
                            onValueChange = { firstName = it },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Text
                            )
                        )

                        // Last Name
                        ProfileField(
                            label = "Last Name",
                            value = lastName,
                            isEditing = isEditing,
                            onValueChange = { lastName = it },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Text
                            )
                        )

                        // Email (read-only for now)
                        ProfileField(
                            label = "Email",
                            value = email,
                            isEditing = false, // Email usually can't be edited easily
                            onValueChange = { },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Email
                            )
                        )

                        // Phone Number
                        ProfileField(
                            label = "Phone Number",
                            value = phone,
                            isEditing = isEditing,
                            onValueChange = { phone = it },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Phone
                            )
                        )
                    }
                }

                // Add some extra space at the bottom of the scrollable content
                Spacer(modifier = Modifier.height(80.dp))
            }

            // Logout Button - Fixed to be always visible at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4444) // bright red
                    )
                ) {
                    Text(
                        text = "Log Out",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF4CB7C2)
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = keyboardOptions,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        } else {
            Text(
                text = value.ifBlank { "Not set" },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4CB7C2).copy(
                    alpha = if (value.isBlank()) 0.5f else 1f
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }
}