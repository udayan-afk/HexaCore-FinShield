package com.finshield.app.model

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory repository for reports generated during the session.
 * Real scans and uploaded PDF analysis results are stored here.
 * If a report is not found in this store, fallback is provided via [SampleReports.forId].
 */
object AnalysisReportStore {
    private val customReports = ConcurrentHashMap<String, AnalysisReport>()

    fun saveReport(id: String, report: AnalysisReport) {
        customReports[id] = report
    }

    fun getReport(id: String): AnalysisReport? = customReports[id]

    fun clear() {
        customReports.clear()
    }
}
