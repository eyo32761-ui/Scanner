package com.example

import com.example.data.local.ScanRecord
import com.example.util.CourierDetector
import com.example.util.ExportHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testCourierDetection() {
    // J&T Express
    val jnt = CourierDetector.detectCourier("JP829104829103", "CODE_128")
    assertEquals("J&T Express", jnt.name)

    // Shopee Xpress
    val spx = CourierDetector.detectCourier("SPXID04829184029B", "CODE_128")
    assertEquals("Shopee Xpress (SPX)", spx.name)

    // SiCepat
    val sicepat = CourierDetector.detectCourier("004819284192", "CODE_128")
    assertEquals("SiCepat Express", sicepat.name)

    // JNE
    val jne = CourierDetector.detectCourier("SOCAG02948192048", "CODE_128")
    assertEquals("JNE Express", jne.name)

    // Anteraja
    val anteraja = CourierDetector.detectCourier("10002948102948", "CODE_128")
    assertEquals("Anteraja", anteraja.name)

    // Ninja Xpress
    val ninja = CourierDetector.detectCourier("NLIDAP0294819284", "CODE_128")
    assertEquals("Ninja Xpress", ninja.name)

    // QR Code
    val qr = CourierDetector.detectCourier("https://cekresi.com/123", "QR_CODE")
    assertTrue(qr.name.contains("QR") || qr.name.contains("URL"))
  }

  @Test
  fun testExportCsvGeneration() {
    val records = listOf(
      ScanRecord(
        id = 1,
        code = "JP001",
        formatName = "CODE_128",
        courierName = "J&T Express",
        timestamp = 1700000000000L,
        isDuplicate = false,
        scanCount = 1,
        notes = "Paket A"
      ),
      ScanRecord(
        id = 2,
        code = "JP001",
        formatName = "CODE_128",
        courierName = "J&T Express",
        timestamp = 1700000060000L,
        isDuplicate = true,
        scanCount = 2,
        notes = "Paket A Duplikat"
      )
    )

    val csv = ExportHelper.generateCsvContent(records, excelFormat = true)
    assertTrue(csv.startsWith("\uFEFF")) // Contains UTF-8 BOM for Excel
    assertTrue(csv.contains("JP001"))
    assertTrue(csv.contains("Duplikat"))
    assertTrue(csv.contains("J&T Express"))

    val summary = ExportHelper.generateSummaryText(records)
    assertTrue(summary.contains("RINGKASAN SCAN RESI"))
    assertTrue(summary.contains("Total Scan: 2 item"))
  }
}

