package com.finshield.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.Money
import com.finshield.app.domain.calculator.RepaymentFrequency
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.FinTextField
import com.finshield.app.ui.components.PrimaryButton
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SegmentedToggle
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.Amber
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

@Composable
fun CalculatorScreen(onBack: () -> Unit, vm: CalculatorViewModel = viewModel()) {
    StatusBarIcons(darkIcons = true)
    val s by vm.state.collectAsState()

    ScreenScaffold(title = "Loan calculator", subtitle = "Independent repayment check", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Verify your repayment", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Text(
                "Use the same terms as your agreement to independently estimate the cost.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FinTextField(
                    label = "Principal amount",
                    value = s.principal,
                    onValueChange = vm::onPrincipal,
                    placeholder = "500000",
                    prefix = "₹ ",
                    error = s.errors.principal,
                    keyboardType = KeyboardType.Decimal,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinTextField(
                        label = "Annual interest rate",
                        value = s.rate,
                        onValueChange = vm::onRate,
                        placeholder = "12.5",
                        suffix = "%",
                        error = s.errors.rate,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    FinTextField(
                        label = "Loan tenure",
                        value = s.tenure,
                        onValueChange = vm::onTenure,
                        placeholder = "36",
                        error = s.errors.tenure,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
                LabelledToggle("Tenure in", listOf("Months", "Years"), if (s.tenureInYears) 1 else 0) { vm.onUnit(it == 1) }
                LabelledToggle(
                    "Interest calculation method",
                    InterestMethod.entries.map { it.label },
                    s.method.ordinal,
                ) { vm.onMethod(InterestMethod.entries[it]) }
                LabelledToggle(
                    "Repayment frequency",
                    RepaymentFrequency.entries.map { it.label },
                    s.frequency.ordinal,
                ) { vm.onFrequency(RepaymentFrequency.entries[it]) }
                Spacer(Modifier.height(2.dp))
                PrimaryButton("Calculate Repayment", onClick = vm::calculate)
            }
        }

        val result = s.result
        if (result != null) {
            Column(
                Modifier.fillMaxWidth().background(Navy, RoundedCornerShape(24.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("ESTIMATED RESULTS", color = EmeraldSoft, style = MaterialTheme.typography.labelSmall)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${s.resultFrequency.installmentNoun} installment", color = Color70, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(Money.inr(result.installment), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f))
                ResultLine("Number of installments", result.numberOfInstallments.toString())
                ResultLine("Total interest", Money.inr(result.totalInterest))
                ResultLine("Total repayment", Money.inr(result.totalRepayment))
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth().height(10.dp).background(androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(50))) {
                    Box(Modifier.weight(result.principalShare.toFloat().coerceAtLeast(0.001f)).height(10.dp).background(EmeraldButton, RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)))
                    Box(Modifier.weight(result.interestShare.toFloat().coerceAtLeast(0.001f)).height(10.dp).background(Amber, RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Legend(EmeraldButton, "Principal ${Money.percent(result.principalShare)}")
                    Legend(Amber, "Interest ${Money.percent(result.interestShare)}")
                }
                Text(
                    if (s.resultMethod == InterestMethod.FLAT)
                        "Flat rate: interest is charged on the full principal for the whole tenure."
                    else
                        "Reducing balance: interest is charged on the outstanding principal.",
                    color = Color70,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Text(
            "Results exclude processing fees, insurance, taxes and other charges, so your lender's figures may differ.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        DisclaimerText()
    }
}

private val Color70 = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)

@Composable
private fun LabelledToggle(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        SegmentedToggle(options, selected, onSelect)
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
        Text(text, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
    }
}
