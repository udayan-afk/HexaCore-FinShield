package com.finshield.app.domain.calculator

import kotlin.math.abs
import kotlin.math.roundToLong

/** Consistent rupee formatting with Indian digit grouping (e.g. ₹6,02,165), rounded to the nearest rupee. */
object Money {
    fun inr(value: Double): String {
        if (!value.isFinite()) return "—"
        val rounded = value.roundToLong()
        val digits = abs(rounded).toString()
        val grouped = if (digits.length <= 3) digits else {
            val head = digits.dropLast(3)
            val tail = digits.takeLast(3)
            head.reversed().chunked(2).joinToString(",").reversed() + "," + tail
        }
        return (if (rounded < 0) "-₹" else "₹") + grouped
    }

    fun percent(fraction: Double): String = "${(fraction * 100).roundToLong()}%"
}
