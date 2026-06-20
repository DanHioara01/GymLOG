package com.example.gymlog2

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.gymlog2.ui.theme.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    onBarcodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val accent = if (isDark) accentColor() else LightPrimaryRed
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // scannedOnce prevents multiple triggers while the callback is in flight
    val scannedOnce = remember { AtomicBoolean(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            errorMessage = strings.cameraPermissionRequired
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(strings.scanBarcode, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                hasCameraPermission && errorMessage == null -> {
                    CameraPreviewWithScan(
                        accent = accent,
                        onBarcodeScanned = { value ->
                            if (scannedOnce.compareAndSet(false, true)) {
                                onBarcodeScanned(value)
                            }
                        }
                    )

                    // Hint text at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .border(2.dp, accent, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.scanBarcodeHint,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                scannedOnce.set(false)
                                if (!hasCameraPermission) {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.retry, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithScan(
    accent: Color,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Keep a stable reference to the callback so AndroidView factory can call it
    val onBarcodeScannedRef = rememberUpdatedState(onBarcodeScanned)

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    val multiFormatReader = remember {
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.ITF,
                        BarcodeFormat.QR_CODE
                    ),
                    DecodeHintType.TRY_HARDER to true
                )
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { multiFormatReader.reset() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            val result = scanBarcode(imageProxy, multiFormatReader)
                            if (result != null) {
                                onBarcodeScannedRef.value(result)
                            }
                        } catch (_: Exception) {
                        } finally {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
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

        // Viewfinder overlay
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1.8f)
                .align(Alignment.Center)
                .border(2.dp, accent, RoundedCornerShape(12.dp))
        )

        // Corner decorations
        CornerDecorations(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1.8f)
                .align(Alignment.Center),
            color = accent
        )
    }
}

@Composable
private fun CornerDecorations(modifier: Modifier, color: Color) {
    Box(modifier = modifier) {
        val cornerSize = 20.dp
        val strokeWidth = 3.dp
        // Top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerSize)
                .border(
                    width = strokeWidth,
                    color = color,
                    shape = RoundedCornerShape(topStart = 8.dp)
                )
        )
        // Top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerSize)
                .border(
                    width = strokeWidth,
                    color = color,
                    shape = RoundedCornerShape(topEnd = 8.dp)
                )
        )
        // Bottom-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerSize)
                .border(
                    width = strokeWidth,
                    color = color,
                    shape = RoundedCornerShape(bottomStart = 8.dp)
                )
        )
        // Bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerSize)
                .border(
                    width = strokeWidth,
                    color = color,
                    shape = RoundedCornerShape(bottomEnd = 8.dp)
                )
        )
    }
}

private fun scanBarcode(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    val image = imageProxy.image ?: return null
    val yPlane = image.planes[0]
    val yBuffer = yPlane.buffer
    val rowStride = yPlane.rowStride
    val pixelStride = yPlane.pixelStride
    val width = image.width
    val height = image.height

    val luminanceData = ByteArray(width * height)
    if (rowStride == width && pixelStride == 1) {
        yBuffer.get(luminanceData)
    } else {
        for (y in 0 until height) {
            val rowStart = y * rowStride
            for (x in 0 until width) {
                val bufferIndex = rowStart + x * pixelStride
                if (bufferIndex < yBuffer.limit()) {
                    luminanceData[y * width + x] = yBuffer[bufferIndex]
                }
            }
        }
    }

    val source = PlanarYUVLuminanceSource(
        luminanceData,
        width,
        height,
        0, 0,
        width,
        height,
        false
    )
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
    return try {
        reader.decodeWithState(binaryBitmap).text
    } catch (_: NotFoundException) {
        null
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
    }
}
