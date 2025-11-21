package ui.screens.patient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecordViewerScreen(
    navController: NavController,
    record: MedicalRecord
) {
    val context = LocalContext.current
    val bitmaps = remember { mutableStateListOf<Bitmap>() }

    // Decode bytes → bitmaps (image or pdf pages)
    LaunchedEffect(record.id) {
        bitmaps.clear()
        val bytes = record.data ?: return@LaunchedEffect

        if (record.mimeType.startsWith("image")) {
            // Simple image
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bmp ->
                bitmaps.add(bmp)
            }
        } else if (record.mimeType.contains("pdf")) {
            // Render all PDF pages using PdfRenderer
            val temp = File(context.cacheDir, "rec_${record.id}.pdf")
            temp.outputStream().use { it.write(bytes) }

            val pfd = ParcelFileDescriptor.open(
                temp,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(
                    page.width,
                    page.height,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bmp)
            }

            renderer.close()
            pfd.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = record.title.ifBlank { record.fileName },
                        color = Color(0xFF4CB7C2)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF4CB7C2),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = Color(0xFF4CB7C2),
                    titleContentColor = Color(0xFF4CB7C2)
                )
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(inner)
        ) {
            if (bitmaps.isEmpty()) {
                // Either still loading or unsupported type
                Text(
                    text = "Unable to display this file.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // If there are multiple pages (typical PDF), zoom/pan the whole column together
                if (bitmaps.size > 1) {
                    MultiPageZoomable(bitmaps)
                } else {
                    // Single image/page – keep old behavior
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        bitmaps.forEach { bmp ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                ZoomableImage(bmp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Used for single images / single-page docs.
 * Each instance has its own zoom state.
 */
@Composable
private fun ZoomableImage(bmp: Bitmap) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    val newOffset = offset + pan
                    scale = newScale
                    offset = newOffset
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Used for multi-page PDFs. The **entire document** (all pages)
 * shares a single zoom + pan state, so it behaves like one big entity.
 */
@Composable
private fun MultiPageZoomable(bitmaps: List<Bitmap>) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(bitmaps) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    val newOffset = offset + pan
                    scale = newScale
                    offset = newOffset
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            bitmaps.forEach { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                        .padding(vertical = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}