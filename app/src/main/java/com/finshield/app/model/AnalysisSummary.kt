package com.finshield.app.model

/** One saved analysis in the history list. */
data class AnalysisSummary(
    val id: String,
    val documentName: String,
    val reviewedOn: String,
    val pages: Int,
    val itemsToReview: Int,
    /** True for built-in demo content that did not come from a real document or from Firestore. */
    val isSample: Boolean,
)
