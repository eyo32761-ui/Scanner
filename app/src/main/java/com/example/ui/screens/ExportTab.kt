package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScanRecord
import com.example.data.repository.DuplicateHandlingMode
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningDuplicate
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel
import com.example.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportTab(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState,
    records: List<ScanRecord>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var excelCompatibleFormat by remember { mutableStateOf(true) }
    var exportScope by remember { mutableStateOf("ALL") } // ALL, UNIQUE, DUPLICATE

    val filteredForExport = remember(records, exportScope) {
        when (exportScope) {
            "UNIQUE" -> records.filter { !it.isDuplicate }
            "DUPLICATE" -> records.filter { it.isDuplicate }
            else -> records
        }
    }

    // Save As (CreateDocument) Launcher
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            val success = ExportHelper.writeToUri(context, it, filteredForExport, excelCompatibleFormat)
            if (success) {
                viewModel.showInfoMessage("File berhasil disimpan ke perangkat!")
            } else {
                viewModel.showInfoMessage("Gagal menyimpan file.")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Ekspor & Pengaturan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ekspor data ke CSV/Excel & konfigurasi deteksi duplikat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Export Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SuccessGreen.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ekspor CSV & Excel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${filteredForExport.size} item siap diekspor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scope Selector
                Text(
                    text = "Data yang Diekspor:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScopeButton(
                        label = "Semua (${records.size})",
                        selected = exportScope == "ALL",
                        onClick = { exportScope = "ALL" },
                        modifier = Modifier.weight(1f)
                    )
                    ScopeButton(
                        label = "Unik (${records.count { !it.isDuplicate }})",
                        selected = exportScope == "UNIQUE",
                        onClick = { exportScope = "UNIQUE" },
                        modifier = Modifier.weight(1f)
                    )
                    ScopeButton(
                        label = "Duplikat (${records.count { it.isDuplicate }})",
                        selected = exportScope == "DUPLICATE",
                        onClick = { exportScope = "DUPLICATE" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Format switch (Excel Compatible BOM)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Optimalkan untuk Excel (UTF-8 BOM)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Mencegah nomor resi panjang terpotong atau menjadi notasi ilmiah di Excel",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = excelCompatibleFormat,
                        onCheckedChange = { excelCompatibleFormat = it }
                    )
                }

                // Live Preview Box
                if (filteredForExport.isNotEmpty()) {
                    Text(
                        text = "Pratinjau Format Kolom:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "No | No Resi / Kode | Ekspedisi | Format | Status | Waktu",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            filteredForExport.take(3).forEachIndexed { idx, item ->
                                Text(
                                    text = "${idx + 1} | ${item.code} | ${item.courierName} | ${item.formatName} | ${if (item.isDuplicate) "Duplikat" else "Unik"}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (filteredForExport.size > 3) {
                                Text(
                                    text = "... dan ${filteredForExport.size - 3} item lainnya",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        if (filteredForExport.isEmpty()) {
                            viewModel.showInfoMessage("Belum ada data scan untuk diekspor")
                            return@Button
                        }
                        val file = ExportHelper.exportToCacheFile(context, filteredForExport, excelCompatibleFormat)
                        val shareIntent = ExportHelper.shareExportFile(context, file)
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan CSV / Excel"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_share_csv_export"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerBlue),
                    enabled = filteredForExport.isNotEmpty()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan File CSV / Excel", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (filteredForExport.isEmpty()) {
                                viewModel.showInfoMessage("Belum ada data scan untuk disimpan")
                                return@OutlinedButton
                            }
                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            saveFileLauncher.launch("ScanResi_$dateStr.csv")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_save_csv_file"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = filteredForExport.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan ke File", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (filteredForExport.isEmpty()) {
                                viewModel.showInfoMessage("Belum ada data scan")
                                return@OutlinedButton
                            }
                            val summary = ExportHelper.generateSummaryText(filteredForExport)
                            copyToClipboard(context, summary)
                            viewModel.showInfoMessage("Ringkasan teks disalin ke papan klip")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_copy_summary_text"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = filteredForExport.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Ringkasan", fontSize = 12.sp)
                    }
                }
            }
        }

        // Duplicate Detection Policy Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = WarningDuplicate.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Rule,
                                contentDescription = null,
                                tint = WarningDuplicate,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Aturan Deteksi Duplikat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Perlakuan saat resi yang sama discan berulang kali",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Mode 1: Record Flagged
                DuplicateModeItem(
                    title = "Catat dengan Tanda Duplikat (Disarankan)",
                    description = "Menyimpan catatan baru dengan label 'Duplikat' dan penanda waktu kejadian.",
                    selected = uiState.duplicateMode == DuplicateHandlingMode.RECORD_FLAGGED,
                    onClick = { viewModel.setDuplicateMode(DuplicateHandlingMode.RECORD_FLAGGED) }
                )

                // Mode 2: Increment Count
                DuplicateModeItem(
                    title = "Perbarui Hitungan Scan",
                    description = "Menambah jumlah hitungan pada resi yang sudah ada tanpa menambah baris baru.",
                    selected = uiState.duplicateMode == DuplicateHandlingMode.INCREMENT_COUNT,
                    onClick = { viewModel.setDuplicateMode(DuplicateHandlingMode.INCREMENT_COUNT) }
                )

                // Mode 3: Reject
                DuplicateModeItem(
                    title = "Peringatkan & Jangan Simpan",
                    description = "Membunyikan peringatan keras dan menolak menyimpan duplikat ke dalam riwayat.",
                    selected = uiState.duplicateMode == DuplicateHandlingMode.REJECT_DUPLICATE,
                    onClick = { viewModel.setDuplicateMode(DuplicateHandlingMode.REJECT_DUPLICATE) }
                )
            }
        }

        // Scanner Feedback & Batch Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Pengaturan Pemindai",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                // Sound Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = ScannerBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Suara Beep & Alarm", fontWeight = FontWeight.Medium)
                            Text("Nada konfirmasi untuk scan baru & alarm untuk duplikat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = uiState.soundEnabled,
                        onCheckedChange = { viewModel.toggleSound(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = ScannerBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Getaran (Haptic Feedback)", fontWeight = FontWeight.Medium)
                            Text("Getar 1x saat sukses, 2x saat duplikat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = uiState.vibrateEnabled,
                        onCheckedChange = { viewModel.toggleVibrate(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Batch Tag
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Label Sesi / Batch Aktif (Opsional):",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = uiState.currentBatchTag,
                        onValueChange = { viewModel.setBatchTag(it) },
                        placeholder = { Text("Contoh: Batch Pagi, Gudang 1, Drop 07-09") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ScopeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (selected) ScannerBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DuplicateModeItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (selected) ScannerBlue.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("Ringkasan Scan", text)
    clipboard?.setPrimaryClip(clip)
}
