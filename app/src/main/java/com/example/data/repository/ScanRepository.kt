package com.example.data.repository

import com.example.data.local.ScanDao
import com.example.data.local.ScanRecord
import kotlinx.coroutines.flow.Flow

enum class DuplicateHandlingMode {
    RECORD_FLAGGED,    // Save duplicate as new record with isDuplicate = true
    INCREMENT_COUNT,   // Increment count on existing record
    REJECT_DUPLICATE   // Reject and do not save
}

data class ScanResult(
    val record: ScanRecord,
    val isDuplicate: Boolean,
    val previousScanTime: Long? = null,
    val totalTimesScanned: Int = 1,
    val wasSaved: Boolean = true
)

class ScanRepository(private val scanDao: ScanDao) {
    val allRecords: Flow<List<ScanRecord>> = scanDao.getAllRecords()
    val totalCount: Flow<Int> = scanDao.getTotalCount()
    val uniqueCount: Flow<Int> = scanDao.getUniqueCodeCount()
    val duplicateCount: Flow<Int> = scanDao.getDuplicateCount()

    fun search(query: String): Flow<List<ScanRecord>> = scanDao.searchRecords(query)

    suspend fun getAllRecordsList(): List<ScanRecord> = scanDao.getAllRecordsList()

    suspend fun processScan(
        code: String,
        formatName: String,
        courierName: String,
        notes: String = "",
        batchTag: String = "",
        duplicateMode: DuplicateHandlingMode = DuplicateHandlingMode.RECORD_FLAGGED
    ): ScanResult {
        val trimmedCode = code.trim()
        val existingRecords = scanDao.getRecordsByCode(trimmedCode)
        val isDuplicate = existingRecords.isNotEmpty()

        if (isDuplicate) {
            val mostRecent = existingRecords.first()
            val totalOccurrences = existingRecords.sumOf { it.scanCount }

            return when (duplicateMode) {
                DuplicateHandlingMode.REJECT_DUPLICATE -> {
                    ScanResult(
                        record = mostRecent,
                        isDuplicate = true,
                        previousScanTime = mostRecent.timestamp,
                        totalTimesScanned = totalOccurrences,
                        wasSaved = false
                    )
                }
                DuplicateHandlingMode.INCREMENT_COUNT -> {
                    val updated = mostRecent.copy(
                        scanCount = mostRecent.scanCount + 1,
                        timestamp = System.currentTimeMillis()
                    )
                    scanDao.update(updated)
                    ScanResult(
                        record = updated,
                        isDuplicate = true,
                        previousScanTime = mostRecent.timestamp,
                        totalTimesScanned = totalOccurrences + 1,
                        wasSaved = true
                    )
                }
                DuplicateHandlingMode.RECORD_FLAGGED -> {
                    val newRecord = ScanRecord(
                        code = trimmedCode,
                        formatName = formatName,
                        courierName = courierName,
                        timestamp = System.currentTimeMillis(),
                        isDuplicate = true,
                        scanCount = totalOccurrences + 1,
                        notes = notes,
                        batchTag = batchTag
                    )
                    val newId = scanDao.insert(newRecord)
                    ScanResult(
                        record = newRecord.copy(id = newId),
                        isDuplicate = true,
                        previousScanTime = mostRecent.timestamp,
                        totalTimesScanned = totalOccurrences + 1,
                        wasSaved = true
                    )
                }
            }
        } else {
            // First time scanned -> Unique!
            val newRecord = ScanRecord(
                code = trimmedCode,
                formatName = formatName,
                courierName = courierName,
                timestamp = System.currentTimeMillis(),
                isDuplicate = false,
                scanCount = 1,
                notes = notes,
                batchTag = batchTag
            )
            val newId = scanDao.insert(newRecord)
            return ScanResult(
                record = newRecord.copy(id = newId),
                isDuplicate = false,
                previousScanTime = null,
                totalTimesScanned = 1,
                wasSaved = true
            )
        }
    }

    suspend fun insert(record: ScanRecord): Long = scanDao.insert(record)

    suspend fun update(record: ScanRecord) = scanDao.update(record)

    suspend fun delete(record: ScanRecord) = scanDao.delete(record)

    suspend fun deleteById(id: Long) = scanDao.deleteById(id)

    suspend fun clearAll() = scanDao.deleteAll()
}
