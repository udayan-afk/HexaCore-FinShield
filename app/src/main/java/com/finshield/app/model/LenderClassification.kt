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

        fun fromBankName(bankName: String): LenderClassification {
            val lower = bankName.trim().lowercase()
            if (lower.contains("south indian bank")) return PRIVATE_BANK

            val publicBanks = listOf(
                "state bank of india",
                "bank of baroda",
                "bank of india",
                "bank of maharashtra",
                "canara bank",
                "central bank of india",
                "indian bank",
                "indian overseas bank",
                "punjab national bank",
                "punjab & sind bank",
                "punjab and sind bank",
                "uco bank",
                "union bank of india",
            )
            val isPublic = publicBanks.any {
                if (it == "indian bank") {
                    lower == "indian bank" || lower.startsWith("indian bank ")
                } else {
                    lower.contains(it)
                }
            }
            return if (isPublic) PUBLIC_BANK else PRIVATE_BANK
        }
    }
}
