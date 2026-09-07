package com.example.ui.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.ScannerLaser
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun CameraScannerView(
    isTorchOn: Boolean,
    isFrontCamera: Boolean,
    continuousScan: Boolean,
    scanDelayMs: Long = 1500L,
    onBarcodeDetected: (code: String, formatName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScanTime by remember { mutableStateOf(0L) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Barcode scanner configured for all 1D & 2D formats common in Indonesian couriers
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_PDF417
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    LaunchedEffect(isTorchOn, camera) {
        try {
            camera?.cameraControl?.enableTorch(isTorchOn)
        } catch (e: Exception) {
            Log.e("CameraScanner", "Torch error", e)
        }
    }

    // Rebind camera whenever front/back camera changes or preview view becomes available
    LaunchedEffect(isFrontCamera, previewViewRef, lifecycleOwner) {
        val previewView = previewViewRef ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProviderRef = cameraProvider
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(
                        barcodeScanner = barcodeScanner,
                        imageProxy = imageProxy,
                        continuousScan = continuousScan,
                        scanDelayMs = scanDelayMs,
                        getLastCode = { lastScannedCode },
                        getLastTime = { lastScanTime },
                        onCodeDetected = { code, format ->
                            lastScannedCode = code
                            lastScanTime = System.currentTimeMillis()
                            onBarcodeDetected(code, format)
                        }
                    )
                }

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraScannerView", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraScannerView", "Error unbinding camera", e)
            }
            cameraExecutor.shutdown()
            try {
                barcodeScanner.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    // Use COMPATIBLE (TextureView) to prevent SurfaceView buffer queue abandonment
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                }
            }
        )

        // Viewfinder overlay with laser animation
        ScannerViewfinderOverlay()
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: androidx.camera.core.ImageProxy,
    continuousScan: Boolean,
    scanDelayMs: Long,
    getLastCode: () -> String?,
    getLastTime: () -> Long,
    onCodeDetected: (String, String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val firstBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (firstBarcode != null) {
                        val rawValue = firstBarcode.rawValue ?: ""
                        val formatName = getFormatName(firstBarcode.format)
                        val now = System.currentTimeMillis()
                        val lastCode = getLastCode()
                        val lastTime = getLastTime()

                        // Throttle: don't re-trigger identical code rapidly
                        val isThrottled = (rawValue == lastCode && (now - lastTime) < scanDelayMs)
                        if (!isThrottled) {
                            onCodeDetected(rawValue, formatName)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CameraScanner", "Barcode scanning failure", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun getFormatName(format: Int): String {
    return when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_PDF417 -> "PDF417"
        else -> "BARCODE"
    }
}

@Composable
fun ScannerViewfinderOverlay(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "laser")
    val laserProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.99f }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Square/Rectangular scan window centered
        val boxWidth = (canvasWidth * 0.82f).coerceAtMost(360.dp.toPx())
        val boxHeight = (boxWidth * 0.72f).coerceAtLeast(200.dp.toPx())

        val left = (canvasWidth - boxWidth) / 2f
        val top = (canvasHeight - boxHeight) / 2.3f
        val right = left + boxWidth
        val bottom = top + boxHeight

        // Dim background outside target box
        drawRect(
            color = Color(0x99000000),
            size = size
        )

        // Clear hole for target box
        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )
            )
        }
        drawPath(
            path = cutoutPath,
            color = Color.Transparent,
            blendMode = BlendMode.Clear
        )

        // Frame border
        drawRoundRect(
            color = Color(0x33FFFFFF),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Bold Corner brackets
        val cornerLength = 28.dp.toPx()
        val cornerRadiusPx = 20.dp.toPx()
        val strokeWidth = 4.5.dp.toPx()
        val bracketColor = ScannerLaser

        // Top-Left
        drawLine(bracketColor, Offset(left + cornerRadiusPx, top), Offset(left + cornerLength + cornerRadiusPx, top), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left, top + cornerRadiusPx), Offset(left, top + cornerLength + cornerRadiusPx), strokeWidth, StrokeCap.Round)

        // Top-Right
        drawLine(bracketColor, Offset(right - cornerRadiusPx, top), Offset(right - cornerLength - cornerRadiusPx, top), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(right, top + cornerRadiusPx), Offset(right, top + cornerLength + cornerRadiusPx), strokeWidth, StrokeCap.Round)

        // Bottom-Left
        drawLine(bracketColor, Offset(left + cornerRadiusPx, bottom), Offset(left + cornerLength + cornerRadiusPx, bottom), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left, bottom - cornerRadiusPx), Offset(left, bottom - cornerLength - cornerRadiusPx), strokeWidth, StrokeCap.Round)

        // Bottom-Right
        drawLine(bracketColor, Offset(right - cornerRadiusPx, bottom), Offset(right - cornerLength - cornerRadiusPx, bottom), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(right, bottom - cornerRadiusPx), Offset(right, bottom - cornerLength - cornerRadiusPx), strokeWidth, StrokeCap.Round)

        // Animated horizontal laser beam inside cutout
        val laserY = top + 16.dp.toPx() + (boxHeight - 32.dp.toPx()) * laserProgress
        val laserBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                ScannerLaser.copy(alpha = 0.4f),
                ScannerLaser,
                ScannerLaser.copy(alpha = 0.4f),
                Color.Transparent
            ),
            startX = left,
            endX = right
        )
        drawLine(
            brush = laserBrush,
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
