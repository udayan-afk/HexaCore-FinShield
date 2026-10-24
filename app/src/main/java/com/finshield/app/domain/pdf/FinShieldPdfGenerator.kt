package com.finshield.app.domain.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.ObligationStatus
import com.finshield.app.model.Severity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Production-grade multi-page PDF generator for FinShield Loan Analysis Reports.
 * Implemented using Android's native [android.graphics.pdf.PdfDocument].
 *
 * Adheres strictly to FinShield's visual brand identity:
 * - Deep Navy (#0F2137), Emerald Green (#059669), and Cyan (#06B6D4) accents.
 * - Standard A4 dimensions (595 x 842 points).
 * - Automatic page breaks without text overlapping or content clipping.
 * - Full Unicode currency (₹) support.
 */
class FinShieldPdfGenerator(
    private val report: AnalysisReport,
    private val options: PdfExportOptions = PdfExportOptions(),
) {

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN_LEFT = 40f
        const val MARGIN_RIGHT = 40f
        const val MARGIN_TOP = 40f
        const val MARGIN_BOTTOM = 45f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT // 515f
        const val PAGE_BOTTOM = PAGE_HEIGHT - MARGIN_BOTTOM

        // FinShield Color Palette
        val COLOR_NAVY = Color.rgb(15, 33, 55)         // #0F2137
        val COLOR_EMERALD = Color.rgb(5, 150, 105)     // #059669
        val COLOR_EMERALD_DARK = Color.rgb(6, 95, 70)  // #065F46
        val COLOR_EMERALD_TINT = Color.rgb(232, 247, 240) // #E8F7F0
        val COLOR_CYAN = Color.rgb(6, 182, 212)        // #06B6D4
        val COLOR_AMBER = Color.rgb(217, 119, 6)       // #D97706
        val COLOR_AMBER_TINT = Color.rgb(254, 243, 199) // #FEF3C7
        val COLOR_RED = Color.rgb(220, 38, 38)         // #DC2626
        val COLOR_RED_TINT = Color.rgb(254, 226, 226)  // #FEE2E2
        val COLOR_CARD_BG = Color.rgb(248, 250, 252)   // #F8FAFC
        val COLOR_BORDER = Color.rgb(226, 232, 240)    // #E2E8F0
        val COLOR_TEXT_PRIMARY = Color.rgb(15, 23, 42) // #0F172A
        val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139) // #64748B
        val COLOR_TEXT_DARK = Color.rgb(30, 41, 59)    // #1E293B

        /**
         * Convenience method to generate the PDF report into a cached temporary file.
         */
        fun generateToCache(
            context: Context,
            report: AnalysisReport,
            options: PdfExportOptions = PdfExportOptions(),
        ): File {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val filename = PdfReportContentBuilder.buildReportFilename(report)
            val file = File(dir, filename)
            FileOutputStream(file).use { os ->
                FinShieldPdfGenerator(report, options).writeTo(os)
            }
            return file
        }
    }

    private val data = PdfReportContentBuilder.buildPdfData(report, options)
    private val pdfDocument = PdfDocument()

    // Paints
    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // Dynamic state
    private var pageIndex = 0
    private var currentPage: PdfDocument.Page? = null
    private var canvas: Canvas = Canvas()
    private var currentY = MARGIN_TOP

    fun writeTo(outputStream: OutputStream) {
        try {
            startNewPage()
            renderDocument()
            finishCurrentPage()
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun startNewPage() {
        finishCurrentPage()
        pageIndex++
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
        val page = pdfDocument.startPage(pageInfo)
        currentPage = page
        canvas = page.canvas

        if (pageIndex > 1) {
            // Running header
            paintText.textSize = 8f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = COLOR_NAVY
            canvas.drawText("FinShield Loan Analysis Report", MARGIN_LEFT, 26f, paintText)

            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.color = COLOR_TEXT_MUTED
            val docLabel = data.metadata.documentName.take(40)
            val docWidth = paintText.measureText(docLabel)
            canvas.drawText(docLabel, PAGE_WIDTH - MARGIN_RIGHT - docWidth, 26f, paintText)

            paintStroke.color = COLOR_CYAN
            paintStroke.strokeWidth = 1f
            canvas.drawLine(MARGIN_LEFT, 32f, PAGE_WIDTH - MARGIN_RIGHT, 32f, paintStroke)

            currentY = MARGIN_TOP + 8f
        } else {
            currentY = MARGIN_TOP
        }
    }

    private fun finishCurrentPage() {
        val page = currentPage ?: return
        // Draw running footer before finishing
        drawFooter(page.canvas, pageIndex)
        pdfDocument.finishPage(page)
        currentPage = null
    }

    private fun drawFooter(c: Canvas, pageNum: Int) {
        paintStroke.color = COLOR_BORDER
        paintStroke.strokeWidth = 0.75f
        c.drawLine(MARGIN_LEFT, PAGE_HEIGHT - 32f, PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - 32f, paintStroke)

        paintText.textSize = 8f
        paintText.color = COLOR_TEXT_MUTED
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerLeft = "FinShield • Understand Before You Borrow • Confidential"
        c.drawText(footerLeft, MARGIN_LEFT, PAGE_HEIGHT - 20f, paintText)

        val pageStr = "Page $pageNum"
        val pWidth = paintText.measureText(pageStr)
        c.drawText(pageStr, PAGE_WIDTH - MARGIN_RIGHT - pWidth, PAGE_HEIGHT - 20f, paintText)
    }

    private fun ensureSpace(neededHeight: Float) {
        if (currentY + neededHeight > PAGE_BOTTOM) {
            startNewPage()
        }
    }

    // -----------------------------------------------------------------------
    // MAIN DOCUMENT RENDERING FLOW
    // -----------------------------------------------------------------------

    private fun renderDocument() {
        renderHeaderBanner()
        renderMetadataCard()
        renderOverviewSection()
        renderNetDisbursalCallout()
        renderChargesSection()
        renderRisksSection()
        renderObligationsSection()
        renderQuestionsSection()
        renderWarningsSection()
        if (options.includeFullTextAppendix && data.appendixPages.isNotEmpty()) {
            renderAppendixSection()
        }
        renderDisclaimerSection()
    }

    // 1. Cover & Brand Banner
    private fun renderHeaderBanner() {
        val bannerHeight = 62f
        paintFill.color = COLOR_NAVY
        canvas.drawRoundRect(
            RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + bannerHeight),
            8f, 8f, paintFill
        )

        // FinShield Brand Mark
        paintText.color = Color.WHITE
        paintText.textSize = 18f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FinShield", MARGIN_LEFT + 14f, currentY + 28f, paintText)

        paintText.color = COLOR_CYAN
        paintText.textSize = 8.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("UNDERSTAND BEFORE YOU BORROW", MARGIN_LEFT + 14f, currentY + 44f, paintText)

        // Document title on the right
        paintText.color = Color.WHITE
        paintText.textSize = 12f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleText = "LOAN ANALYSIS REPORT"
        val tWidth = paintText.measureText(titleText)
        canvas.drawText(titleText, MARGIN_LEFT + CONTENT_WIDTH - 14f - tWidth, currentY + 34f, paintText)

        currentY += bannerHeight + 12f
    }

    // 2. Metadata Grid Card
    private fun renderMetadataCard() {
        val cardHeight = 66f
        paintFill.color = COLOR_CARD_BG
        canvas.drawRoundRect(
            RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + cardHeight),
            6f, 6f, paintFill
        )
        paintStroke.color = COLOR_BORDER
        paintStroke.strokeWidth = 1f
        canvas.drawRoundRect(
            RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + cardHeight),
            6f, 6f, paintStroke
        )

        val col1 = MARGIN_LEFT + 12f
        val col2 = MARGIN_LEFT + (CONTENT_WIDTH * 0.52f)

        // Row 1
        drawMetaPair(col1, currentY + 16f, "Document", data.metadata.documentName)
        drawMetaPair(col2, currentY + 16f, "Analysis Date", data.metadata.generationDate)

        // Row 2
        drawMetaPair(col1, currentY + 34f, "Lender", data.metadata.lenderName)
        drawMetaPair(col2, currentY + 34f, "Borrower", data.metadata.borrowerName)

        // Row 3
        drawMetaPair(col1, currentY + 52f, "Agreement Date", data.metadata.agreementDate)
        drawMetaPair(col2, currentY + 52f, "Extraction Engine", "${data.metadata.extractionSourceSummary} (${data.metadata.completenessPercentage}% Identified)")

        currentY += cardHeight + 16f
    }

    private fun drawMetaPair(x: Float, y: Float, label: String, value: String) {
        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = COLOR_TEXT_MUTED
        canvas.drawText("$label: ", x, y, paintText)

        val labelW = paintText.measureText("$label: ")
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = COLOR_TEXT_DARK
        val maxValW = (CONTENT_WIDTH * 0.44f) - labelW
        val trimmedVal = truncateText(value, maxValW, paintText)
        canvas.drawText(trimmedVal, x + labelW, y, paintText)
    }

    // 3. Loan Overview Section
    private fun renderOverviewSection() {
        drawSectionTitle("1. LOAN OVERVIEW & REPAYMENT SNAPSHOT")

        val rowHeight = 22f
        for ((idx, row) in data.overviewRows.withIndex()) {
            ensureSpace(rowHeight + 4f)
            val y = currentY

            // Row background
            paintFill.color = if (row.isDiscrepancy) COLOR_AMBER_TINT else if (idx % 2 == 0) COLOR_CARD_BG else Color.WHITE
            canvas.drawRect(MARGIN_LEFT, y, MARGIN_LEFT + CONTENT_WIDTH, y + rowHeight, paintFill)

            // Left label
            paintText.textSize = 9f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = if (row.isDiscrepancy) COLOR_AMBER else COLOR_NAVY
            canvas.drawText(row.label, MARGIN_LEFT + 10f, y + 14f, paintText)

            // Right value
            paintText.textSize = 9.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = if (row.isDiscrepancy) COLOR_RED else COLOR_TEXT_PRIMARY
            val valWidth = paintText.measureText(row.value)
            canvas.drawText(row.value, MARGIN_LEFT + CONTENT_WIDTH - 10f - valWidth, y + 14f, paintText)

            // Border bottom
            paintStroke.color = COLOR_BORDER
            paintStroke.strokeWidth = 0.5f
            canvas.drawLine(MARGIN_LEFT, y + rowHeight, MARGIN_LEFT + CONTENT_WIDTH, y + rowHeight, paintStroke)

            currentY += rowHeight
        }
        currentY += 12f
    }

    // 4. Highlighted Net Disbursal Callout
    private fun renderNetDisbursalCallout() {
        val boxHeight = 44f
        ensureSpace(boxHeight + 10f)

        paintFill.color = COLOR_EMERALD_TINT
        canvas.drawRoundRect(
            RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + boxHeight),
            6f, 6f, paintFill
        )
        paintStroke.color = COLOR_EMERALD
        paintStroke.strokeWidth = 1.2f
        canvas.drawRoundRect(
            RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + boxHeight),
            6f, 6f, paintStroke
        )

        // Estimated Net Disbursal
        paintText.textSize = 8.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = COLOR_EMERALD_DARK
        canvas.drawText("ESTIMATED NET DISBURSAL (INTO BANK)", MARGIN_LEFT + 12f, currentY + 16f, paintText)

        paintText.textSize = 14f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = COLOR_NAVY
        canvas.drawText(data.estimatedNetDisbursal, MARGIN_LEFT + 12f, currentY + 34f, paintText)

        // Total Deductions on the right
        paintText.textSize = 8.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = COLOR_AMBER
        val rLabel = "UPFRONT DEDUCTIONS: ${data.totalUpfrontDeductions}"
        val rWidth = paintText.measureText(rLabel)
        canvas.drawText(rLabel, MARGIN_LEFT + CONTENT_WIDTH - 12f - rWidth, currentY + 26f, paintText)

        currentY += boxHeight + 14f
    }

    // 5. Fees & Charges Section
    private fun renderChargesSection() {
        drawSectionTitle("2. ITEMIZED FEES & CHARGES BREAKDOWN")

        // Table Header
        drawChargesTableHeader()

        val col1W = CONTENT_WIDTH * 0.32f
        val col2W = CONTENT_WIDTH * 0.22f
        val col3W = CONTENT_WIDTH * 0.28f
        val col4W = CONTENT_WIDTH * 0.18f

        for ((idx, c) in data.charges.withIndex()) {
            val lines = PdfReportContentBuilder.wrapText(c.basisFormula, 30)
            val rowHeight = (lines.size * 11f + 14f).coerceAtLeast(24f)
            ensureSpace(rowHeight + 4f)

            // Alternate row background
            paintFill.color = if (idx % 2 == 0) COLOR_CARD_BG else Color.WHITE
            canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + rowHeight, paintFill)

            // Col 1: Name + Upfront tag
            paintText.textSize = 8.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = COLOR_NAVY
            canvas.drawText(truncateText(c.name, col1W - 8f, paintText), MARGIN_LEFT + 6f, currentY + 13f, paintText)
            if (c.isUpfront) {
                paintText.textSize = 7f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paintText.color = COLOR_EMERALD_DARK
                canvas.drawText("[Upfront Deduction]", MARGIN_LEFT + 6f, currentY + 22f, paintText)
            }

            // Col 2: Category
            paintText.textSize = 8f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.color = COLOR_TEXT_MUTED
            canvas.drawText(truncateText(c.category, col2W - 8f, paintText), MARGIN_LEFT + col1W + 4f, currentY + 14f, paintText)

            // Col 3: Formula / Basis (wrapped)
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.color = COLOR_TEXT_DARK
            var basisY = currentY + 11f
            for (line in lines.take(2)) {
                canvas.drawText(line, MARGIN_LEFT + col1W + col2W + 4f, basisY, paintText)
                basisY += 10f
            }

            // Col 4: Amount
            paintText.textSize = 9f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = if (c.isUpfront) COLOR_NAVY else COLOR_TEXT_PRIMARY
            val amtW = paintText.measureText(c.amount)
            canvas.drawText(c.amount, MARGIN_LEFT + CONTENT_WIDTH - 6f - amtW, currentY + 14f, paintText)

            // Row hairline
            paintStroke.color = COLOR_BORDER
            paintStroke.strokeWidth = 0.5f
            canvas.drawLine(MARGIN_LEFT, currentY + rowHeight, MARGIN_LEFT + CONTENT_WIDTH, currentY + rowHeight, paintStroke)

            currentY += rowHeight
        }
        currentY += 14f
    }

    private fun drawChargesTableHeader() {
        val hHeight = 18f
        ensureSpace(hHeight + 10f)

        paintFill.color = COLOR_NAVY
        canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + hHeight, paintFill)

        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.WHITE

        val col1W = CONTENT_WIDTH * 0.32f
        val col2W = CONTENT_WIDTH * 0.22f
        val col3W = CONTENT_WIDTH * 0.28f

        canvas.drawText("CHARGE NAME", MARGIN_LEFT + 6f, currentY + 12f, paintText)
        canvas.drawText("CATEGORY", MARGIN_LEFT + col1W + 4f, currentY + 12f, paintText)
        canvas.drawText("CALCULATION BASIS", MARGIN_LEFT + col1W + col2W + 4f, currentY + 12f, paintText)
        val amtH = "AMOUNT"
        val amtHW = paintText.measureText(amtH)
        canvas.drawText(amtH, MARGIN_LEFT + CONTENT_WIDTH - 6f - amtHW, currentY + 12f, paintText)

        currentY += hHeight
    }

    // 6. Risks & Clause Analysis
    private fun renderRisksSection() {
        drawSectionTitle("3. RISK & CLAUSE ANALYSIS (${data.riskItems.size} FINDINGS)")

        for (risk in data.riskItems) {
            val expLines = PdfReportContentBuilder.wrapText(risk.explanation, 80)
            val quoteLines = risk.evidenceQuote?.let { PdfReportContentBuilder.wrapText(it, 76) } ?: emptyList()
            val whyLines = risk.whyItMatters?.let { PdfReportContentBuilder.wrapText(it, 78) } ?: emptyList()

            val estimatedHeight = 28f + (expLines.size * 11f) +
                (if (quoteLines.isNotEmpty()) quoteLines.size * 10f + 16f else 0f) +
                (if (whyLines.isNotEmpty()) whyLines.size * 10f + 12f else 0f) + 16f

            ensureSpace(estimatedHeight)

            // Card background & border
            val top = currentY
            val (badgeBg, badgeText, badgeLabel) = when (risk.severity) {
                Severity.HIGH -> Triple(COLOR_RED_TINT, COLOR_RED, "HIGH ATTENTION")
                Severity.MEDIUM -> Triple(COLOR_AMBER_TINT, COLOR_AMBER, "REVIEW REQUIRED")
                Severity.LOW -> Triple(COLOR_EMERALD_TINT, COLOR_EMERALD, "NOTICE")
                Severity.INFO -> Triple(COLOR_CARD_BG, COLOR_NAVY, "VERIFIED")
            }

            paintFill.color = COLOR_CARD_BG
            canvas.drawRoundRect(RectF(MARGIN_LEFT, top, MARGIN_LEFT + CONTENT_WIDTH, top + estimatedHeight - 4f), 6f, 6f, paintFill)
            paintStroke.color = COLOR_BORDER
            paintStroke.strokeWidth = 0.75f
            canvas.drawRoundRect(RectF(MARGIN_LEFT, top, MARGIN_LEFT + CONTENT_WIDTH, top + estimatedHeight - 4f), 6f, 6f, paintStroke)

            var cy = top + 15f

            // Severity Badge
            val badgeW = 90f
            paintFill.color = badgeBg
            canvas.drawRoundRect(RectF(MARGIN_LEFT + 8f, cy - 10f, MARGIN_LEFT + 8f + badgeW, cy + 3f), 4f, 4f, paintFill)
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = badgeText
            canvas.drawText(badgeLabel, MARGIN_LEFT + 14f, cy - 1f, paintText)

            // Title
            paintText.textSize = 9.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = COLOR_NAVY
            val titleMaxW = CONTENT_WIDTH - badgeW - 32f
            canvas.drawText(truncateText(risk.title, titleMaxW, paintText), MARGIN_LEFT + badgeW + 16f, cy - 1f, paintText)

            cy += 14f

            // Explanation lines
            paintText.textSize = 8.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.color = COLOR_TEXT_DARK
            for (line in expLines) {
                canvas.drawText(line, MARGIN_LEFT + 10f, cy, paintText)
                cy += 11f
            }

            // Evidence quote box
            if (quoteLines.isNotEmpty()) {
                cy += 4f
                val qHeight = quoteLines.size * 10f + 14f
                paintFill.color = Color.rgb(241, 245, 249) // #F1F5F9
                canvas.drawRoundRect(
                    RectF(MARGIN_LEFT + 10f, cy - 8f, MARGIN_LEFT + CONTENT_WIDTH - 10f, cy - 8f + qHeight),
                    4f, 4f, paintFill
                )
                // Left accent bar
                paintFill.color = COLOR_CYAN
                canvas.drawRect(MARGIN_LEFT + 10f, cy - 8f, MARGIN_LEFT + 13f, cy - 8f + qHeight, paintFill)

                paintText.textSize = 7.5f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                paintText.color = COLOR_TEXT_MUTED
                val refInfo = listOfNotNull(risk.clauseRef?.let { "Clause $it" }, risk.pageNumber?.let { "Page $it" }).joinToString(", ")
                if (refInfo.isNotBlank()) {
                    canvas.drawText("Source Evidence ($refInfo):", MARGIN_LEFT + 18f, cy + 1f, paintText)
                    cy += 10f
                }
                for (qLine in quoteLines) {
                    canvas.drawText(qLine, MARGIN_LEFT + 18f, cy + 1f, paintText)
                    cy += 10f
                }
            }

            // Question to ask
            if (risk.questionToAsk != null) {
                cy += 4f
                paintText.textSize = 8f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paintText.color = COLOR_EMERALD_DARK
                canvas.drawText("Suggested Borrower Question: ", MARGIN_LEFT + 10f, cy, paintText)
                val qPromptW = paintText.measureText("Suggested Borrower Question: ")
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paintText.color = COLOR_TEXT_PRIMARY
                val qLine = truncateText(risk.questionToAsk, CONTENT_WIDTH - qPromptW - 20f, paintText)
                canvas.drawText(qLine, MARGIN_LEFT + 10f + qPromptW, cy, paintText)
                cy += 10f
            }

            currentY = top + estimatedHeight + 6f
        }
        currentY += 8f
    }

    // 7. Key Obligations
    private fun renderObligationsSection() {
        drawSectionTitle("4. KEY BORROWER OBLIGATIONS & COVENANTS")

        for (ob in data.obligations) {
            val detLines = PdfReportContentBuilder.wrapText(ob.details, 82)
            val cardHeight = 22f + (detLines.size * 10.5f) + 6f
            ensureSpace(cardHeight + 4f)

            paintFill.color = COLOR_CARD_BG
            canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + cardHeight), 4f, 4f, paintFill)

            paintStroke.color = COLOR_BORDER
            paintStroke.strokeWidth = 0.5f
            canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + cardHeight), 4f, 4f, paintStroke)

            // Category & Status
            paintText.textSize = 8.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = COLOR_NAVY
            canvas.drawText(ob.category, MARGIN_LEFT + 8f, currentY + 12f, paintText)

            paintText.textSize = 7.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = when (ob.status) {
                ObligationStatus.EXTRACTED -> COLOR_EMERALD_DARK
                ObligationStatus.REVIEW_REQUIRED -> COLOR_AMBER
                ObligationStatus.NOT_FOUND -> COLOR_TEXT_MUTED
            }
            val statusStr = "[${ob.status.label}]"
            val statW = paintText.measureText(statusStr)
            canvas.drawText(statusStr, MARGIN_LEFT + CONTENT_WIDTH - 8f - statW, currentY + 12f, paintText)

            var dy = currentY + 23f
            paintText.textSize = 8f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.color = COLOR_TEXT_DARK
            for (line in detLines) {
                canvas.drawText(line, MARGIN_LEFT + 8f, dy, paintText)
                dy += 10.5f
            }

            currentY += cardHeight + 5f
        }
        currentY += 10f
    }

    // 8. Questions for Lender
    private fun renderQuestionsSection() {
        if (data.questions.isEmpty()) return
        drawSectionTitle("5. RECOMMENDED QUESTIONS FOR LENDER")

        for (q in data.questions) {
            val qLines = PdfReportContentBuilder.wrapText("${q.index}. ${q.question}", 82)
            val qHeight = qLines.size * 11.5f + 4f
            ensureSpace(qHeight + 2f)

            paintText.textSize = 8.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.color = COLOR_TEXT_PRIMARY

            var qy = currentY + 10f
            for (line in qLines) {
                canvas.drawText(line, MARGIN_LEFT + 10f, qy, paintText)
                qy += 11.5f
            }
            currentY += qHeight
        }
        currentY += 12f
    }

    // 9. Warnings & Missing Fields
    private fun renderWarningsSection() {
        if (data.warnings.isEmpty()) return
        drawSectionTitle("6. VERIFICATION, MISSING FIELDS & WARNINGS")

        for (w in data.warnings) {
            val wLines = PdfReportContentBuilder.wrapText(w.description, 80)
            val wHeight = 18f + (wLines.size * 10.5f) + 4f
            ensureSpace(wHeight + 4f)

            paintFill.color = if (w.isHighPriority) COLOR_RED_TINT else COLOR_AMBER_TINT
            canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + wHeight), 4f, 4f, paintFill)

            paintText.textSize = 8.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = if (w.isHighPriority) COLOR_RED else COLOR_AMBER
            canvas.drawText(w.title, MARGIN_LEFT + 8f, currentY + 12f, paintText)

            var wy = currentY + 23f
            paintText.textSize = 8f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.color = COLOR_TEXT_DARK
            for (line in wLines) {
                canvas.drawText(line, MARGIN_LEFT + 8f, wy, paintText)
                wy += 10.5f
            }
            currentY += wHeight + 6f
        }
        currentY += 10f
    }

    // 10. Appendix: Full Extracted Text
    private fun renderAppendixSection() {
        startNewPage()
        drawSectionTitle("7. APPENDIX: COMPLETE EXTRACTED DOCUMENT TEXT")

        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paintText.color = COLOR_TEXT_MUTED
        canvas.drawText(
            "Unmodified text extracted by ${data.metadata.extractionSourceSummary} with page boundaries preserved.",
            MARGIN_LEFT, currentY, paintText
        )
        currentY += 16f

        for (p in data.appendixPages) {
            ensureSpace(32f)

            // Page delimiter banner
            paintFill.color = COLOR_CARD_BG
            canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + 16f, paintFill)
            paintStroke.color = COLOR_BORDER
            paintStroke.strokeWidth = 0.5f
            canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + 16f, paintStroke)

            paintText.textSize = 8f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.color = COLOR_NAVY
            canvas.drawText("--- PAGE ${p.pageNumber} (${p.charCount} characters, ${p.sourceKind}) ---", MARGIN_LEFT + 8f, currentY + 11f, paintText)
            currentY += 22f

            // Text lines
            val lines = PdfReportContentBuilder.wrapText(p.text, 86)
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            paintText.color = COLOR_TEXT_DARK

            for (line in lines) {
                ensureSpace(10.5f)
                canvas.drawText(line, MARGIN_LEFT + 6f, currentY, paintText)
                currentY += 10.5f
            }
            currentY += 14f
        }
        currentY += 12f
    }

    // 11. Disclaimer
    private fun renderDisclaimerSection() {
        val lines = PdfReportContentBuilder.wrapText(data.disclaimer, 88)
        val boxHeight = (lines.size * 9.5f) + 16f
        ensureSpace(boxHeight + 8f)

        paintFill.color = COLOR_CARD_BG
        canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + boxHeight), 4f, 4f, paintFill)
        paintStroke.color = COLOR_BORDER
        paintStroke.strokeWidth = 0.5f
        canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + boxHeight), 4f, 4f, paintStroke)

        var dy = currentY + 12f
        paintText.textSize = 7f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = COLOR_TEXT_MUTED

        for (line in lines) {
            canvas.drawText(line, MARGIN_LEFT + 8f, dy, paintText)
            dy += 9.5f
        }
        currentY += boxHeight + 10f
    }

    // -----------------------------------------------------------------------
    // DRAWING HELPERS
    // -----------------------------------------------------------------------

    private fun drawSectionTitle(title: String) {
        val hHeight = 22f
        ensureSpace(hHeight + 10f)

        // Navy Accent Bar on left
        paintFill.color = COLOR_EMERALD
        canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + 4f, currentY + 16f), 2f, 2f, paintFill)

        paintText.textSize = 10f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = COLOR_NAVY
        canvas.drawText(title, MARGIN_LEFT + 10f, currentY + 13f, paintText)

        currentY += hHeight
    }

    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length - 1
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + "…" else text
    }
}
