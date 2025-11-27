package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.R
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.data.FavoritesManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    val context = LocalContext.current
    val db = Firebase.firestore
    var favoriteDoctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val userId = remember {
        runBlocking {
            AuthManager.getCurrentUserProfile()?.humanId
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Favorite Doctors",
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (userId == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Please log in to view favorites")
                }
                return@Column
            }

            LaunchedEffect(userId) {
                loading = true
                val favoriteIds = FavoritesManager.getFavorites(context, userId)
                
                if (favoriteIds.isEmpty()) {
                    favoriteDoctors = emptyList()
                    loading = false
                    return@LaunchedEffect
                }

                // Fetch doctor details for all favorite IDs
                val doctorsList = mutableListOf<DoctorFull>()
                
                try {
                    // Use await() for better coroutine handling
                    val results = favoriteIds.map { doctorId ->
                        db.collection("users")
                            .whereEqualTo("humanId", doctorId)
                            .whereEqualTo("role", "doctor")
                            .get()
                            .await()
                    }
                    
                    results.forEach { snapshot ->
                        snapshot.documents.forEach { doc ->
                            val id = doc.getString("humanId") ?: return@forEach
                            val first = doc.getString("firstName") ?: ""
                            val last = doc.getString("lastName") ?: ""
                            val email = doc.getString("email") ?: ""
                            val phone = doc.getString("phone") ?: ""
                            val speciality = doc.getString("speciality") ?: ""

                            val daysList = (doc.get("days") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            val daysDisplay = if (daysList.isNotEmpty()) daysList.joinToString(", ") else ""

                            val timingsRaw = (doc.get("timings") as? List<*>)?.mapNotNull {
                                when (it) {
                                    is Long -> it.toInt()
                                    is Int -> it
                                    else -> null
                                }
                            } ?: emptyList()

                            val timingsDisplay = if (timingsRaw.size >= 2) {
                                "${formatTimeHHmm(timingsRaw[0])} – ${formatTimeHHmm(timingsRaw[1])}"
                            } else {
                                ""
                            }
                            
                            // Get fees - handle both string and number formats
                            val fees = when (val feesRaw = doc.get("fees")) {
                                is String -> feesRaw
                                is Long -> "Rs. $feesRaw"
                                is Double -> "Rs. ${feesRaw.toInt()}"
                                else -> ""
                            }
                            
                            val clinicAddress = doc.getString("clinicAddress") ?: ""

                            doctorsList.add(
                                DoctorFull(
                                    id = id,
                                    firstName = first,
                                    lastName = last,
                                    email = email,
                                    phone = phone,
                                    speciality = speciality,
                                    days = daysDisplay,
                                    timings = timingsDisplay,
                                    fees = fees,
                                    clinicAddress = clinicAddress
                                )
                            )
                        }
                    }
                    
                    favoriteDoctors = doctorsList.toList()
                } catch (e: Exception) {
                    // Handle error
                    favoriteDoctors = emptyList()
                } finally {
                    loading = false
                }
            }

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (favoriteDoctors.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No favorite doctors yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "Add doctors to favorites by clicking the heart icon",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(favoriteDoctors) { doctor ->
                        FavoriteDoctorCard(
                            doctor = doctor,
                            onBookClick = {
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("selectedDoctor", doctor)
                                navController.navigate("book_appointment")
                            },
                            onFavoriteClick = {
                                coroutineScope.launch {
                                    FavoritesManager.removeFavorite(context, doctor.id)
                                    // Remove from local state immediately for better UX
                                    favoriteDoctors = favoriteDoctors.filter { it.id != doctor.id }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteDoctorCard(
    doctor: DoctorFull,
    onBookClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = doctor.speciality,
                        color = Color(0xFF4CB7C2),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    if (doctor.days.isNotBlank()) {
                        Text(
                            "Days: ${doctor.days}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    if (doctor.timings.isNotBlank()) {
                        Text(
                            "Timings: ${doctor.timings}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    if (doctor.fees.isNotBlank()) {
                        Text(
                            "Fees: ${doctor.fees}",
                            color = Color(0xFF4CB7C2),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    if (doctor.clinicAddress.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color(0xFF8DC2C8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = doctor.clinicAddress,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // Heart icon for favorites - filled since this is favorites screen
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favourite_filled),
                        contentDescription = "Remove from favorites",
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onBookClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CC7CD)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Book Appointment", color = Color.White)
            }
        }
    }
}

// Helper function (same as in DoctorListScreen)
private fun formatTimeHHmm(value: Int): String {
    val hours24 = value / 100
    val minutes = value % 100

    val amPm = if (hours24 >= 12) "pm" else "am"
    val hours12 = when {
        hours24 == 0 -> 12
        hours24 > 12 -> hours24 - 12
        else -> hours24
    }

    return String.format("%d:%02d %s", hours12, minutes, amPm)
}