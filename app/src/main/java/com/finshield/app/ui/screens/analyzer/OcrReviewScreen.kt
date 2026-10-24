package com.finshield.app.ui.screens.analyzer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.domain.analyzer.LoanAgreementAnalyzer
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.LoanInput
import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.AnalysisReportStore
import com.finshield.app.model.SampleReports
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.IconTile
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
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.ErrorRed
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun OcrReviewScreen(
    reportId: String,
    onConfirmAndProceed: (String) -> Unit,
    onBack: () -> Unit,
) {
    StatusBarIcons(darkIcons = true)
    BackHandler { onBack() }

    val initialReport = remember(reportId) {
        AnalysisReportStore.getReport(reportId) ?: SampleReports.forId(reportId)
    }

    var principalText by remember { mutableStateOf(initialReport.loanSnapshot.sanctionedPrincipal?.toInt()?.toString() ?: "") }
    var rateText by remember { mutableStateOf(initialReport.loanSnapshot.annualRatePercent?.toString() ?: "") }
    var tenureText by remember { mutableStateOf(initialReport.loanSnapshot.tenureMonths?.toString() ?: "") }
    var statedEmiText by remember { mutableStateOf(initialReport.loanSnapshot.statedEmi?.toInt()?.toString() ?: "") }
    var procFeeText by remember { mutableStateOf(initialReport.financialBreakdown.processingFeeAmount?.toInt()?.toString() ?: "") }
    var lenderText by remember { mutableStateOf(initialReport.loanSnapshot.lenderName ?: "") }
    var borrowerText by remember { mutableStateOf(initialReport.loanSnapshot.borrowerName ?: "") }
    var selectedMethod by remember { mutableStateOf(initialReport.loanSnapshot.interestMethod ?: InterestMethod.REDUCING_BALANCE) }

    // Live parsed inputs
    val principal = principalText.toDoubleOrNull()
    val rate = rateText.toDoubleOrNull()
    val tenure = tenureText.toIntOrNull()
    val statedEmi = statedEmiText.toDoubleOrNull()
    val procFee = procFeeText.toDoubleOrNull()

    // Live recalculation
    val liveCalculatedEmi by remember(principal, rate, tenure, selectedMethod) {
        derivedStateOf {
            if (principal != null && rate != null && tenure != null && tenure > 0) {
                LoanCalculator.calculate(LoanInput(principal, rate, tenure, selectedMethod)).installment
            } else null
        }
    }

    val liveNetDisbursal by remember(principal, procFee) {
        derivedStateOf {
            if (principal != null) {
                val feeGst = (procFee ?: 0.0) * 1.18
                val otherCharges = (initialReport.financialBreakdown.stampDuty ?: 0.0) +
                    (initialReport.financialBreakdown.documentationCharges ?: 0.0) +
                    (initialReport.financialBreakdown.insuranceCharges ?: 0.0)
                principal - feeGst - otherCharges
            } else null
        }
    }

    val emiDiff = if (statedEmi != null && liveCalculatedEmi != null) statedEmi - liveCalculatedEmi!! else null

    var showFullTextDialog by remember { mutableStateOf(false) }

    if (showFullTextDialog) {
        FullDocumentTextViewerDialog(
            report = initialReport,
            onDismiss = { showFullTextDialog = false },
        )
    }

    ScreenScaffold(
        title = "Verify Extracted Terms",
        subtitle = "Review & confirm OCR findings before final report",
        onBack = onBack,
        handleNavBarInset = false,
    ) {
        // Step Indicator Badge & Document Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .background(InfoBlueTint, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Info, null, tint = InfoBlue, modifier = Modifier.size(14.dp))
                Text(
                    "Step 2 of 3: OCR Review",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = InfoBlue,
                )
            }

            Text(
                text = "${initialReport.rawPages.size} Page${if (initialReport.rawPages.size != 1) "s" else ""}",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
            )
        }

        // Full Extracted Text & Completeness CTA
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
                            text = "View Full Extracted Text",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                        )
                        Text(
                            text = "${initialReport.extractionSourceSummary} • Search & Evidence",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                StatusChip(
                    text = "${initialReport.completenessReport?.completenessPercentage ?: 0}% Identified",
                    background = EmeraldTint,
                    content = EmeraldDark,
                )
            }
        }

        // Live Calculated Preview Card
        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Live Repayment & Disbursal Preview", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Calculations update automatically as you adjust numbers below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricBox(
                        label = "EST. NET PAYOUT",
                        value = liveNetDisbursal?.let { Money.inr(it) } ?: "—",
                        subtitle = "After ₹${Money.inr((procFee ?: 0.0) * 1.18)} fees",
                        color = EmeraldDark,
                        modifier = Modifier.weight(1f),
                    )
                    MetricBox(
                        label = "CALCULATED EMI",
                        value = liveCalculatedEmi?.let { Money.inr(it) + "/mo" } ?: "—",
                        subtitle = if (emiDiff != null && abs(emiDiff) >= 50.0) {
                            val sign = if (emiDiff > 0) "+" else ""
                            "Diff: $sign${Money.inr(emiDiff)}"
                        } else "Matches standard",
                        color = if (emiDiff != null && abs(emiDiff) >= 200.0) AmberDark else EmeraldDark,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text(
            "Extracted Financial Terms",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.padding(top = 4.dp),
        )

        // 1. Sanctioned Principal
        ReviewFieldCard(
            label = "Sanctioned Principal Amount (₹)",
            icon = Icons.Filled.Payments,
            value = principalText,
            onValueChange = { principalText = it },
            keyboardType = KeyboardType.Number,
            placeholder = "e.g. 500000",
            sourcePage = initialReport.terms.firstOrNull { it.label == "Sanctioned amount" }?.pageNumber,
            evidence = initialReport.terms.firstOrNull { it.label == "Sanctioned amount" }?.evidence,
            isDetected = initialReport.loanSnapshot.sanctionedPrincipal != null,
        )

        // 2. Annual Interest Rate
        ReviewFieldCard(
            label = "Annual Interest Rate (% p.a.)",
            icon = Icons.Filled.Percent,
            value = rateText,
            onValueChange = { rateText = it },
            keyboardType = KeyboardType.Decimal,
            placeholder = "e.g. 12.5",
            sourcePage = initialReport.terms.firstOrNull { it.label == "Interest rate" }?.pageNumber,
            evidence = initialReport.terms.firstOrNull { it.label == "Interest rate" }?.evidence,
            isDetected = initialReport.loanSnapshot.annualRatePercent != null,
        )

        // 3. Loan Tenure
        ReviewFieldCard(
            label = "Loan Tenure (Months)",
            icon = Icons.Filled.CalendarMonth,
            value = tenureText,
            onValueChange = { tenureText = it },
            keyboardType = KeyboardType.Number,
            placeholder = "e.g. 36",
            sourcePage = initialReport.terms.firstOrNull { it.label == "Tenure" }?.pageNumber,
            evidence = initialReport.terms.firstOrNull { it.label == "Tenure" }?.evidence,
            isDetected = initialReport.loanSnapshot.tenureMonths != null,
        )

        // 4. Stated EMI
        ReviewFieldCard(
            label = "Stated Agreement EMI (₹/month)",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            value = statedEmiText,
            onValueChange = { statedEmiText = it },
            keyboardType = KeyboardType.Number,
            placeholder = "Contractual EMI stated on agreement",
            sourcePage = initialReport.terms.firstOrNull { it.label == "Stated EMI" }?.pageNumber,
            evidence = initialReport.terms.firstOrNull { it.label == "Stated EMI" }?.evidence,
            isDetected = initialReport.loanSnapshot.statedEmi != null,
        )

        // 5. Processing Fee
        ReviewFieldCard(
            label = "Base Processing Fee Amount (₹ excl. GST)",
            icon = Icons.Filled.Payments,
            value = procFeeText,
            onValueChange = { procFeeText = it },
            keyboardType = KeyboardType.Number,
            placeholder = "e.g. 7500",
            sourcePage = initialReport.feesAndPenalties.firstOrNull { it.label == "Processing fee" }?.pageNumber,
            evidence = initialReport.feesAndPenalties.firstOrNull { it.label == "Processing fee" }?.evidence,
            isDetected = initialReport.financialBreakdown.processingFeeAmount != null,
        )

        // 6. Lender & Borrower
        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Parties & Reference", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                OutlinedTextField(
                    value = lenderText,
                    onValueChange = { lenderText = it },
                    label = { Text("Lender / Financial Institution") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldDark,
                        unfocusedBorderColor = DividerColor,
                    ),
                )

                OutlinedTextField(
                    value = borrowerText,
                    onValueChange = { borrowerText = it },
                    label = { Text("Borrower Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldDark,
                        unfocusedBorderColor = DividerColor,
                    ),
                )
            }
        }

        // Actions
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = "Confirm & View Full Report",
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = {
                    val updated = LoanAgreementAnalyzer.recalculateWithCustomTerms(
                        originalReport = initialReport,
                        principal = principal,
                        annualRatePercent = rate,
                        tenureMonths = tenure,
                        statedEmi = statedEmi,
                        interestMethod = selectedMethod,
                        processingFeeAmount = procFee,
                        lenderName = lenderText.ifBlank { null },
                        borrowerName = borrowerText.ifBlank { null },
                    )
                    AnalysisReportStore.saveReport(reportId, updated)
                    onConfirmAndProceed(reportId)
                },
            )

            SecondaryButton(
                text = "Reset to Original OCR Extraction",
                leadingIcon = Icons.Filled.Restore,
                onClick = {
                    principalText = initialReport.loanSnapshot.sanctionedPrincipal?.toInt()?.toString() ?: ""
                    rateText = initialReport.loanSnapshot.annualRatePercent?.toString() ?: ""
                    tenureText = initialReport.loanSnapshot.tenureMonths?.toString() ?: ""
                    statedEmiText = initialReport.loanSnapshot.statedEmi?.toInt()?.toString() ?: ""
                    procFeeText = initialReport.financialBreakdown.processingFeeAmount?.toInt()?.toString() ?: ""
                    lenderText = initialReport.loanSnapshot.lenderName ?: ""
                    borrowerText = initialReport.loanSnapshot.borrowerName ?: ""
                    selectedMethod = initialReport.loanSnapshot.interestMethod ?: InterestMethod.REDUCING_BALANCE
                },
            )
        }

        DisclaimerText()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(AppBackground, RoundedCornerShape(14.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
        }
    }
}

@Composable
private fun ReviewFieldCard(
    label: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    placeholder: String,
    sourcePage: Int?,
    evidence: String?,
    isDetected: Boolean,
) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconTile(icon, EmeraldDark, EmeraldTint, size = 32)
                    Text(label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                }

                if (isDetected) {
                    StatusChip("Page ${sourcePage ?: 1}", EmeraldTint, EmeraldDark)
                } else {
                    StatusChip("Needs Input", AmberTint, AmberDark)
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldDark,
                    unfocusedBorderColor = DividerColor,
                ),
            )

            if (!evidence.isNullOrBlank()) {
                Text(
                    "OCR snippet: \"$evidence\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                    maxLines = 2,
                )
            }
        }
    }
}
