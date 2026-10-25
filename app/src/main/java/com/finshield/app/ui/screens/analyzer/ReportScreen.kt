package com.finshield.app.ui.screens.analyzer

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.core.content.FileProvider
import com.finshield.app.domain.pdf.FinShieldPdfGenerator
import com.finshield.app.domain.pdf.PdfExportOptions
import com.finshield.app.domain.pdf.PdfReportContentBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.finshield.app.domain.calculator.CalculatorPrefill
import com.finshield.app.domain.calculator.CalculatorPrefillStore
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.AnalysisReportStore
import com.finshield.app.model.ChargeType
import com.finshield.app.model.FinancialCharge
import com.finshield.app.model.Finding
import com.finshield.app.model.RiskFinding
import com.finshield.app.model.SampleReports
import com.finshield.app.model.Severity
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.PrimaryButton
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.StatusChip
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.Amber
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.ErrorRed
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.RoseTint
import com.finshield.app.ui.theme.SegmentTrack
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Compact Analysis Report Screen (Feature 1 & Feature 2).
 *
 * Keeps the main report clean, focused and readable:
 * 1. Report header and document filename.
 * 2. Compact loan snapshot: principal, rate, tenure, EMI, total repayment.
 * 3. Prominent green Estimated Net Disbursal card.
 * 4. High-visibility "Complete Breakdown" button immediately below the green card.
 * 5. Horizontally swipeable Risk Review pager (one finding at a time, accessibility controls, position indicator).
 * 6. Quick repayment verification and concise regulatory disclaimer.
 *
 * The long itemized breakdown, full fee schedules and foreclosure scenarios are moved
 * to [CompleteBreakdownScreen].
 */
