package com.example.mtgutilityapp.ui.camera

import android.Manifest
import android.os.Build.VERSION.SDK_INT
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.mtgutilityapp.ui.result.ResultOverlay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
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
                    text = "Scanning automatically...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            // Pondering GIF (Local File)
            if (uiState.isScanning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .size(120.dp)
                        .clip(CircleShape)
                ) {
                    val imageLoader = ImageLoader.Builder(context)
                        .components {
                            if (SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File("/home/max/AndroidStudioProjects/MTGUtilityApp/app/pondering-pondering-my-orb.gif"))
                            .build(),
                        contentDescription = "Processing",
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Result overlay with confidence and alternatives
            uiState.selectedCard?.let { card ->
                ResultOverlay(
                    card = card,
                    onSave = { updatedCard ->
                        viewModel.saveCard(updatedCard)
                    },
                    onDismiss = { viewModel.dismissCard() },
                    matchConfidence = uiState.matchConfidence,
                    suggestedAlternatives = uiState.suggestedAlternatives
                )
            }

            // Bottom Navigation Bar
            CustomBottomNavigation(
                modifier = Modifier.align(Alignment.BottomCenter),
                onHistoryClick = onNavigateToHistory,
                onFavoritesClick = onNavigateToFavorites,
                onScanClick = { /* Already here */ },
                activeScreen = "Scan"
            )
        } else {
            // Permission not granted
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Camera Permission")
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    modifier: Modifier = Modifier,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onScanClick: () -> Unit,
    activeScreen: String
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavigationItem(
                icon = Icons.Default.History,
                label = "History",
                onClick = onHistoryClick,
                isSelected = activeScreen == "History"
            )

            NavigationItem(
                icon = Icons.Default.CropFree,
                label = "Scan",
                onClick = onScanClick,
                isSelected = activeScreen == "Scan"
            )

            NavigationItem(
                icon = if (activeScreen == "Favorites") Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = "Favorites",
                onClick = onFavoritesClick,
                isSelected = activeScreen == "Favorites"
            )
        }
    }
}

@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    val tint = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.6f)
    val backgroundColor = if (isSelected) Color(0xFF0EA5E9).copy(alpha = 0.2f) else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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

        // 4. Center Crosshair
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
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    cameraProvider = cameraProviderFuture.get()
                }, ContextCompat.getMainExecutor(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { previewView ->
            val provider = cameraProvider ?: return@AndroidView
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Request higher resolution for better OCR
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(1920, 1080), // Minimum 1080p, preferably higher
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        onImageCaptured(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}