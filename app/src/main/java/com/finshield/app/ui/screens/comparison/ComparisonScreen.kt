package com.finshield.app.ui.screens.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.domain.calculator.Money
import com.finshield.app.domain.comparison.LoanComparison
import com.finshield.app.domain.comparison.OfferEvaluation
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.FinTextField
import com.finshield.app.ui.components.KeyValueRow
import com.finshield.app.ui.components.SampleNotice
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.StatusChip
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.ErrorRed
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

@Composable
fun ComparisonScreen(vm: ComparisonViewModel = viewModel()) {
    StatusBarIcons(darkIcons = true)
    val s by vm.state.collectAsState()

    ScreenScaffold(title = "Compare loans", subtitle = "Same amount, same tenure", onBack = null, handleNavBarInset = false) {
        SampleNotice(
            "Illustrative sample offers",
            "These offers are examples, not real lenders or live rates. Replace them with the terms from your own offers.",
        )

        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Shared terms", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinTextField(
                        "Principal amount", s.principal, vm::onPrincipal, "500000",
                        prefix = "₹ ", error = s.result.principalError, keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1.4f),
                    )
                    FinTextField(
                        "Tenure", s.tenureMonths, vm::onTenure, "36",
                        suffix = "mo", error = s.result.tenureError, keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        s.result.offers.forEachIndexed { index, eval ->
            OfferCard(
                index = index,
                eval = eval,
                isBest = eval.draft.id == s.result.lowestCostId,
                canRemove = s.offers.size > LoanComparison.MIN_OFFERS,
                onName = { vm.onName(eval.draft.id, it) },
                onRate = { vm.onRate(eval.draft.id, it) },
                onFee = { vm.onFee(eval.draft.id, it) },
                onRemove = { vm.removeOffer(eval.draft.id) },
            )
        }

        val atMax = s.offers.size >= LoanComparison.MAX_OFFERS
        SecondaryButton(
            if (atMax) "Maximum ${LoanComparison.MAX_OFFERS} offers" else "Add another offer",
            onClick = vm::addOffer,
            leadingIcon = Icons.Filled.Add,
            enabled = !atMax,
        )

        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Assumptions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                listOf(
                    "All offers use the same principal and tenure entered above.",
                    "Monthly reducing-balance EMI; no promotional or floating rates.",
                    "Total cost = total repayment + the known fee you enter. Taxes, insurance and other charges are not included.",
                    "\"Lowest total cost\" only reflects the numbers you typed. It is not a recommendation of any lender.",
                ).forEach { Text("•  $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
            }
        }
        DisclaimerText()
    }
}

@Composable
private fun OfferCard(
    index: Int,
    eval: OfferEvaluation,
    isBest: Boolean,
    canRemove: Boolean,
    onName: (String) -> Unit,
    onRate: (String) -> Unit,
    onFee: (String) -> Unit,
    onRemove: () -> Unit,
) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Offer ${index + 1}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (isBest) StatusChip("Lowest total cost", EmeraldTint, EmeraldDark)
                }
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove offer ${index + 1}", tint = ErrorRed)
                    }
                }
            }
            FinTextField("Lender / offer name", eval.draft.name, onName, "e.g. Offer name")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FinTextField(
                    "Annual rate", eval.draft.rateText, onRate, "10.5",
                    suffix = "%", error = eval.rateError, keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                FinTextField(
                    "Known fees", eval.draft.feeText, onFee, "0",
                    prefix = "₹ ", error = eval.feeError, keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
            val r = eval.result
            if (r != null) {
                HorizontalDivider(color = com.finshield.app.ui.theme.DividerColor)
                KeyValueRow("Monthly EMI", Money.inr(r.installment), emphasize = true)
                KeyValueRow("Total repayment", Money.inr(r.totalRepayment))
                KeyValueRow("Total interest", Money.inr(r.totalInterest))
                KeyValueRow("Known fees", Money.inr(eval.knownFee))
                eval.totalCost?.let { KeyValueRow("Total cost incl. fees", Money.inr(it), emphasize = true) }
            } else {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Enter valid shared terms and a rate to see results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}