@Composable
fun ReportScreen(
    reportId: String,
    onBack: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenCompleteBreakdown: (String) -> Unit = {},
    onOpenOcrReview: (String) -> Unit = {},
) {
    StatusBarIcons(darkIcons = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    BackHandler { onBack() }

    val report = remember(reportId) {
        AnalysisReportStore.getReport(reportId) ?: SampleReports.forId(reportId)
    }

    var selectedClauseFinding by remember { mutableStateOf<ClauseViewData?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullTextDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var includeFullTextAppendix by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var showDownloadSuccessDialog by remember { mutableStateOf(false) }

    val suggestedFilename = remember(report) {
        PdfReportContentBuilder.buildReportFilename(report)
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        isGeneratingPdf = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        FinShieldPdfGenerator(
                            report = report,
                            options = PdfExportOptions(includeFullTextAppendix = includeFullTextAppendix)
                        ).writeTo(os)
                    } ?: throw IllegalStateException("Unable to open output destination for writing.")

                    val cached = FinShieldPdfGenerator.generateToCache(
                        context = context,
                        report = report,
                        options = PdfExportOptions(includeFullTextAppendix = includeFullTextAppendix)
                    )
                    downloadedFile = cached
                }
                showDownloadSuccessDialog = true
                snackbar.showSnackbar("Report PDF successfully saved to your device.")
            } catch (e: Exception) {
                snackbar.showSnackbar("Failed to download PDF: ${e.message ?: "Unknown error"}")
            } finally {
                isGeneratingPdf = false
            }
        }
    }

    if (showFullTextDialog) {
        FullDocumentTextViewerDialog(
            report = report,
            onDismiss = { showFullTextDialog = false },
        )
    }

    val prefillAndNavigateCalculator: () -> Unit = {
        val p = report.loanSnapshot.sanctionedPrincipal
        val r = report.loanSnapshot.annualRatePercent
        val n = report.loanSnapshot.tenureMonths
        val m = report.loanSnapshot.interestMethod ?: InterestMethod.REDUCING_BALANCE
        CalculatorPrefillStore.set(CalculatorPrefill(p, r, n, m))
        onOpenCalculator()
    }

    ScreenScaffold(
        title = "Analysis Report",
        subtitle = report.documentName,
        onBack = onBack,
        snackbarHostState = snackbar,
        handleNavBarInset = false,
    ) {
        // 1. Report Header Section
        ReportHeaderSection(
            report = report,
            onEdit = { onOpenOcrReview(report.id) },
            onSave = {
                scope.launch {
                    snackbar.showSnackbar("Report saved to your active session.")
                }
            },
            onShare = {
                shareReportSummary(context, report)
            },
            onDelete = { showDeleteConfirm = true },
        )

        // 2. Prominent Download Report as PDF Section
        DownloadPdfCard(
            isGenerating = isGeneratingPdf,
            includeAppendix = includeFullTextAppendix,
            hasAppendixPages = report.rawPages.isNotEmpty(),
            appendixPagesCount = report.rawPages.size,
            downloadedFile = downloadedFile,
            onToggleAppendix = { includeFullTextAppendix = it },
            onDownloadPdf = {
                if (!isGeneratingPdf) {
                    createDocumentLauncher.launch(suggestedFilename)
                }
            },
            onOpenPdf = { file ->
                openPdfFile(context, file)
            },
            onSharePdf = { file ->
                sharePdfFile(context, file, report.documentName)
            },
        )

        // Full Extracted Text & Grounding Card
        FinCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showFullTextDialog = true },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Full Extracted Document Text",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                        )
                        Text(
                            text = "${report.extractionSourceSummary} • Search & Grounding",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
                StatusChip(
                    text = "${report.completenessReport?.completenessPercentage ?: 0}% Identified",
                    background = EmeraldTint,
                    content = EmeraldDark,
                )
            }
        }

        // 2. Compact Loan Snapshot
        CompactLoanSnapshotSection(report)

        // 3. Green Estimated Net Disbursal Card
        EstimatedNetDisbursalCard(report = report)

        // 4. Prominent "Complete Breakdown" CTA Button Immediately Below Green Card
        CompleteBreakdownCtaButton(
            onClick = { onOpenCompleteBreakdown(report.id) },
        )

        // 5. Swipeable Risk Review (Horizontally swipeable pager, 1 finding at a time)
        SwipeableRiskReviewSection(
            report = report,
            onViewClause = { selectedClauseFinding = it },
        )

        // 6. Fees and Charges (Restored compact summary on main report)
        CompactFeesAndChargesSection(
            report = report,
            onViewClause = { selectedClauseFinding = it },
        )

        // 7. Questions to Ask the Lender (Restored dynamic questions)
        QuestionsForLenderSection(
            questions = report.questionsForLender,
        )

        // 8. Extracted Text & Calculation Evidence (Collapsible section)
        CollapsibleEvidenceSection(
            report = report,
            onViewClause = { selectedClauseFinding = it },
        )

        // Supported Action Buttons
        ReportFooterActions(
            onOpenCompleteBreakdown = { onOpenCompleteBreakdown(report.id) },
            onOpenOcrReview = { onOpenOcrReview(report.id) },
            onVerifyRepayment = prefillAndNavigateCalculator,
            onDownloadPdf = {
                if (!isGeneratingPdf) {
                    createDocumentLauncher.launch(suggestedFilename)
                }
            },
            onShare = { shareReportSummary(context, report) },
        )

        // 9. Report Disclaimer
        DisclaimerText()
    }

    // Modal dialog for viewing exact clause text
    selectedClauseFinding?.let { clauseData ->
        ClauseDetailDialog(clauseData = clauseData, onDismiss = { selectedClauseFinding = null })
    }

    // Modal dialog for successful PDF download (Open & Share)
    if (showDownloadSuccessDialog && downloadedFile != null) {
        val file = downloadedFile!!
        AlertDialog(
            onDismissRequest = { showDownloadSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(36.dp),
                )
            },
            title = {
                Text(
                    "Report PDF Downloaded",
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your FinShield analysis report has been generated and saved successfully to your selected location.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        file.name,
                        color = Navy,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        sharePdfFile(context, file, report.documentName)
                    }) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp), tint = Navy)
                        Spacer(Modifier.width(4.dp))
                        Text("Share", color = Navy, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = {
                        showDownloadSuccessDialog = false
                        openPdfFile(context, file)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = EmeraldDark)
                        Spacer(Modifier.width(4.dp))
                        Text("Open PDF", color = EmeraldDark, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadSuccessDialog = false }) {
                    Text("Done", color = TextSecondary)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(20.dp),
        )
    }

    // Modal dialog for delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Report", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this analysis report from this session?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 1. REPORT HEADER SECTION
