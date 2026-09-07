package com.example.util

data class CourierInfo(
    val name: String,
    val shortCode: String,
    val description: String
)

object CourierDetector {

    fun detectCourier(code: String, formatName: String): CourierInfo {
        val upper = code.trim().uppercase()

        if (formatName.equals("QR_CODE", ignoreCase = true) && (upper.startsWith("HTTP://") || upper.startsWith("HTTPS://"))) {
            return CourierInfo("Tautan Web / QR URL", "URL", "QR Code Tautan")
        }

        return when {
            // Shopee Xpress (SPX)
            upper.startsWith("SPXID") || upper.startsWith("SPEID") || upper.startsWith("ID0") || upper.startsWith("SPX") -> {
                CourierInfo("Shopee Xpress (SPX)", "SPX", "Resi Shopee Xpress")
            }
            // J&T Express
            upper.startsWith("JP") || upper.startsWith("JX") || upper.startsWith("JS") ||
            upper.startsWith("JT") || upper.startsWith("JTS") || (upper.length == 12 && upper.startsWith("888")) -> {
                CourierInfo("J&T Express", "J&T", "Resi J&T Express")
            }
            // SiCepat
            (upper.length == 12 && (upper.startsWith("00") || upper.startsWith("01") || upper.startsWith("02"))) ||
            upper.startsWith("TKP") || upper.startsWith("SICEPAT") -> {
                CourierInfo("SiCepat Express", "SiCepat", "Resi SiCepat")
            }
            // Anteraja
            (upper.length == 14 && (upper.startsWith("100") || upper.startsWith("101") || upper.startsWith("102"))) ||
            upper.startsWith("1000") -> {
                CourierInfo("Anteraja", "Anteraja", "Resi Anteraja")
            }
            // Ninja Xpress
            upper.startsWith("NLID") || upper.startsWith("SHP") || upper.startsWith("NINJA") -> {
                CourierInfo("Ninja Xpress", "Ninja", "Resi Ninja Xpress")
            }
            // ID Express
            upper.startsWith("IDE") || upper.startsWith("IDS") -> {
                CourierInfo("ID Express", "IDX", "Resi ID Express")
            }
            // Lion Parcel
            upper.startsWith("LP") || (upper.length == 12 && upper.startsWith("77")) -> {
                CourierInfo("Lion Parcel", "Lion", "Resi Lion Parcel")
            }
            // Wahana
            upper.startsWith("WHA") || upper.startsWith("WNA") -> {
                CourierInfo("Wahana Express", "Wahana", "Resi Wahana")
            }
            // POS Indonesia
            upper.startsWith("POS") || upper.startsWith("P1") || upper.startsWith("P2") ||
            (upper.length in 11..13 && upper.all { it.isDigit() } && upper.startsWith("1")) -> {
                CourierInfo("POS Indonesia", "POS", "Resi POS Indonesia")
            }
            // JNE (commonly alphanumeric starting with 3-5 letters like CGK, SOCAG, TJIAA or 15-16 digits)
            upper.startsWith("JNE") || (upper.length in 15..16 && upper.all { it.isDigit() }) ||
            (upper.length in 15..16 && upper.take(4).all { it.isLetter() }) -> {
                CourierInfo("JNE Express", "JNE", "Resi JNE")
            }
            formatName.equals("QR_CODE", ignoreCase = true) -> {
                CourierInfo("QR Code", "QR", "Format QR Code")
            }
            else -> {
                CourierInfo("Resi / Barcode", "Umum", "Kode Standar")
            }
        }
    }

    // Curated sample Indonesian courier resi codes for quick testing/simulation
    val sampleResiList = listOf(
        Pair("JP829104829103", "J&T Express"),
        Pair("SPXID04829184029B", "Shopee Xpress (SPX)"),
        Pair("004819284192", "SiCepat Express"),
        Pair("SOCAG02948192048", "JNE Express"),
        Pair("10002948102948", "Anteraja"),
        Pair("NLIDAP0294819284", "Ninja Xpress"),
        Pair("IDE029481920481", "ID Express"),
        Pair("P2409071294812", "POS Indonesia"),
        Pair("https://track.package.id/resi/INV-9281", "QR Code")
    )
}
