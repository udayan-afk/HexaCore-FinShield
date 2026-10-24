package com.finshield.app.domain.calculator

import kotlin.math.abs
import kotlin.math.roundToLong

/** Consistent rupee formatting with Indian digit grouping (e.g. ₹6,02,165 or ₹5,00,000.00). */
object Money {
    fun formatIndianNumber(value: Long): String {
        val digits = abs(value).toString()
        return if (digits.length <= 3) digits else {
            val head = digits.dropLast(3)
            val tail = digits.takeLast(3)
            head.reversed().chunked(2).joinToString(",").reversed() + "," + tail
        }
    }

    fun inr(value: Double, decimals: Int = 0): String {
        if (!value.isFinite()) return "—"
        val isNegative = value < 0
        val absVal = abs(value)
        val prefix = if (isNegative) "-₹" else "₹"
        if (decimals <= 0) {
            val rounded = absVal.roundToLong()
            return prefix + formatIndianNumber(rounded)
        }
        val integerPart = absVal.toLong()
        var mult = 1L
        repeat(decimals) { mult *= 10L }
        val fractionPart = ((absVal - integerPart) * mult).roundToLong()
        val (finalInt, finalFrac) = if (fractionPart >= mult) {
            Pair(integerPart + 1L, 0L)
        } else {
            Pair(integerPart, fractionPart)
        }
        val fracStr = finalFrac.toString().padStart(decimals, '0')
        return prefix + formatIndianNumber(finalInt) + "." + fracStr
    }

    /** Convenience 2-decimal rupee formatter (e.g. ₹5,00,000.00) */
    fun inr2(value: Double): String = inr(value, decimals = 2)

    fun percent(fraction: Double, decimals: Int = 0): String {
        if (!fraction.isFinite()) return "—"
        val pct = fraction * 100.0
        return if (decimals <= 0) {
            "${pct.roundToLong()}%"
        } else {
            String.format(java.util.Locale.US, "%.${decimals}f%%", pct)
        }
    }
}
