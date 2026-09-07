package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScanDatabase
import com.example.data.local.ScanRecord
import com.example.data.repository.DuplicateHandlingMode
import com.example.data.repository.ScanRepository
import com.example.data.repository.ScanResult
import com.example.util.CourierDetector
import com.example.util.SoundFeedbackHelper
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannerUiState(
    val latestScanResult: ScanResult? = null,
    val showDuplicateBanner: Boolean = false,
    val isScanningActive: Boolean = true,
    val isTorchOn: Boolean = false,
    val isFrontCamera: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val continuousScan: Boolean = true,
    val duplicateMode: DuplicateHandlingMode = DuplicateHandlingMode.RECORD_FLAGGED,
    val currentBatchTag: String = "",
    val searchQuery: String = "",
    val selectedCourierFilter: String = "Semua",
    val filterDuplicateOnly: Boolean = false,
    val infoMessage: String? = null,
    val isProcessingImage: Boolean = false
)

class ScannerViewModel(
    application: Application,
    private val repository: ScanRepository
) : AndroidViewModel(application) {

    private val soundHelper = SoundFeedbackHelper(application.applicationContext)

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val allRecords = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val uniqueCount = repository.uniqueCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val duplicateCount = repository.duplicateCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filtered list based on search and filters
    val filteredRecords: StateFlow<List<ScanRecord>> = combine(
        repository.allRecords,
        _uiState
    ) { records, state ->
        records.filter { record ->
            val matchesQuery = state.searchQuery.isBlank() ||
                record.code.contains(state.searchQuery, ignoreCase = true) ||
                record.courierName.contains(state.searchQuery, ignoreCase = true) ||
                record.notes.contains(state.searchQuery, ignoreCase = true)

            val matchesCourier = state.selectedCourierFilter == "Semua" ||
                record.courierName.contains(state.selectedCourierFilter, ignoreCase = true)

            val matchesDuplicate = !state.filterDuplicateOnly || record.isDuplicate

            matchesQuery && matchesCourier && matchesDuplicate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onBarcodeScanned(code: String, formatName: String, notes: String = "") {
        if (code.isBlank()) return

        val state = _uiState.value
        val detectedCourier = CourierDetector.detectCourier(code, formatName).name

        viewModelScope.launch {
            val result = repository.processScan(
                code = code,
                formatName = formatName,
                courierName = detectedCourier,
                notes = notes,
                batchTag = state.currentBatchTag,
                duplicateMode = state.duplicateMode
            )

            // Audio & Haptic feedback
            if (result.isDuplicate) {
                soundHelper.playDuplicateWarning(
                    soundEnabled = state.soundEnabled,
                    vibrateEnabled = state.vibrateEnabled
                )
            } else {
                soundHelper.playUniqueSuccess(
                    soundEnabled = state.soundEnabled,
                    vibrateEnabled = state.vibrateEnabled
                )
            }

            _uiState.value = _uiState.value.copy(
                latestScanResult = result,
                showDuplicateBanner = result.isDuplicate
            )
        }
    }

    fun scanFromImageUri(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isProcessingImage = true)
        try {
            val image = InputImage.fromFilePath(context, uri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    _uiState.value = _uiState.value.copy(isProcessingImage = false)
                    if (barcodes.isNotEmpty()) {
                        val first = barcodes.first()
                        val raw = first.rawValue ?: ""
                        if (raw.isNotBlank()) {
                            onBarcodeScanned(raw, "IMAGE_SCAN")
                        } else {
                            showInfoMessage("Kode batang kosong atau tidak terbaca")
                        }
                    } else {
                        showInfoMessage("Tidak ditemukan barcode / QR pada gambar")
                    }
                }
                .addOnFailureListener { e ->
                    _uiState.value = _uiState.value.copy(isProcessingImage = false)
                    showInfoMessage("Gagal memindai gambar: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isProcessingImage = false)
            showInfoMessage("Gagal membuka gambar: ${e.localizedMessage}")
        }
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchOn = !_uiState.value.isTorchOn)
    }

    fun toggleCamera() {
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
    }

    fun toggleSound(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
    }

    fun toggleVibrate(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrateEnabled = enabled)
    }

    fun toggleContinuous(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(continuousScan = enabled)
    }

    fun setDuplicateMode(mode: DuplicateHandlingMode) {
        _uiState.value = _uiState.value.copy(duplicateMode = mode)
    }

    fun setBatchTag(tag: String) {
        _uiState.value = _uiState.value.copy(currentBatchTag = tag)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setCourierFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedCourierFilter = filter)
    }

    fun setFilterDuplicateOnly(onlyDuplicate: Boolean) {
        _uiState.value = _uiState.value.copy(filterDuplicateOnly = onlyDuplicate)
    }

    fun dismissLatestResult() {
        _uiState.value = _uiState.value.copy(latestScanResult = null, showDuplicateBanner = false)
    }

    fun showInfoMessage(msg: String) {
        _uiState.value = _uiState.value.copy(infoMessage = msg)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    fun deleteRecord(record: ScanRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    fun updateRecordNotes(record: ScanRecord, newNotes: String) {
        viewModelScope.launch {
            repository.update(record.copy(notes = newNotes))
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            showInfoMessage("Riwayat scan berhasil dibersihkan")
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundHelper.release()
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = ScanDatabase.getDatabase(application)
                    val repo = ScanRepository(db.scanDao())
                    return ScannerViewModel(application, repo) as T
                }
            }
    }
}
