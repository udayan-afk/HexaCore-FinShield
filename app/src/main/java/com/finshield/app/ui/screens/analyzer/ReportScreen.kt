package com.finshield.app.ui.screens.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.domain.calculator.LoanCalculator
import com.finshield.app.domain.calculator.LoanInput
import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.EvidenceKind
import com.finshield.app.model.Finding
import com.finshield.app.model.SampleReports
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.KeyValueRow
import com.finshield.app.ui.components.SampleNotice
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.StatusChip
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.Amber
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.SegmentTrack
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun ReportScreen(reportId: String, onBack: () -> Unit, onOpenCalculator: () -> Unit) {
    StatusBarIcons(darkIcons = true)
    val report = remember(reportId) {
        com.finshield.app.model.AnalysisReportStore.getReport(reportId) ?: SampleReports.forId(reportId)
    }

    ScreenScaffold(title = "Analysis report", subtitle = report.documentName, onBack = onBack) {
        if (report.isSample) {
            SampleNotice(
                "Sample data",
                "This report shows the layout using illustrative content. Nothing here was extracted from a real document.",
            )
        }

        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(report.documentName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("${report.manualReview.size} to review", AmberTint, AmberDark)
                    if (report.isSample) StatusChip("Sample", SegmentTrack, TextSecondary)
                }
            }
        }

        Section("Key terms", report.terms)
        IndependentCheck(report, onOpenCalculator)
        Section("Fees and penalty clauses", report.feesAndPenalties)

        BulletCard("Missing or ambiguous information", report.missingInformation, "Nothing flagged as missing.")
        BulletCard("Items requiring manual review", report.manualReview, "Nothing flagged for manual review.")

        FinCard {
            Text(
                "Findings marked \"Explicit\" quote text from the document. \"Heuristic\" findings are pattern matches and need your confirmation. " +
                    "This is not a full legal review.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        DisclaimerText()
    }
}

@Composable
private fun Section(title: String, findings: List<Finding>) {
    FinCard {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            findings.forEachIndexed { i, f ->
                if (i > 0) HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
                else Spacer(Modifier.height(10.dp))
                FindingRow(f)
            }
        }
    }
}

@Composable
private fun FindingRow(f: Finding) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(f.label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
            when (f.kind) {
                EvidenceKind.EXPLICIT -> StatusChip("Explicit", EmeraldTint, EmeraldDark)
                EvidenceKind.HEURISTIC -> StatusChip("Heuristic", AmberTint, AmberDark)
                EvidenceKind.NOT_FOUND -> StatusChip("Not found", SegmentTrack, TextSecondary)
            }
        }
        Text(f.value ?: "Unknown", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        f.evidence?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, fontSize = 12.sp),
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth().background(AppBackground, RoundedCornerShape(10.dp)).padding(10.dp),
            )
        }
    }
}

@Composable
private fun IndependentCheck(report: AnalysisReport, onOpenCalculator: () -> Unit) {
    val p = report.principal
    val r = report.annualRatePercent
    val n = report.tenureMonths
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Independent repayment check", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (p != null && r != null && n != null) {
                val calc = remember(p, r, n) { LoanCalculator.calculate(LoanInput(p, r, n)) }
                KeyValueRow("Calculated EMI", Money.inr(calc.installment), emphasize = true)
                report.statedEmi?.let { stated ->
                    KeyValueRow("EMI stated in document", Money.inr(stated))
                    val diff = stated - calc.installment
                    Text(
                        if (abs(diff) < 1) "The stated EMI matches the calculation."
                        else "The stated EMI is ${Money.inr(abs(diff))} ${if (diff > 0) "higher" else "lower"} per month than the calculation. " +
                            "Fees, rounding or a different method may explain this.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (abs(diff) < 1) EmeraldDark else AmberDark,
                    )
                }
                KeyValueRow("Total repayment", Money.inr(calc.totalRepayment))
                KeyValueRow("Total interest", Money.inr(calc.totalInterest))
            } else {
                Text(
                    "Not enough valid values (amount, rate and tenure) to calculate an EMI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Open loan calculator", onOpenCalculator, leadingIcon = Icons.Filled.Calculate)
        }
    }
}

@Composable
private fun BulletCard(title: String, items: List<String>, emptyText: String) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (items.isEmpty()) Text(emptyText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            items.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, color = TextPrimary) }
        }
    }
}
