package com.finshield.app.model

enum class LenderClassification(
    val label: String,
    val shortLabel: String,
) {
    PUBLIC_BANK("Public Sector Bank", "Public Bank"),
    PRIVATE_BANK("Private Sector Bank", "Private Bank"),
    NBFC("Non-Banking Financial Company (NBFC)", "NBFC"),
    DIGITAL_LENDER("Digital Lender / Fintech Partner", "Digital Lender");

    companion object {
        fun fromCsv(raw: String): LenderClassification {
            val lower = raw.trim().lowercase()
            return when {
                lower.contains("public") -> PUBLIC_BANK
                lower.contains("private") -> PRIVATE_BANK
                lower.contains("digital") -> DIGITAL_LENDER
                lower.contains("nbfc") -> NBFC
                else -> DIGITAL_LENDER
            }
        }
    }
}
