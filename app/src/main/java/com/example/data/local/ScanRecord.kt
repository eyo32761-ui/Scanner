package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val formatName: String = "CODE_128",
    val courierName: String = "Lainnya",
    val timestamp: Long = System.currentTimeMillis(),
    val isDuplicate: Boolean = false,
    val scanCount: Int = 1,
    val notes: String = "",
    val batchTag: String = ""
)
