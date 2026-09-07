package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScanRecord
import com.example.ui.theme.CourierAnteraja
import com.example.ui.theme.CourierJNE
import com.example.ui.theme.CourierJNT
import com.example.ui.theme.CourierNinja
import com.example.ui.theme.CourierOther
import com.example.ui.theme.CourierPOS
import com.example.ui.theme.CourierSPX
import com.example.ui.theme.CourierSiCepat
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningDuplicate
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryTab(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState,
    records: List<ScanRecord>,
    totalCount: Int,
    uniqueCount: Int,
    duplicateCount: Int,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearAllDialog by remember { mutableStateOf(false) }
    var recordToEditNote by remember { mutableStateOf<ScanRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<ScanRecord?>(null) }

    val couriersList = remember {
        listOf("Semua", "J&T", "SPX", "SiCepat", "JNE", "Anteraja", "Ninja", "POS", "QR Code")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Riwayat Scan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Basis data lokal & riwayat resi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (records.isNotEmpty()) {
                IconButton(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier.testTag("btn_clear_all_history")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Hapus Semua Riwayat",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Summary Metric Cards (Total, Unik, Duplikat)
        SummaryMetricRow(
            total = totalCount,
            unique = uniqueCount,
            duplicate = duplicateCount,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Cari kode resi, ekspedisi, atau catatan...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus Pencarian")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("input_history_search")
        )

        // Filter Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.filterDuplicateOnly,
                    onClick = { viewModel.setFilterDuplicateOnly(!uiState.filterDuplicateOnly) },
                    label = { Text("⚠️ Duplikat Saja") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WarningDuplicate.copy(alpha = 0.2f),
                        selectedLabelColor = WarningDuplicate
                    ),
                    modifier = Modifier.testTag("chip_filter_duplicate")
                )
            }

            items(couriersList) { courier ->
                FilterChip(
                    selected = uiState.selectedCourierFilter == courier,
                    onClick = { viewModel.setCourierFilter(courier) },
                    label = { Text(courier) }
                )
            }
        }

        // List of Scans or Empty State
        if (records.isEmpty()) {
            HistoryEmptyState(
                isSearching = uiState.searchQuery.isNotEmpty() || uiState.selectedCourierFilter != "Semua" || uiState.filterDuplicateOnly,
                onNavigateToScan = onNavigateToScan,
                onResetFilter = {
                    viewModel.setSearchQuery("")
                    viewModel.setCourierFilter("Semua")
                    viewModel.setFilterDuplicateOnly(false)
                }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = records,
                    key = { it.id }
                ) { record ->
                    HistoryItemCard(
                        record = record,
                        onCopy = {
                            copyToClipboard(context, record.code)
                            viewModel.showInfoMessage("Resi ${record.code} disalin")
                        },
                        onEditNote = { recordToEditNote = record },
                        onDelete = { recordToDelete = record }
                    )
                }
            }
        }
    }

    // Dialog: Edit Note
    recordToEditNote?.let { record ->
        EditNoteDialog(
            initialNote = record.notes,
            onDismiss = { recordToEditNote = null },
            onSave = { newNote ->
                viewModel.updateRecordNotes(record, newNote)
                recordToEditNote = null
                viewModel.showInfoMessage("Catatan diperbarui")
            }
        )
    }

    // Dialog: Delete Confirm
    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Hapus Riwayat?") },
            text = { Text("Hapus data scan untuk resi \"${record.code}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record)
                        recordToDelete = null
                        viewModel.showInfoMessage("Data dihapus")
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Clear All History
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Bersihkan Semua Riwayat?") },
            text = { Text("Seluruh riwayat pemindaian ($totalCount item) akan dihapus secara permanen. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Hapus Semua", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricRow(
    total: Int,
    unique: Int,
    duplicate: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Total
        MetricCard(
            label = "Total Scan",
            count = total.toString(),
            color = ScannerBlue,
            modifier = Modifier.weight(1f)
        )
        // Unique
        MetricCard(
            label = "Resi Unik",
            count = unique.toString(),
            color = SuccessGreen,
            modifier = Modifier.weight(1f)
        )
        // Duplicate
        MetricCard(
            label = "Duplikat",
            count = duplicate.toString(),
            color = WarningDuplicate,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Text(
                text = count,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    record: ScanRecord,
    onCopy: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("id", "ID")) }
    val courierBadgeColor = getCourierColor(record.courierName)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("item_history_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (record.isDuplicate) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Courier Tag Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = courierBadgeColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = record.courierName,
                        color = courierBadgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Duplicate indicator badge
                if (record.isDuplicate) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WarningDuplicate.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningDuplicate,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Duplikat (${record.scanCount}x)",
                                color = WarningDuplicate,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Unik",
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barcode text
            Text(
                text = record.code,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.4.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Timestamp and format
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormat.format(Date(record.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${record.formatName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Notes if present
            if (record.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Catatan: ${record.notes}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Action Buttons (Copy, Edit Note, Delete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Salin",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onEditNote,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Catatan",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(
    isSearching: Boolean,
    onNavigateToScan: () -> Unit,
    onResetFilter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Search else Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = if (isSearching) "Tidak ada hasil cocok" else "Belum Ada Riwayat Scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isSearching) {
                    "Coba periksa kembali kata kunci pencarian atau filter ekspedisi Anda."
                } else {
                    "Mulai pemindaian barcode resi atau QR untuk mencatat secara lokal dengan deteksi duplikat otomatis."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isSearching) {
                TextButton(onClick = onResetFilter) {
                    Text("Reset Filter")
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ScannerBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToScan() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buka Pemindai",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditNoteDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catatan Paket") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan / Keterangan") },
                placeholder = { Text("Contoh: Paket baju, gudang B, pesanan #123") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(note.trim()) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

private fun getCourierColor(courier: String): Color {
    return when {
        courier.contains("J&T", ignoreCase = true) -> CourierJNT
        courier.contains("Shopee", ignoreCase = true) || courier.contains("SPX", ignoreCase = true) -> CourierSPX
        courier.contains("SiCepat", ignoreCase = true) -> CourierSiCepat
        courier.contains("JNE", ignoreCase = true) -> CourierJNE
        courier.contains("Anteraja", ignoreCase = true) -> CourierAnteraja
        courier.contains("Ninja", ignoreCase = true) -> CourierNinja
        courier.contains("POS", ignoreCase = true) -> CourierPOS
        else -> CourierOther
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("Nomor Resi", text)
    clipboard?.setPrimaryClip(clip)
}
