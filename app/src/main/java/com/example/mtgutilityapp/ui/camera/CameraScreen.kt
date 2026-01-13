package com.example.mtgutilityapp.ui.camera

import android.Manifest
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.mtgutilityapp.ui.result.ResultOverlay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                onImageCaptured = { imageProxy ->
                    viewModel.processImage(imageProxy, context)
                }
            )

            // Scanning Overlay
            ScannerOverlay()

            // Header Text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Align card within the frame",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Card will be scanned automatically",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            // Error message
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Result overlay
            uiState.selectedCard?.let { card ->
                ResultOverlay(
                    card = card,
                    onSave = {
                        viewModel.saveCard()
                        viewModel.dismissCard()
                    },
                    onDismiss = { viewModel.dismissCard() }
                )
            }

            // Bottom Navigation Bar
            CustomBottomNavigation(
                modifier = Modifier.align(Alignment.BottomCenter),
                onHistoryClick = onNavigateToHistory,
                onFavoritesClick = onNavigateToFavorites
            )
        } else {
            // Permission not granted
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Camera permission required",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    modifier: Modifier = Modifier,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.9f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavigationItem(
                icon = Icons.Default.History,
                label = "History",
                onClick = onHistoryClick
            )

            // Scan Button (Center Highlighted)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF0EA5E9).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { /* Already on Scan */ },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF0EA5E9).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Scan",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Scan",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            NavigationItem(
                icon = Icons.Default.Favorite,
                label = "Favorites",
                onClick = onFavoritesClick
            )
        }
    }
}

@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // MTG card ratio ~0.716
        val frameWidth = width * 0.75f
        val frameHeight = frameWidth * (88f / 63f)
        
        val left = (width - frameWidth) / 2
        val top = (height - frameHeight) / 2
        
        val cornerColor = Color(0xFF38BDF8)
        val strokeWidth = 3.dp.toPx()
        val cornerLength = 40.dp.toPx()
        val cornerRadius = 24.dp.toPx()

        // 1. Darken outside
        drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            size = size
        )

        // 2. Draw card frame corners (cyan glow)
        // Top Left
        drawArc(
            color = cornerColor,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left, top),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + cornerRadius, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top + cornerRadius),
            end = Offset(left, top + cornerLength),
            strokeWidth = strokeWidth
        )

        // Top Right
        drawArc(
            color = cornerColor,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left + frameWidth - cornerRadius * 2, top),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + frameWidth - cornerLength, top),
            end = Offset(left + frameWidth - cornerRadius, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + frameWidth, top + cornerRadius),
            end = Offset(left + frameWidth, top + cornerLength),
            strokeWidth = strokeWidth
        )

        // Bottom Left
        drawArc(
            color = cornerColor,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left, top + frameHeight - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top + frameHeight - cornerLength),
            end = Offset(left, top + frameHeight - cornerRadius),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + cornerRadius, top + frameHeight),
            end = Offset(left + cornerLength, top + frameHeight),
            strokeWidth = strokeWidth
        )

        // Bottom Right
        drawArc(
            color = cornerColor,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left + frameWidth - cornerRadius * 2, top + frameHeight - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + frameWidth, top + frameHeight - cornerLength),
            end = Offset(left + frameWidth, top + frameHeight - cornerRadius),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + frameWidth - cornerLength, top + frameHeight),
            end = Offset(left + frameWidth - cornerRadius, top + frameHeight),
            strokeWidth = strokeWidth
        )

        // 3. Center Crosshair
        val crossSize = 30.dp.toPx()
        val centerX = width / 2
        val centerY = height / 2
        
        drawLine(
            color = cornerColor.copy(alpha = 0.5f),
            start = Offset(centerX - crossSize / 2, centerY),
            end = Offset(centerX + crossSize / 2, centerY),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = cornerColor.copy(alpha = 0.5f),
            start = Offset(centerX, centerY - crossSize / 2),
            end = Offset(centerX, centerY + crossSize / 2),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun CameraPreview(
    onImageCaptured: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            onImageCaptured(imageProxy)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}