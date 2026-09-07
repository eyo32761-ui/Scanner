package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.scanner.CameraScannerView
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.ScannerLaser
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningDuplicate
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel
import com.example.util.CourierDetector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScannerTab(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            viewModel.showInfoMessage("Izin kamera diperlukan untuk memindai kode batang secara langsung.")
        }
    }

    // Photo picker for scanning barcode from image/photo of receipt
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.scanFromImageUri(context, it)
        }
    }

    var showManualInputDialog by remember { mutableStateOf(false) }
    var showTestPresetsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera View or Permission Fallback View
        if (hasCameraPermission) {
            CameraScannerView(
                isTorchOn = uiState.isTorchOn,
                isFrontCamera = uiState.isFrontCamera,
                continuousScan = uiState.continuousScan,
                onBarcodeDetected = { code, format ->
                    viewModel.onBarcodeScanned(code, format)
                }
            )
        } else {
            CameraPermissionRequiredScreen(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onOpenManualInput = { showManualInputDialog = true },
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onOpenTestPresets = { showTestPresetsDialog = true }
            )
        }

        // Top Floating Toolbar (Flash, Flip, Audio, Mode)
        TopScannerToolbar(
            uiState = uiState,
            onToggleTorch = { viewModel.toggleTorch() },
            onToggleCamera = { viewModel.toggleCamera() },
            onToggleSound = { viewModel.toggleSound(!uiState.soundEnabled) },
            onToggleContinuous = { viewModel.toggleContinuous(!uiState.continuousScan) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Bottom Action Controls (Manual Input, Image Pick, Test Presets)
        BottomScannerControls(
            onManualInput = { showManualInputDialog = true },
            onPickImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onTestPresets = { showTestPresetsDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )

        // Duplicate or Success Result Notification Banner
        AnimatedVisibility(
            visible = uiState.latestScanResult != null,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            uiState.latestScanResult?.let { result ->
                ScanResultAlertCard(
                    result = result,
                    onDismiss = { viewModel.dismissLatestResult() },
                    onCopyCode = {
                        copyToClipboard(context, result.record.code)
                        viewModel.showInfoMessage("Nomor resi disalin ke papan klip")
                    }
                )
            }
        }

        // Processing indicator when scanning from image
        if (uiState.isProcessingImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = ScannerBlue)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Memindai barcode dari gambar...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Manual input dialog
    if (showManualInputDialog) {
        ManualInputDialog(
            onDismiss = { showManualInputDialog = false },
            onSubmit = { code, notes ->
                showManualInputDialog = false
                viewModel.onBarcodeScanned(code, "MANUAL_INPUT", notes)
            }
        )
    }

    // Test Presets Dialog (quick courier resi barcodes for testing duplicates in emulator)
    if (showTestPresetsDialog) {
        TestPresetsDialog(
            onDismiss = { showTestPresetsDialog = false },
            onSelectPreset = { code, courier ->
                showTestPresetsDialog = false
                viewModel.onBarcodeScanned(code, "CODE_128", "Uji Coba $courier")
            }
        )
    }
}

@Composable
private fun TopScannerToolbar(
    uiState: ScannerUiState,
    onToggleTorch: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleContinuous: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color(0xCC0F172A),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Flashlight Toggle
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier.testTag("btn_toggle_torch")
            ) {
                Icon(
                    imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flashlight",
                    tint = if (uiState.isTorchOn) Color(0xFFFACC15) else Color.White
                )
            }

            // Flip Camera
            IconButton(
                onClick = onToggleCamera,
                modifier = Modifier.testTag("btn_flip_camera")
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Ganti Kamera",
                    tint = if (uiState.isFrontCamera) ScannerLaser else Color.White
                )
            }

            // Sound Toggle
            IconButton(
                onClick = onToggleSound,
                modifier = Modifier.testTag("btn_toggle_sound")
            ) {
                Icon(
                    imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                    contentDescription = "Suara Beep",
                    tint = if (uiState.soundEnabled) Color(0xFF38BDF8) else Color.Gray
                )
            }

            // Continuous Mode Toggle
            Surface(
                shape = CircleShape,
                color = if (uiState.continuousScan) ScannerBlue.copy(alpha = 0.35f) else Color.Transparent,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onToggleContinuous() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Mode Kontinu",
                        tint = if (uiState.continuousScan) ScannerLaser else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.continuousScan) "Kontinu" else "Tunggal",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomScannerControls(
    onManualInput: () -> Unit,
    onPickImage: () -> Unit,
    onTestPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Manual Input
            FilledTonalButton(
                onClick = onManualInput,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_manual_input"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xDD1E293B),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = ScannerLaser
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Input Manual", fontSize = 13.sp)
            }

            // Pick Image
            FilledTonalButton(
                onClick = onPickImage,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_pick_image"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xDD1E293B),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = ScannerLaser
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Foto Resi", fontSize = 13.sp)
            }

            // Test Simulation Button (Quick Presets)
            FilledTonalButton(
                onClick = onTestPresets,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_test_presets"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xEE0369A1),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Uji Resi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ScanResultAlertCard(
    result: com.example.data.repository.ScanResult,
    onDismiss: () -> Unit,
    onCopyCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDuplicate = result.isDuplicate
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isDuplicate) "card_duplicate_detected" else "card_unique_scanned"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDuplicate) Color(0xFF7F1D1D) else Color(0xFF064E3B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDuplicate) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDuplicate) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDuplicate) "DUPLIKAT TERDETEKSI!" else "RESI BARU (UNIK)",
                        color = if (isDuplicate) Color(0xFFFECACA) else Color(0xFFA7F3D0),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isDuplicate) {
                            "Pernah discan ${result.totalTimesScanned}x • Terakhir: ${result.previousScanTime?.let { timeFormat.format(Date(it)) } ?: "-"}"
                        } else {
                            "Scan pertama kali • Siap diekspor"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Code Display Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.record.code,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = result.record.courierName,
                                color = if (isDuplicate) Color(0xFFFDE047) else Color(0xFF67E8F9),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${result.record.formatName}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onCopyCode,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Kode",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequiredScreen(
    onRequestPermission: () -> Unit,
    onOpenManualInput: () -> Unit,
    onPickImage: () -> Unit,
    onOpenTestPresets: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ScannerBlue.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ScannerLaser,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "Akses Kamera Diperlukan",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Aplikasi membutuhkan izin kamera untuk memindai barcode resi & QR secara langsung. Anda juga bisa mengunggah foto resi atau input kode secara manual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    lineHeight = 20.sp
                )

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_request_permission"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerBlue)
                ) {
                    Text("Berikan Izin Kamera")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenManualInput,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Input Manual", fontSize = 12.sp)
                    }
                    FilledTonalButton(
                        onClick = onOpenTestPresets,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Uji Resi Cepat", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualInputDialog(
    onDismiss: () -> Unit,
    onSubmit: (code: String, notes: String) -> Unit
) {
    var codeText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Input Nomor Resi / Barcode")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Masukkan nomor resi ekspedisi (misal J&T, SiCepat, Shopee Xpress, JNE) atau teks QR Code:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    label = { Text("Nomor Resi / Kode") },
                    placeholder = { Text("Contoh: JP1234567890") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_manual_code")
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Catatan Paket (Opsional)") },
                    placeholder = { Text("Contoh: Baju, Retur, Gudang") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (codeText.isNotBlank()) {
                        onSubmit(codeText.trim(), notesText.trim())
                    }
                },
                enabled = codeText.isNotBlank(),
                modifier = Modifier.testTag("btn_submit_manual_code")
            ) {
                Text("Proses Scan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun TestPresetsDialog(
    onDismiss: () -> Unit,
    onSelectPreset: (code: String, courier: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pilih Resi Simulasi (Uji Coba)")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pilih salah satu nomor resi di bawah ini untuk menguji deteksi ekspedisi otomatis dan uji deteksi duplikat:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                CourierDetector.sampleResiList.forEach { (code, courier) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectPreset(code, courier)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = courier,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = code,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "PILIH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("Resi Barcode", text)
    clipboard?.setPrimaryClip(clip)
}