// ---------------------------------------------------------------------------

@Composable
private fun ReportHeaderSection(
    report: AnalysisReport,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Analysis Report",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Text(
                        report.documentName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary,
                        maxLines = 1,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Review Extracted Terms", tint = EmeraldDark)
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Report", tint = EmeraldDark)
                    }
                    IconButton(onClick = onSave, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Report", tint = EmeraldDark)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextHint)
                    }
                }
            }

            HorizontalDivider(color = DividerColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusChip("Verified OCR", EmeraldTint, EmeraldDark)
                    if (report.isSample) {
                        StatusChip("Sample Document", AmberTint, AmberDark)
                    } else {
                        StatusChip("${report.pagesCount} Page${if (report.pagesCount != 1) "s" else ""}", SegmentTrack, TextSecondary)
                    }
                }
                val dateStr = remember(report.analyzedAt) {
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(report.analyzedAt))
                }
                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextHint,
                )
            }

            if (report.isSessionOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SegmentTrack, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Text(
                        "Session memory only. Generated locally on this device.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. COMPACT LOAN SNAPSHOT SECTION
// ---------------------------------------------------------------------------

@Composable
private fun CompactLoanSnapshotSection(report: AnalysisReport) {
    val s = report.loanSnapshot

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Loan Snapshot",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Navy,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (s.aprPercent != null) {
                        StatusChip("APR: ${s.aprPercent}%", AmberTint, AmberDark)
                    }
                    StatusChip(s.loanType, InfoBlueTint, InfoBlue)
                }
            }

            // Agreement & Parties Metadata Header if present
            if (!s.lenderName.isNullOrBlank() || !s.borrowerName.isNullOrBlank() || !s.referenceNumber.isNullOrBlank() || !s.agreementDate.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SegmentTrack, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (!s.lenderName.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lender / Bank:", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextHint)
                            Text(s.lenderName, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = Navy)
                        }
                    }
                    if (!s.borrowerName.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Borrower:", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextHint)
                            Text(s.borrowerName, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = TextPrimary)
                        }
                    }
                    if (!s.referenceNumber.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Agreement / Sanction Ref:", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextHint)
                            Text(s.referenceNumber, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = TextSecondary)
                        }
                    }
                    if (!s.agreementDate.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Agreement Date:", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextHint)
                            Text(s.agreementDate, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
                        }
                    }
                }
            }

            HorizontalDivider(color = DividerColor)

            // Principal & Interest Rate
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SnapshotTile(
                    label = "Sanctioned Principal",
                    value = s.sanctionedPrincipal?.let { Money.inr2(it) } ?: "Needs Confirmation",
                    sub = "Base Approved Amount",
                    color = EmeraldDark,
                    bg = EmeraldTint,
                    provenance = if (s.sanctionedPrincipal != null) "Extracted from document" else "Needs verification",
                    provenanceColor = if (s.sanctionedPrincipal != null) EmeraldDark else AmberDark,
                    provenanceBg = if (s.sanctionedPrincipal != null) CardWhite else AmberTint,
                    modifier = Modifier.weight(1f),
                )
                SnapshotTile(
                    label = "Annual Interest Rate",
                    value = s.annualRatePercent?.let { "$it% p.a." } ?: "Not Found",
                    sub = s.interestMethod?.label ?: "Reducing Balance",
                    color = Navy,
                    bg = SegmentTrack,
                    provenance = if (s.annualRatePercent != null) {
                        if (s.isFloatingRate == true) "Floating (Extracted)" else "Extracted from document"
                    } else "Not found in document",
                    provenanceColor = if (s.annualRatePercent != null) Navy else Rose,
                    provenanceBg = if (s.annualRatePercent != null) CardWhite else RoseTint,
                    modifier = Modifier.weight(1f),
                )
            }

            // Tenure & Monthly EMI
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SnapshotTile(
                    label = "Tenure",
                    value = s.tenureMonths?.let { "$it Months" } ?: "Not Found",
                    sub = s.tenureYears?.let { String.format(Locale.US, "%.1f Years", it) } ?: "Unspecified",
                    color = Navy,
                    bg = SegmentTrack,
                    provenance = if (s.tenureMonths != null) "Extracted from document" else "Not found in document",
                    provenanceColor = if (s.tenureMonths != null) Navy else Rose,
                    provenanceBg = if (s.tenureMonths != null) CardWhite else RoseTint,
                    modifier = Modifier.weight(1f),
                )
                SnapshotTile(
                    label = "Monthly EMI",
                    value = s.statedEmi?.let { Money.inr2(it) }
                        ?: s.calculatedEmi?.let { Money.inr2(it) }
                        ?: "Needs Confirmation",
                    sub = if (s.statedEmi != null) "Stated in Agreement" else "FinShield Calculated",
                    color = if (s.emiDifference != null && abs(s.emiDifference) >= 50) AmberDark else EmeraldDark,
                    bg = if (s.emiDifference != null && abs(s.emiDifference) >= 50) AmberTint else EmeraldTint,
                    provenance = if (s.statedEmi != null) "Extracted from document"
                    else if (s.calculatedEmi != null) "Independently calculated"
                    else "Needs verification",
                    provenanceColor = if (s.statedEmi != null) EmeraldDark else if (s.calculatedEmi != null) InfoBlue else AmberDark,
                    provenanceBg = CardWhite,
                    modifier = Modifier.weight(1f),
                )
            }

            // Total Repayment Row
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 340.dp
                if (isNarrow) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppBackground, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Total Scheduled Repayment",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary,
                        )
                        Text(
                            s.statedTotalRepayment?.let { Money.inr2(it) }
                                ?: s.calculatedTotalRepayment?.let { Money.inr2(it) }
                                ?: "Needs Confirmation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Navy,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            "${s.tenureMonths ?: 36} monthly installments • ${if (s.statedTotalRepayment != null) "Extracted from document" else "Independently calculated"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextHint,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppBackground, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                "Total Scheduled Repayment",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextSecondary,
                            )
                            Text(
                                "${s.tenureMonths ?: 36} monthly installments • ${if (s.statedTotalRepayment != null) "Extracted from document" else "Independently calculated"}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextHint,
                            )
                        }
                        Text(
                            s.statedTotalRepayment?.let { Money.inr2(it) }
                                ?: s.calculatedTotalRepayment?.let { Money.inr2(it) }
                                ?: "Needs Confirmation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Navy,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotTile(
    label: String,
    value: String,
    sub: String,
    color: Color,
    bg: Color,
    provenance: String? = null,
    provenanceColor: Color = EmeraldDark,
    provenanceBg: Color = CardWhite,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color, maxLines = 1, softWrap = false)
        Text(sub, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextSecondary, maxLines = 1)
        if (!provenance.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(provenanceBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    provenance,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = provenanceColor,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. GREEN ESTIMATED NET DISBURSAL CARD
// ---------------------------------------------------------------------------

@Composable
private fun EstimatedNetDisbursalCard(report: AnalysisReport) {
    val fb = report.financialBreakdown

    FinCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EmeraldTint, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 340.dp
                if (isNarrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "ESTIMATED NET DISBURSAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                            color = EmeraldDark,
                        )
                        Text(
                            fb.estimatedNetDisbursal?.let { Money.inr2(it) } ?: "Needs Principal",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldDark,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            "Amount transferred to your bank account",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                "ESTIMATED NET DISBURSAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = EmeraldDark,
                            )
                            Text(
                                "Amount transferred to your bank account",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                            )
                        }
                        Text(
                            fb.estimatedNetDisbursal?.let { Money.inr2(it) } ?: "Needs Principal",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            ),
                            color = EmeraldDark,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            HorizontalDivider(color = EmeraldDark.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Sanctioned Principal:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    fb.sanctionedPrincipal?.let { Money.inr(it) } ?: "Needs confirmation",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Total Upfront Deductions:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    fb.totalUpfrontDeductions?.let { "- ${Money.inr(it)}" } ?: "₹0",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if ((fb.totalUpfrontDeductions ?: 0.0) > 0) Rose else EmeraldDark,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Text(
                "Only deductions explicitly deducted before or at disbursal (processing fees, GST, stamp duty) are subtracted.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic),
                color = TextHint,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. PROMINENT "COMPLETE BREAKDOWN" BUTTON IMMEDIATELY BELOW GREEN CARD
// ---------------------------------------------------------------------------

@Composable
private fun CompleteBreakdownCtaButton(onClick: () -> Unit) {
    PrimaryButton(
        text = "Complete Breakdown",
        onClick = onClick,
        trailingIcon = Icons.AutoMirrored.Filled.ListAlt,
    )
}

// ---------------------------------------------------------------------------
// 5. HORIZONTALLY SWIPEABLE RISK REVIEW SECTION (FEATURE 2)
// ---------------------------------------------------------------------------

@Composable
private fun SwipeableRiskReviewSection(
    report: AnalysisReport,
    onViewClause: (ClauseViewData) -> Unit,
) {
    val findings = report.riskFindings
    val count = findings.size
    val scope = rememberCoroutineScope()

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with severity badge and count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Risk Review",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Text(
                        if (count == 0) "No attention items found"
                        else "$count ${if (count == 1) "item" else "items"} identified for confirmation",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                if (count > 0) {
                    val highCount = findings.count { it.severity == Severity.HIGH }
                    StatusChip(
                        text = if (highCount > 0) "$highCount High Attention" else "$count Review Required",
                        background = if (highCount > 0) RoseTint else AmberTint,
                        content = if (highCount > 0) Rose else AmberDark,
                    )
                } else {
                    StatusChip("All Clear", EmeraldTint, EmeraldDark)
                }
            }

            HorizontalDivider(color = DividerColor)

            if (count == 0) {
                // Empty state handled cleanly without crashes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmeraldTint, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            "No High-Severity Risks Detected",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldDark,
                        )
                        Text(
                            "All analyzed clauses align with standard regulatory thresholds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            } else {
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { count },
                )

                // Navigation controls & position indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Finding ${pagerState.currentPage + 1} of $count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous finding",
                                tint = if (pagerState.currentPage > 0) Navy else TextHint,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < count - 1) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < count - 1,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next finding",
                                tint = if (pagerState.currentPage < count - 1) Navy else TextHint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Horizontal Pager displaying one finding at a time with subtle edge preview
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) { pageIndex ->
                    val finding = findings[pageIndex]
                    SwipeableRiskCard(
                        finding = finding,
                        onViewClause = {
                            onViewClause(
                                ClauseViewData(
                                    title = finding.title,
                                    explanation = finding.explanation,
                                    evidence = finding.evidence,
                                    pageNumber = finding.pageNumber,
                                    clauseReference = finding.clauseReference,
                                    whyItMatters = finding.whyItMatters,
                                    questionToAsk = finding.questionToAsk,
                                ),
                            )
                        },
                    )
                }

                // Dot pager indicator for multi-item finding lists
                if (count > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(count) { i ->
                            val isSelected = pagerState.currentPage == i
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) EmeraldDark else DividerColor),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Card in the Swipeable Risk Pager.
 */
@Composable
private fun SwipeableRiskCard(
    finding: RiskFinding,
    onViewClause: () -> Unit,
) {
    val (statusLabel, chipBg, chipText) = when (finding.severity) {
        Severity.HIGH -> Triple("High Attention", RoseTint, Rose)
        Severity.MEDIUM -> Triple("Review Required", AmberTint, AmberDark)
        Severity.LOW -> Triple("Verified", InfoBlueTint, InfoBlue)
        Severity.INFO -> Triple("Informational", EmeraldTint, EmeraldDark)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground, RoundedCornerShape(14.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(statusLabel, chipBg, chipText)
            finding.pageNumber?.let { page ->
                Text(
                    "Page $page${finding.clauseReference?.let { ", $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = TextHint,
                )
            }
        }

        Text(
            text = finding.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
        )

        Text(
            text = finding.explanation,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 18.sp,
        )

        if (!finding.whyItMatters.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InfoBlueTint.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Why this matters to the borrower:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = InfoBlue,
                )
                Text(
                    finding.whyItMatters,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextPrimary,
                )
            }
        }

        if (!finding.questionToAsk.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SegmentTrack, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Question to ask the lender:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Navy,
                )
                Text(
                    finding.questionToAsk,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = TextPrimary,
                )
            }
        }

        // Estimated Rupee Impact (only when calculable)
        if (!finding.financialImpactDescription.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmberTint.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = AmberDark,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Estimated Impact: ${finding.financialImpactDescription}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = AmberDark,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onViewClause) {
                Icon(
                    Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "View Clause",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldDark,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. RESTORED: COMPACT FEES AND CHARGES SECTION
// ---------------------------------------------------------------------------

@Composable
private fun CompactFeesAndChargesSection(
    report: AnalysisReport,
    onViewClause: (ClauseViewData) -> Unit,
) {
    // Filter to fee & charge items, excluding principal and scheduled interest which belong to loan snapshot/breakdown
    val charges = report.feesAndCharges.filter { charge ->
        !charge.name.contains("Principal", ignoreCase = true) &&
            !charge.name.contains("Scheduled Interest", ignoreCase = true)
    }

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Fees and Charges",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Text(
                        "Extracted statutory & lender charges",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                val count = if (charges.isNotEmpty()) charges.size else report.feesAndPenalties.size
                StatusChip(
                    text = if (count > 0) "$count Disclosed" else "Review Schedule",
                    background = if (count > 0) InfoBlueTint else AmberTint,
                    content = if (count > 0) InfoBlue else AmberDark,
                )
            }

            HorizontalDivider(color = DividerColor)

            if (charges.isEmpty()) {
                if (report.feesAndPenalties.isNotEmpty()) {
                    // Fallback to legacy fee findings if itemized charges are not populated
                    report.feesAndPenalties.forEach { fee ->
                        LegacyFeeRow(fee = fee, onViewClause = onViewClause)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppBackground, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "No itemized fee clauses were detected in the scanned text. Verify tariff schedule directly with lender.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    charges.forEach { charge ->
                        CompactFeeRow(charge = charge, onViewClause = onViewClause)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFeeRow(
    charge: FinancialCharge,
    onViewClause: (ClauseViewData) -> Unit,
) {
    val (typeLabel, typeBg, typeColor) = when {
        charge.isUpfrontDeduction -> Triple("Upfront", RoseTint, Rose)
        charge.chargeType == ChargeType.CONDITIONAL -> Triple("Conditional", AmberTint, AmberDark)
        charge.chargeType == ChargeType.RECURRING -> Triple("Recurring", InfoBlueTint, InfoBlue)
        else -> Triple(charge.chargeType.label, SegmentTrack, TextSecondary)
    }

    val amountStr = when {
        charge.fixedAmount != null -> Money.inr(charge.fixedAmount)
        charge.percentage != null -> {
            val pctStr = if (charge.percentage % 1.0 == 0.0) "${charge.percentage.toInt()}%" else "${charge.percentage}%"
            if (charge.calculatedAmount != null) {
                "$pctStr (${Money.inr(charge.calculatedAmount)})"
            } else {
                pctStr
            }
        }
        charge.calculatedAmount != null -> Money.inr(charge.calculatedAmount)
        charge.totalWithGst != null -> Money.inr(charge.totalWithGst)
        else -> "Needs confirmation"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground, RoundedCornerShape(10.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = charge.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                StatusChip(typeLabel, typeBg, typeColor)
            }

            Text(
                text = amountStr,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (charge.isUpfrontDeduction) Rose else Navy,
                textAlign = TextAlign.End,
            )
        }

        Text(
            text = charge.formulaDescription,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextSecondary,
        )

        charge.assumptions?.let { assumption ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmberTint.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Assumption: $assumption",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic),
                    color = AmberDark,
                )
            }
        }

        // Clause & Page attribution + View Clause action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val locText = buildString {
                charge.pageNumber?.let { append("Page $it") }
                charge.clauseReference?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
            }
            if (locText.isNotBlank()) {
                Text(
                    text = locText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextHint,
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (!charge.evidence.isNullOrBlank()) {
                Text(
                    text = "View Clause",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark,
                    ),
                    modifier = Modifier
                        .clickable {
                            onViewClause(
                                ClauseViewData(
                                    title = charge.name,
                                    explanation = charge.formulaDescription + (charge.assumptions?.let { "\n\nAssumption: $it" } ?: ""),
                                    evidence = charge.evidence,
                                    pageNumber = charge.pageNumber,
                                    clauseReference = charge.clauseReference,
                                ),
                            )
                        }
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LegacyFeeRow(
    fee: Finding,
    onViewClause: (ClauseViewData) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground, RoundedCornerShape(10.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = fee.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            fee.clauseReference?.let { ref ->
                Text(
                    text = ref,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextHint,
                )
            }
        }
        Text(
            text = fee.value ?: "Not stated",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Navy,
        )
    }
}

// ---------------------------------------------------------------------------
// 7. RESTORED: QUESTIONS TO ASK THE LENDER
// ---------------------------------------------------------------------------

@Composable
private fun QuestionsForLenderSection(
    questions: List<String>,
) {
    var showAll by remember { mutableStateOf(false) }
    val displayQuestions = if (showAll || questions.size <= 5) questions else questions.take(5)

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Questions to Ask the Lender",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Text(
                        "Clarifications derived from agreement analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                StatusChip(
                    text = if (questions.isNotEmpty()) "${questions.size} Questions" else "All Clear",
                    background = if (questions.isNotEmpty()) EmeraldTint else AppBackground,
                    content = if (questions.isNotEmpty()) EmeraldDark else TextSecondary,
                )
            }

            HorizontalDivider(color = DividerColor)

            if (questions.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppBackground, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "No specific discrepancies detected that require questions for the lender.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayQuestions.forEachIndexed { index, question ->
                        QuestionCardItem(number = index + 1, question = question)
                    }
                }

                if (questions.size > 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAll = !showAll }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (showAll) "Show Less" else "Show All (${questions.size} Questions)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldDark,
                        )
                        Icon(
                            imageVector = if (showAll) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCardItem(number: Int, question: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground, RoundedCornerShape(10.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(EmeraldTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = EmeraldDark,
            )
        }
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 8. RESTORED: EXTRACTED TEXT & CALCULATION EVIDENCE (COLLAPSIBLE)
// ---------------------------------------------------------------------------

@Composable
private fun CollapsibleEvidenceSection(
    report: AnalysisReport,
    onViewClause: (ClauseViewData) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    FinCard {
        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Extracted Text & Calculation Evidence",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Text(
                        "Collapsible page transcripts & clause proof",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (report.rawPages.isEmpty() && report.terms.isEmpty()) {
                        Text(
                            "No raw OCR page transcripts or calculation evidence stored.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHint,
                        )
                    } else {
                        // Raw OCR page transcripts
                        report.rawPages.forEach { page ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppBackground, RoundedCornerShape(10.dp))
                                    .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Page ${page.pageNumber}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldDark,
                                    )
                                    Text(
                                        "${page.charCount} characters",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = TextHint,
                                    )
                                }
                                Text(
                                    text = page.text.ifBlank { "[Blank page or unreadable image]" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = 16.sp,
                                    ),
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 9. REPORT FOOTER ACTIONS
// ---------------------------------------------------------------------------

@Composable
private fun ReportFooterActions(
    onOpenCompleteBreakdown: () -> Unit,
    onOpenOcrReview: () -> Unit,
    onVerifyRepayment: () -> Unit,
    onDownloadPdf: () -> Unit,
    onShare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton(
            text = "Download Report (PDF)",
            onClick = onDownloadPdf,
            leadingIcon = Icons.Filled.PictureAsPdf,
        )

        SecondaryButton(
            text = "Review & Edit Extracted Fields",
            onClick = onOpenOcrReview,
            leadingIcon = Icons.Filled.Edit,
        )

        SecondaryButton(
            text = "Verify Repayment in Calculator",
            onClick = onVerifyRepayment,
            leadingIcon = Icons.Filled.Calculate,
        )

        SecondaryButton(
            text = "Share Analysis Summary",
            onClick = onShare,
            leadingIcon = Icons.Filled.Share,
        )
    }
}


// ---------------------------------------------------------------------------
// SHARING HELPER
// ---------------------------------------------------------------------------

private fun shareReportSummary(context: Context, report: AnalysisReport) {
    val s = report.loanSnapshot
    val fb = report.financialBreakdown
    val shareText = buildString {
        append("FinShield Loan Agreement Analysis Report\n")
        append("Document: ${report.documentName}\n\n")
        append("LOAN SNAPSHOT:\n")
        append("• Loan Type: ${s.loanType}\n")
        append("• Sanctioned Principal: ${s.sanctionedPrincipal?.let { Money.inr2(it) } ?: "N/A"}\n")
        append("• Interest Rate: ${s.annualRatePercent?.let { "$it%" } ?: "N/A"} (${s.interestMethod?.label ?: "Unknown"})\n")
        append("• Tenure: ${s.tenureMonths?.let { "$it months" } ?: "N/A"}\n")
        append("• Stated EMI: ${s.statedEmi?.let { Money.inr2(it) } ?: "N/A"}\n")
        append("• Estimated Net Disbursed: ${fb.estimatedNetDisbursal?.let { Money.inr2(it) } ?: "N/A"}\n")
        append("• Upfront Deductions: ${fb.totalUpfrontDeductions?.let { Money.inr2(it) } ?: "₹0"}\n\n")
        if (report.riskFindings.isNotEmpty()) {
            append("KEY RISKS & DISCREPANCIES (${report.riskFindings.size}):\n")
            report.riskFindings.forEach { r ->
                append("• [${r.severity.label}] ${r.title}: ${r.explanation}\n")
            }
        }
        append("\nGenerated securely on-device with FinShield.")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "FinShield Report: ${report.documentName}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share FinShield Report"))
}

// ---------------------------------------------------------------------------
// 10. DOWNLOAD PDF CARD & FILE HELPERS
// ---------------------------------------------------------------------------

@Composable
private fun DownloadPdfCard(
    isGenerating: Boolean,
    includeAppendix: Boolean,
    hasAppendixPages: Boolean,
    appendixPagesCount: Int,
    downloadedFile: File?,
    onToggleAppendix: (Boolean) -> Unit,
    onDownloadPdf: () -> Unit,
    onOpenPdf: (File) -> Unit,
    onSharePdf: (File) -> Unit,
) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(EmeraldTint, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PictureAsPdf,
                        contentDescription = "PDF Document",
                        tint = EmeraldDark,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download Analysis Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                    )
                    Text(
                        text = "Comprehensive multi-page PDF formatted to print or share",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            if (hasAppendixPages) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardWhite, RoundedCornerShape(10.dp))
                        .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                        .clickable(enabled = !isGenerating) { onToggleAppendix(!includeAppendix) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = includeAppendix,
                        onCheckedChange = onToggleAppendix,
                        enabled = !isGenerating,
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldButton,
                            checkmarkColor = Color.White,
                            uncheckedColor = TextHint,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Include full extracted document text appendix",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                        )
                        Text(
                            text = "Preserves all $appendixPagesCount raw extracted pages with character counts",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                        )
                    }
                }
            }

            PrimaryButton(
                text = if (isGenerating) "Generating PDF Report..." else "Download PDF",
                onClick = onDownloadPdf,
                loading = isGenerating,
                enabled = !isGenerating,
                leadingIcon = Icons.Filled.Download,
            )

            if (downloadedFile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SecondaryButton(
                            text = "Open PDF",
                            onClick = { onOpenPdf(downloadedFile) },
                            leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SecondaryButton(
                            text = "Share PDF",
                            onClick = { onSharePdf(downloadedFile) },
                            leadingIcon = Icons.Filled.Share,
                        )
                    }
                }
            }
        }
    }
}

private fun openPdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "No PDF viewer application found on device.",
            Toast.LENGTH_LONG,
        ).show()
    }
}

private fun sharePdfFile(context: Context, file: File, documentName: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FinShield Report: $documentName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share FinShield PDF Report"))
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Could not share PDF: ${e.message}",
            Toast.LENGTH_SHORT,
        ).show()
    }
}

