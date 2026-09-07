package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.ScanRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID"))
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generateCsvContent(records: List<ScanRecord>, excelFormat: Boolean = true): String {
        val sb = StringBuilder()

        // Write UTF-8 Byte Order Mark (BOM) so Excel detects UTF-8 correctly
        sb.append("\uFEFF")

        // CSV Header
        sb.append("No,Nomor Resi / Kode,Ekspedisi,Format,Status,Jumlah Scan,Waktu Scan,Batch,Catatan\n")

        records.forEachIndexed { index, record ->
            val num = index + 1
            // In Excel, long numbers with leading zeros like "004812" get stripped unless escaped as ="004812" or quoted
            val safeCode = if (excelFormat) {
                "=\"${record.code.replace("\"", "\"\"")}\""
            } else {
                "\"${record.code.replace("\"", "\"\"")}\""
            }
            val courier = "\"${record.courierName.replace("\"", "\"\"")}\""
            val format = "\"${record.formatName.replace("\"", "\"\"")}\""
            val status = if (record.isDuplicate) "\"Duplikat\"" else "\"Unik\""
            val scanCount = record.scanCount
            val timeStr = "\"${dateFormat.format(Date(record.timestamp))}\""
            val batch = "\"${record.batchTag.replace("\"", "\"\"")}\""
            val notes = "\"${record.notes.replace("\"", "\"\"")}\""

            sb.append("$num,$safeCode,$courier,$format,$status,$scanCount,$timeStr,$batch,$notes\n")
        }

        return sb.toString()
    }

    fun exportToCacheFile(context: Context, records: List<ScanRecord>, excelFormat: Boolean = true): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val timestamp = fileNameFormat.format(Date())
        val fileName = "ScanResi_${timestamp}.csv"
        val file = File(exportDir, fileName)

        val csvData = generateCsvContent(records, excelFormat)
        FileOutputStream(file).use { out ->
            out.write(csvData.toByteArray(Charsets.UTF_8))
        }

        return file
    }

    fun shareExportFile(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ekspor Data Resi ScanResi")
            putExtra(
                Intent.EXTRA_TEXT,
                "Berikut file ekspor CSV data pemindaian barcode resi (${file.name})."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun writeToUri(context: Context, uri: Uri, records: List<ScanRecord>, excelFormat: Boolean = true): Boolean {
        return try {
            val csvData = generateCsvContent(records, excelFormat)
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
            outputStream?.use { out ->
                out.write(csvData.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateSummaryText(records: List<ScanRecord>): String {
        val total = records.size
        val duplicates = records.count { it.isDuplicate }
        val unique = total - duplicates
        val courierGroups = records.groupBy { it.courierName }

        val sb = StringBuilder()
        sb.append("📦 *RINGKASAN SCAN RESI*\n")
        sb.append("📅 Waktu: ${dateFormat.format(Date())}\n")
        sb.append("📊 Total Scan: $total item\n")
        sb.append("✅ Unik: $unique | ⚠️ Duplikat: $duplicates\n\n")
        sb.append("🚚 *Rincian Ekspedisi:*\n")
        courierGroups.forEach { (courier, list) ->
            sb.append("• $courier: ${list.size} paket\n")
        }
        return sb.toString()
    }
}
