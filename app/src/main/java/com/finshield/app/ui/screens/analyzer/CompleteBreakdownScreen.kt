package com.finshield.app.ui.screens.analyzer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.finshield.app.domain.calculator.CalculatorPrefill
import com.finshield.app.domain.calculator.CalculatorPrefillStore
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.AnalysisReportStore
import com.finshield.app.model.ChargeType
import com.finshield.app.model.FinancialCharge
import com.finshield.app.model.SampleReports
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.SegmentedToggle
import com.finshield.app.ui.components.StatusChip
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.SegmentTrack
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun CompleteBreakdownScreen(
    reportId: String,
    onBack: () -> Unit,
    onOpenCalculator: () -> Unit,
) {
    StatusBarIcons(darkIcons = true)
    BackHandler { onBack() }

    val report = remember(reportId) {
        AnalysisReportStore.getReport(reportId) ?: SampleReports.forId(reportId)
    }

    var selectedClauseFinding by remember { mutableStateOf<ClauseViewData?>(null) }

    val prefillAndNavigateCalculator: () -> Unit = {
        val p = report.loanSnapshot.sanctionedPrincipal
        val r = report.loanSnapshot.annualRatePercent
        val n = report.loanSnapshot.tenureMonths
        val m = report.loanSnapshot.interestMethod ?: InterestMethod.REDUCING_BALANCE
        CalculatorPrefillStore.set(CalculatorPrefill(p, r, n, m))
        onOpenCalculator()
    }

    ScreenScaffold(
        title = "Complete Breakdown",
        subtitle = report.documentName,
        onBack = onBack,
        handleNavBarInset = false,
    ) {
        // Document Banner
        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Complete Financial Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Navy,
                        )
                        Text(
                            report.documentName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    }
                    StatusChip("Audited", EmeraldTint, EmeraldDark)
                }
                Text(
                    "Itemized accounting of every fee, upfront deduction, tax rate, and scheduled repayment derived from document text and standard banking rules.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                )
            }
        }

        // Net Disbursal Highlight Card
        EstimatedNetDisbursalCard(report = report)

        // Itemized Table (Deductions 1 through 14)
        ItemizedBreakdownTableCard(report = report, onViewClause = { selectedClauseFinding = it })

        // Foreclosure & Late Payment Contingent Charges
        ContingentChargesCard(report = report)

        // Agreement vs FinShield Side-by-Side Comparison
        AgreementComparisonCard(report = report, onRecalculate = prefillAndNavigateCalculator)

        // Collapsible Raw OCR Evidence
        CollapsibleEvidenceCard(report = report)

        // Bottom CTA
        SecondaryButton(
            text = "Verify Repayment in Calculator",
            onClick = prefillAndNavigateCalculator,
            leadingIcon = Icons.Filled.Calculate,
        )

        DisclaimerText()
    }

    // Modal dialog for viewing clause text
    selectedClauseFinding?.let { clauseData ->
        ClauseDetailDialog(clauseData = clauseData, onDismiss = { selectedClauseFinding = null })
    }
}

// ---------------------------------------------------------------------------
// NET DISBURSAL HIGHLIGHT
// ---------------------------------------------------------------------------

