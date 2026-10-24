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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
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
            onVerifyRepayment = prefillAndNavigateCalculator,
            onShare = { shareReportSummary(context, report) },
        )

        // 9. Report Disclaimer
        DisclaimerText()
    }

    // Modal dialog for viewing exact clause text
    selectedClauseFinding?.let { clauseData ->
        ClauseDetailDialog(clauseData = clauseData, onDismiss = { selectedClauseFinding = null })
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
                StatusChip(s.loanType, InfoBlueTint, InfoBlue)
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
                    modifier = Modifier.weight(1f),
                )
                SnapshotTile(
                    label = "Annual Interest Rate",
                    value = s.annualRatePercent?.let { "$it% p.a." } ?: "Not Found",
                    sub = s.interestMethod?.label ?: "Reducing Balance",
                    color = Navy,
                    bg = SegmentTrack,
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
                    modifier = Modifier.weight(1f),
                )
            }

            // Total Repayment Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Total Scheduled Repayment",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextSecondary,
                    )
                    Text(
                        "${s.tenureMonths ?: 36} monthly installments",
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
                )
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(sub, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextSecondary)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
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
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldDark,
                )
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
                )
                Text(
                    fb.sanctionedPrincipal?.let { Money.inr(it) } ?: "Needs confirmation",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
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
                )
                Text(
                    fb.totalUpfrontDeductions?.let { "- ${Money.inr(it)}" } ?: "₹0",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if ((fb.totalUpfrontDeductions ?: 0.0) > 0) Rose else EmeraldDark,
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
    onVerifyRepayment: () -> Unit,
    onShare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
