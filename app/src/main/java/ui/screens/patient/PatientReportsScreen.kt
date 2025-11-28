package ui.screens.patient

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.patienttracker.auth.AuthManager
import androidx.compose.material3.MaterialTheme
import com.example.patienttracker.ui.screens.doctor.DoctorBottomBar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.patienttracker.ui.screens.patient.PatientBottomBar
import java.io.File
import java.io.Serializable

data class MedicalRecord(
    val id: String = "",
    val patientId: String = "",
    val title: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val bytes: ByteArray? = null,
    val createdAt: Timestamp? = null,
    val data: ByteArray? = null
) : Serializable

// Helper function to send automatic chat message when document is uploaded
private fun sendDocumentUploadMessage(
    db: FirebaseFirestore,
    conversationId: String,
    senderId: String,
    senderRole: String,
    doctorId: String,
    patientId: String,
    documentName: String
) {
    if (conversationId.isBlank()) return
    
    // Determine the collection based on who uploaded
    val uploadCollection = if (senderRole == "doctor") "prescriptions" else "records"
    
    val msgData = hashMapOf(
        "text" to "I have uploaded a new document: $documentName",
        "senderId" to senderId,
        "senderRole" to senderRole,
        "doctorId" to doctorId,
        "patientId" to patientId,
        "timestamp" to Timestamp.now(),
        "status" to "SENT",
        // Document upload metadata for clickable link
        "isDocumentUpload" to true,
        "documentName" to documentName,
        "uploadedBy" to senderRole,
        "uploadCollection" to uploadCollection
    )

    db.collection("conversations")
        .document(conversationId)
        .collection("messages")
        .add(msgData)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientReportsScreen(
    navController: NavController,
    context: Context,
    patientIdOverride: String? = null,
    canUpload: Boolean = true,
    collectionOverride: String? = null,
    title: String = "My Medical Records",
    entitySingular: String = "record",
    entityPlural: String = "records",
    // Chat integration parameters
    fromChat: Boolean = false,
    chatDoctorId: String = "",
    chatPatientId: String = "",
    chatConversationId: String = "",
    // Doctor filter for scoped document access (security)
    doctorIdFilter: String = ""
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val collection = collectionOverride ?: "records"

    // Who is logged in?
    var currentRole by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // may throw, so wrap in try if you want
        currentRole = try {
            com.example.patienttracker.auth.AuthManager.getCurrentUserRole()
        } catch (e: Exception) {
            null
        }
    }

// Only allow uploads for the right role + collection
    val effectiveCanUpload = remember(canUpload, currentRole, collection) {
        when (collection) {
            // patients upload their own lab reports / scans
            "records" -> canUpload && currentRole == "patient"
            // only doctors can upload prescriptions
            "prescriptions" -> canUpload && currentRole == "doctor"
            // any other collection – default to read-only
            else -> false
        }
    }

    var patientId by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recordPendingDelete by remember { mutableStateOf<MedicalRecord?>(null) }

    // ---------- Load current patientId & records ----------
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            patientId = patientIdOverride ?: profile?.humanId

            if (patientId == null) {
                error = "Could not find patient ID."
                loading = false
                return@LaunchedEffect
            }

            // Query by patientId and order by createdAt
            // Note: doctorId filtering is done client-side to avoid requiring composite indexes
            db.collection(collection)
                .whereEqualTo("patientId", patientId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        error = e.message
                        loading = false
                        return@addSnapshotListener
                    }

                    val list = snapshot?.documents?.mapNotNull { doc ->
                        val docDoctorId = doc.getString("doctorId") ?: ""
                        
                        // Security filter: if doctorIdFilter is set, only show docs for that doctor
                        // Also show docs with no doctorId (legacy data) only if no filter is set
                        if (doctorIdFilter.isNotBlank() && docDoctorId != doctorIdFilter) {
                            return@mapNotNull null  // Skip documents not for this doctor
                        }
                        
                        MedicalRecord(
                            id = doc.id,
                            patientId = doc.getString("patientId") ?: "",
                            title = doc.getString("title") ?: "",
                            fileName = doc.getString("fileName") ?: "",
                            mimeType = doc.getString("mimeType") ?: "",
                            createdAt = doc.getTimestamp("createdAt"),
                            data = doc.getBlob("data")?.toBytes()
                        )
                    } ?: emptyList()

                    records = list
                    loading = false
                }
        } catch (e: Exception) {
            error = e.message
            loading = false
        }
    }

    // ---------- File picker launcher ----------
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // if user cancelled
        if (uri == null) return@rememberLauncherForActivityResult

        val pid = patientId
        if (pid.isNullOrBlank()) {
            Toast.makeText(context, "Missing patient ID", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        val cr = context.contentResolver

        val fileName = queryDisplayName(cr, uri) ?: "record_${System.currentTimeMillis()}"
        val mimeType = cr.getType(uri) ?: "application/octet-stream"

        // read bytes
        val inputStream = cr.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes == null) {
            Toast.makeText(context, "Cannot read selected file.", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        // Use doctorIdFilter for scoping document to specific doctor-patient relationship
        // For chat uploads, use chatDoctorId as fallback
        val effectiveDoctorId = doctorIdFilter.ifBlank { chatDoctorId }
        
        val recordMap = hashMapOf(
            "patientId" to pid,
            "doctorId" to effectiveDoctorId,  // Security: scope document to specific doctor
            "title" to fileName,
            "fileName" to fileName,
            "mimeType" to mimeType,
            "data" to Blob.fromBytes(bytes),
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection(collection)
            .add(recordMap)
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    "${entitySingular.replaceFirstChar { it.uppercase() }} saved!",
                    Toast.LENGTH_SHORT
                ).show()
                
                // If we came from chat, send automatic message
                if (fromChat && chatConversationId.isNotBlank()) {
                    val senderRole = currentRole ?: "patient"
                    val senderId = if (senderRole == "doctor") chatDoctorId else chatPatientId
                    sendDocumentUploadMessage(
                        db = db,
                        conversationId = chatConversationId,
                        senderId = senderId,
                        senderRole = senderRole,
                        doctorId = chatDoctorId,
                        patientId = chatPatientId,
                        documentName = fileName
                    )
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save ${entitySingular}: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ---------- Helper function to upload from URI ----------
    fun uploadFromUri(uri: Uri, isCamera: Boolean = false) {
        val pid = patientId
        if (pid.isNullOrBlank()) {
            Toast.makeText(context, "Missing patient ID", Toast.LENGTH_LONG).show()
            return
        }

        val cr = context.contentResolver
        val fileName = if (isCamera) {
            "photo_${System.currentTimeMillis()}.jpg"
        } else {
            queryDisplayName(cr, uri) ?: "record_${System.currentTimeMillis()}"
        }
        val mimeType = cr.getType(uri) ?: "image/jpeg"

        val inputStream = cr.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes == null) {
            Toast.makeText(context, "Cannot read selected file.", Toast.LENGTH_LONG).show()
            return
        }

        // Use doctorIdFilter for scoping document to specific doctor-patient relationship
        // For chat uploads, use chatDoctorId as fallback
        val effectiveDoctorId = doctorIdFilter.ifBlank { chatDoctorId }
        
        val recordMap = hashMapOf(
            "patientId" to pid,
            "doctorId" to effectiveDoctorId,  // Security: scope document to specific doctor
            "title" to fileName,
            "fileName" to fileName,
            "mimeType" to mimeType,
            "data" to Blob.fromBytes(bytes),
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection(collection)
            .add(recordMap)
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    "${entitySingular.replaceFirstChar { it.uppercase() }} saved!",
                    Toast.LENGTH_SHORT
                ).show()

                if (fromChat && chatConversationId.isNotBlank()) {
                    val senderRole = currentRole ?: "patient"
                    val senderId = if (senderRole == "doctor") chatDoctorId else chatPatientId
                    sendDocumentUploadMessage(
                        db = db,
                        conversationId = chatConversationId,
                        senderId = senderId,
                        senderRole = senderRole,
                        doctorId = chatDoctorId,
                        patientId = chatPatientId,
                        documentName = fileName
                    )
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save ${entitySingular}: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ---------- Photo picker launcher ----------
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadFromUri(uri, isCamera = false)
        }
    }

    // ---------- Camera launcher ----------
    val cameraImageUri = remember {
        FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
        )
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            uploadFromUri(cameraImageUri, isCamera = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = Color(0xFF4CB7C2)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF4CB7C2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = Color(0xFF4CB7C2),
                    titleContentColor = Color(0xFF4CB7C2)
                )
            )
        },
        bottomBar = {
            // Show correct bottom bar based on user role
            when (currentRole) {
                "doctor" -> DoctorBottomBar(navController, selectedTab = 2) // Patients tab
                else -> PatientBottomBar(navController)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                records.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // empty state text
                        Text(
                            text = "You don't have any $entityPlural yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (effectiveCanUpload) {
                            Spacer(Modifier.height(16.dp))
                            
                            var showUploadMenu by remember { mutableStateOf(false) }
                            
                            Box {
                                Button(
                                    onClick = { showUploadMenu = true }
                                ) {
                                    Text("Upload ${entitySingular.replaceFirstChar { it.uppercase() }}")
                                }
                                
                                DropdownMenu(
                                    expanded = showUploadMenu,
                                    onDismissRequest = { showUploadMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Camera") },
                                        onClick = {
                                            showUploadMenu = false
                                            takePictureLauncher.launch(cameraImageUri)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Camera",
                                                tint = Color(0xFF4CB7C2)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Photos") },
                                        onClick = {
                                            showUploadMenu = false
                                            pickImageLauncher.launch("image/*")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Photo,
                                                contentDescription = "Photos",
                                                tint = Color(0xFF4CB7C2)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Files") },
                                        onClick = {
                                            showUploadMenu = false
                                            pickFileLauncher.launch(arrayOf("application/pdf", "image/*"))
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.InsertDriveFile,
                                                contentDescription = "Files",
                                                tint = Color(0xFF4CB7C2)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (effectiveCanUpload) {
                            var showUploadMenu by remember { mutableStateOf(false) }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Button(
                                    onClick = { showUploadMenu = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Add New ${entitySingular.replaceFirstChar { it.uppercase() }}")
                                }
                                
                                DropdownMenu(
                                    expanded = showUploadMenu,
                                    onDismissRequest = { showUploadMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Camera") },
                                        onClick = {
                                            showUploadMenu = false
                                            takePictureLauncher.launch(cameraImageUri)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Camera",
                                                tint = Color(0xFF4CB7C2)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Photos") },
                                        onClick = {
                                            showUploadMenu = false
                                            pickImageLauncher.launch("image/*")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Photo,
                                                contentDescription = "Photos",
                                                tint = Color(0xFF4CB7C2)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Files") },
                                        onClick = {
                                            showUploadMenu = false
                                            pickFileLauncher.launch(arrayOf("application/pdf", "image/*"))
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.InsertDriveFile,
                                                contentDescription = "Files",
                                                tint = Color(0xFF4CB7C2)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(records) { record ->
                                RecordRow(
                                    record = record,
                                    canDelete = effectiveCanUpload,
                                    onClick = {
                                        navController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("selectedRecord", record)
                                        navController.navigate("patient_record_viewer")
                                    },
                                    onDelete = {
                                        recordPendingDelete = record
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (effectiveCanUpload) {
                val toDelete = recordPendingDelete
                if (toDelete != null) {
                    AlertDialog(
                        onDismissRequest = { recordPendingDelete = null },
                        title = {
                            Text("Delete ${entitySingular.replaceFirstChar { it.uppercase() }}?")
                        },
                        text = {
                            Text("This will permanently delete this $entitySingular for everyone.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    recordPendingDelete = null
                                    db.collection(collection)
                                        .document(toDelete.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "${entitySingular.replaceFirstChar { it.uppercase() }} deleted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Failed to delete $entitySingular: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { recordPendingDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

// ---------- UI for one row ----------
@Composable
private fun RecordRow(
    record: MedicalRecord,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simple icon based on type
            val label = when {
                record.mimeType.startsWith("image") -> "Image"
                record.mimeType.contains("pdf") -> "PDF"
                else -> "File"
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF0EA5B8),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.widthIn(min = 48.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title.ifBlank { record.fileName },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                record.createdAt?.toDate()?.let { date ->
                    Text(
                        text = date.toString(), // TODO: pretty-format later
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (canDelete) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onDelete() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}


// ---------- Helpers ----------

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    val cursor: Cursor? = resolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && index >= 0) {
            name = it.getString(index)
        }
    }
    return name
}

private fun openRecord(context: Context, record: MedicalRecord) {
    val bytes = record.data
    if (bytes == null) {
        Toast.makeText(context, "No file data stored for this record.", Toast.LENGTH_LONG).show()
        return
    }

    // Create a temp file in cache
    val safeName = if (record.fileName.isNotBlank()) record.fileName else "record_${record.id}"
    val file = File(context.cacheDir, safeName)
    file.outputStream().use { it.write(bytes) }

    // Build content:// URI via FileProvider
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".provider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, record.mimeType.ifBlank { "application/octet-stream" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // Avoid crash if no app can handle this mime type
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_LONG).show()
    }
}