@Composable
private fun EstimatedNetDisbursalCard(report: AnalysisReport) {
    val fb = report.financialBreakdown
    val s = report.loanSnapshot

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
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = EmeraldDark,
                        )
                        Text(
                            fb.estimatedNetDisbursal?.let { Money.inr2(it) } ?: "Needs Principal",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldDark,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "ESTIMATED NET DISBURSAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = EmeraldDark,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                        )
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
            Text(
                "Sanctioned Principal: ${s.sanctionedPrincipal?.let { Money.inr2(it) } ?: "—"}  −  Total Upfront Deductions: ${fb.totalUpfrontDeductions?.let { Money.inr2(it) } ?: "₹0.00"}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            Text(
                "Calculation Rule: Only deductions explicitly stated in the agreement to occur prior to or at disbursal are deducted. No unverified fees are subtracted.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic),
                color = TextSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ITEMIZED BREAKDOWN TABLE
// ---------------------------------------------------------------------------

@Composable
private fun ItemizedBreakdownTableCard(
    report: AnalysisReport,
    onViewClause: (ClauseViewData) -> Unit,
) {
    val fb = report.financialBreakdown
    val s = report.loanSnapshot

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Itemized Financial Schedule",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Navy,
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SegmentTrack, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("#", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextHint, modifier = Modifier.width(20.dp))
                Text("Item & Formula", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary, modifier = Modifier.weight(1f))
                Text("Rupee Amount", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary, textAlign = TextAlign.End)
            }

            // 1. Sanctioned Principal
            DetailedBreakdownRow(
                number = "1",
                name = "Sanctioned Loan Principal",
                formula = "Base approved borrowing facility",
                amount = s.sanctionedPrincipal?.let { Money.inr2(it) } ?: "Needs confirmation",
                type = ChargeType.UPFRONT,
                source = "Clause 1.1",
            )

            // 2. Processing Fee
            val procCharge = fb.itemizedCharges.firstOrNull { it.name == "Processing Fee" }
            DetailedBreakdownRow(
                number = "2",
                name = "Processing Fee",
                formula = procCharge?.formulaDescription ?: "Bank processing fee",
                amount = fb.processingFeeAmount?.let { Money.inr2(it) } ?: "Not disclosed",
                type = ChargeType.UPFRONT,
                source = procCharge?.clauseReference ?: "Clause 4.1",
                onViewClause = if (procCharge?.evidence != null) {
                    {
                        onViewClause(
                            ClauseViewData("Processing Fee", procCharge.formulaDescription, procCharge.evidence, procCharge.pageNumber, procCharge.clauseReference),
                        )
                    }
                } else null,
            )

            // 3. GST on Processing Fee
            DetailedBreakdownRow(
                number = "3",
                name = "GST on Processing Fee",
                formula = "18% statutory GST (configurable assumption)",
                amount = fb.processingFeeGst?.let { Money.inr2(it) } ?: "Not established",
                type = ChargeType.UPFRONT,
                source = "Statutory Rule",
            )

            // 4. Stamp Duty
            DetailedBreakdownRow(
                number = "4",
                name = "Stamp Duty",
                formula = "State schedule legal stamp duty",
                amount = fb.stampDuty?.let { Money.inr2(it) } ?: "Not stated",
                type = ChargeType.UPFRONT,
                source = "Schedule B",
            )

            // 5. Documentation Charges
            DetailedBreakdownRow(
                number = "5",
                name = "Documentation / Admin Charges",
                formula = "Verification and administrative charge",
                amount = fb.documentationCharges?.let { Money.inr2(it) } ?: "Not disclosed",
                type = ChargeType.UPFRONT,
                source = "Clause 4.3",
            )

            // 6. Insurance & Optional Add-ons
            DetailedBreakdownRow(
                number = "6",
                name = "Loan Protection Insurance / Add-on",
                formula = "Credit life protection insurance premium",
                amount = fb.insuranceCharges?.let { Money.inr2(it) } ?: "Not included",
                type = ChargeType.UPFRONT,
                source = "Clause 5.2",
            )

            // 7. Other Upfront Charges
            DetailedBreakdownRow(
                number = "7",
                name = "Other Upfront Deductions",
                formula = "Miscellaneous pre-disbursal charges",
                amount = fb.otherUpfrontDeductions?.let { Money.inr2(it) } ?: "₹0.00",
                type = ChargeType.UPFRONT,
                source = "Scanned clauses",
            )

            HorizontalDivider(color = DividerColor)

            // 8. Total Upfront Deductions
            SummaryHighlightRow(
                label = "Total Upfront Deductions",
                value = fb.totalUpfrontDeductions?.let { Money.inr2(it) } ?: "₹0.00",
                isNegative = true,
            )

            // 9. Estimated Net Disbursal
            SummaryHighlightRow(
                label = "Estimated Net Disbursed",
                value = fb.estimatedNetDisbursal?.let { Money.inr2(it) } ?: "—",
                isHighlight = true,
            )

            HorizontalDivider(color = DividerColor)

            // 10. Total Interest
            DetailedBreakdownRow(
                number = "10",
                name = "Total Interest Over Loan Tenure",
                formula = "Amortized at ${s.annualRatePercent ?: 0.0}% p.a. (${s.interestMethod?.label ?: "Reducing Balance"})",
                amount = fb.totalInterest?.let { Money.inr2(it) } ?: "Unable to calculate reliably",
                type = ChargeType.RECURRING,
                source = "Amortisation",
            )

            // 11. Total Scheduled EMIs
            DetailedBreakdownRow(
                number = "11",
                name = "Total Scheduled EMI Payments",
                formula = "${s.tenureMonths ?: 0} instalments × Monthly EMI",
                amount = fb.totalScheduledEmis?.let { Money.inr2(it) } ?: "—",
                type = ChargeType.RECURRING,
                source = "Amortisation Schedule",
            )

            // 12. Total Scheduled Repayment (P + I)
            SummaryHighlightRow(
                label = "Total Scheduled Repayment (P + I)",
                value = fb.totalRepaymentScheduled?.let { Money.inr2(it) } ?: "—",
            )

            // 13. Other Mandatory Payments
            DetailedBreakdownRow(
                number = "13",
                name = "Other Mandatory Payments",
                formula = "Mandatory recurring charges supported by agreement",
                amount = fb.otherMandatoryCharges?.let { Money.inr2(it) } ?: "₹0.00",
                type = ChargeType.RECURRING,
                source = "Scanned clauses",
            )

            // 14. Conditional Charges
            val foreclosureCharge = fb.itemizedCharges.firstOrNull { it.name.contains("Foreclosure", ignoreCase = true) }
            DetailedBreakdownRow(
                number = "14",
                name = "Conditional Charges (Foreclosure & Penalties)",
                formula = foreclosureCharge?.formulaDescription ?: "Foreclosure penalty & late payment fees",
                amount = "Contingent",
                type = ChargeType.CONDITIONAL,
                source = foreclosureCharge?.clauseReference ?: "Clauses 7.1 & 8.1",
            )

            HorizontalDivider(color = DividerColor)

            // Total Borrower Cost
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "TOTAL ESTIMATED BORROWER COST",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = EmeraldButton,
                )
                Text(
                    fb.totalEstimatedBorrowerCost?.let { Money.inr2(it) } ?: "—",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(
                    "Scheduled Repayments (${fb.totalRepaymentScheduled?.let { Money.inr(it) } ?: "—"}) + Upfront Deductions (${fb.totalUpfrontDeductions?.let { Money.inr(it) } ?: "₹0"}).",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun DetailedBreakdownRow(
    number: String,
    name: String,
    formula: String,
    amount: String,
    type: ChargeType,
    source: String,
    onViewClause: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                number,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = TextHint,
                modifier = Modifier.width(20.dp),
            )
            Column {
                Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                Text(formula, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(source, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic), color = TextHint)
                    if (onViewClause != null) {
                        Text(
                            "• View Quote",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = EmeraldDark,
                            modifier = Modifier.clickable(onClick = onViewClause),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.padding(start = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                amount,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
            )
            val (chipBg, chipText) = when (type) {
                ChargeType.UPFRONT -> Pair(AmberTint, AmberDark)
                ChargeType.RECURRING -> Pair(EmeraldTint, EmeraldDark)
                ChargeType.CONDITIONAL -> Pair(InfoBlueTint, InfoBlue)
                ChargeType.UNKNOWN -> Pair(SegmentTrack, TextSecondary)
            }
            Text(
                type.label.split(" ").first(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = chipText,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// CONTINGENT CHARGES & FORECLOSURE SIMULATOR
// ---------------------------------------------------------------------------

@Composable
private fun ContingentChargesCard(report: AnalysisReport) {
    val s = report.loanSnapshot
    val principal = s.sanctionedPrincipal ?: 500_000.0
    val annualRate = s.annualRatePercent ?: 12.0
    val tenureMonths = s.tenureMonths ?: 36

    var selectedScenarioMonth by remember { mutableIntStateOf(12) }

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Conditional Charges & Foreclosure Scenarios",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Navy,
            )

            // Foreclosure Scenario Calculator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "FORECLOSURE SCENARIO SIMULATOR",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Navy,
                )
                Text(
                    "Foreclosure penalties depend on the remaining principal at the time of prepayment. Select a prepayment milestone to simulate costs:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                )

                SegmentedToggle(
                    options = listOf("Month 6", "Month 12", "Month 24"),
                    selectedIndex = when (selectedScenarioMonth) {
                        6 -> 0
                        24 -> 2
                        else -> 1
                    },
                    onSelect = { idx ->
                        selectedScenarioMonth = when (idx) {
                            0 -> 6
                            2 -> 24
                            else -> 12
                        }
                    },
                )

                val balanceAtMonth = remember(principal, annualRate, tenureMonths, selectedScenarioMonth) {
                    LoanCalculator.outstandingBalanceAfter(principal, annualRate, tenureMonths, selectedScenarioMonth)
                }

                val foreclosureCharge = report.feesAndCharges.firstOrNull {
                    it.name.contains("Foreclosure", ignoreCase = true) || it.name.contains("Prepayment", ignoreCase = true)
                }
                val feePercent = foreclosureCharge?.percentage ?: 4.0
                val foreclosureCalc = remember(balanceAtMonth, feePercent) {
                    LoanCalculator.calculateForeclosure(balanceAtMonth, feePercent, 18.0)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Outstanding Balance (Month $selectedScenarioMonth)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(Money.inr2(balanceAtMonth), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Foreclosure Penalty ($feePercent%)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(Money.inr2(foreclosureCalc.feeAmount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Rose)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Statutory 18% GST", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(Money.inr2(foreclosureCalc.gstAmount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Rose)
                }
                HorizontalDivider(color = DividerColor)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Estimated Foreclosure Cost", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Navy)
                    Text(Money.inr2(foreclosureCalc.totalCost), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Rose)
                }
            }

            // Late Payment Charge Detail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "LATE PAYMENT TERMS & PENALTIES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Navy,
                )
                val statedEmi = s.statedEmi ?: 16_727.0
                val lateCalc = remember(statedEmi) {
                    LoanCalculator.calculateIllustrativeLateFee(statedEmi, 2.0, 500.0, 1)
                }
                Text(
                    "If 1 instalment (${Money.inr(statedEmi)}) is delayed by 30 days:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("2% Monthly Penal Interest", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(Money.inr2(lateCalc.penalInterest), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("NACH / Mandate Bounce Fee", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(Money.inr2(lateCalc.fixedBounceFee), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Delay Charge (Illustrative)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Navy)
                    Text(Money.inr2(lateCalc.totalLateFee), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Rose)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AGREEMENT VS FINSHIELD COMPARISON
// ---------------------------------------------------------------------------

@Composable
private fun AgreementComparisonCard(
    report: AnalysisReport,
    onRecalculate: () -> Unit,
) {
    val s = report.loanSnapshot
    val fb = report.financialBreakdown

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Agreement vs FinShield Comparison",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Navy,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SegmentTrack, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Item", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary, modifier = Modifier.weight(1.2f))
                Text("Stated in Doc", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Calculated", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            ComparisonDetailRow(
                title = "Monthly EMI",
                stated = s.statedEmi?.let { Money.inr2(it) } ?: "Not stated",
                calculated = s.calculatedEmi?.let { Money.inr2(it) } ?: "—",
            )
            ComparisonDetailRow(
                title = "Total Repayment",
                stated = s.statedTotalRepayment?.let { Money.inr2(it) } ?: "—",
                calculated = s.calculatedTotalRepayment?.let { Money.inr2(it) } ?: "—",
            )
            ComparisonDetailRow(
                title = "Upfront Fees",
                stated = fb.totalUpfrontDeductions?.let { Money.inr2(it) } ?: "₹0.00",
                calculated = fb.totalUpfrontDeductions?.let { Money.inr2(it) } ?: "₹0.00",
            )
            ComparisonDetailRow(
                title = "Total Borrower Cost",
                stated = s.statedTotalRepayment?.let { Money.inr2(it) } ?: "—",
                calculated = fb.totalEstimatedBorrowerCost?.let { Money.inr2(it) } ?: "—",
            )

            if (s.emiDifference != null && abs(s.emiDifference) >= 1.0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberTint, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Variance Analysis (${Money.inr(abs(s.emiDifference))}/month):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AmberDark,
                    )
                    Text(
                        "• Check if processing fee or insurance premium is amortized into the monthly installments\n" +
                            "• Broken-period interest charged between loan disbursal and the 1st EMI date\n" +
                            "• Different interest calculation convention (e.g. flat rate vs reducing balance)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AmberDark,
                    )
                }
            }

            SecondaryButton(
                text = "Recalculate with These Terms",
                onClick = onRecalculate,
                leadingIcon = Icons.Filled.Calculate,
            )
        }
    }
}

@Composable
private fun ComparisonDetailRow(title: String, stated: String, calculated: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, modifier = Modifier.weight(1.2f))
        Text(stated, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(calculated, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Navy, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

// ---------------------------------------------------------------------------
// COLLAPSIBLE EVIDENCE CARD
// ---------------------------------------------------------------------------

@Composable
private fun CollapsibleEvidenceCard(report: AnalysisReport) {
    var expanded by remember { mutableStateOf(false) }

    FinCard {
        Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Original Clause Quotes & Evidence",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Text(
                        "Page-by-page OCR transcript audit",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    if (report.rawPages.isEmpty()) {
                        Text("No raw OCR page transcripts available.", style = MaterialTheme.typography.bodySmall, color = TextHint)
                    } else {
                        report.rawPages.forEach { page ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppBackground, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "Page ${page.pageNumber} (${page.charCount} characters)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldDark,
                                )
                                Text(
                                    page.text.ifBlank { "[Blank page or unreadable image]" },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontStyle = FontStyle.Italic),
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

@Composable
private fun SummaryHighlightRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isNegative: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) EmeraldTint else SegmentTrack, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isHighlight) EmeraldDark else TextPrimary,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Text(
            if (isNegative && !value.startsWith("-")) "− $value" else value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isHighlight) EmeraldDark else if (isNegative) Rose else TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
        )
    }
}
