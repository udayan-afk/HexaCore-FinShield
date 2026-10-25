package com.finshield.app.ui.screens.credit

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.domain.calculator.Money
import com.finshield.app.domain.credit.AAConnectionStatus
import com.finshield.app.domain.credit.CreditScoreBand
import com.finshield.app.domain.credit.CreditScoreStatus
import com.finshield.app.domain.credit.EligibilityStatus
import com.finshield.app.domain.credit.EmploymentType
import com.finshield.app.domain.credit.LoanEligibilityResult
import com.finshield.app.ui.components.CreditScoreGauge
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.FinTextField
import com.finshield.app.ui.components.PrimaryButton
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.SegmentedToggle
import com.finshield.app.ui.components.StatusChip
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.Amber
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.Emerald
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.ErrorRed
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.NavyLight
import com.finshield.app.ui.theme.Outline
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.RoseTint
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

@Composable
fun CreditHealthScreen(
    onBack: () -> Unit,
    vm: CreditHealthViewModel = viewModel(),
) {
    StatusBarIcons(darkIcons = true)
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissSnackbar()
        }
    }

    ScreenScaffold(
        title = "Credit Health",
        subtitle = "Bureau score & borrowing capacity",
        onBack = onBack,
    ) {
        // Section A: CIBIL Credit Score Card
        CreditScoreSection(
            state = state,
            onCheckScore = vm::openConsentDialog,
            onLoadSandbox = vm::loadSandboxTestData,
        )

        // Section B: Connect Financial Accounts (RBI Account Aggregator)
        AccountAggregatorSection(
            state = state,
            onConnect = vm::openAaSetupInfo,
            onDisconnect = vm::disconnectAccountAggregator,
        )

        // Section C: Interactive Loan Eligibility Calculator
        LoanEligibilitySection(
            state = state,
            onIncomeChange = vm::onIncomeChange,
            onExistingEmiChange = vm::onExistingEmiChange,
            onLoanAmountChange = vm::onLoanAmountChange,
            onTenureChange = vm::onTenureChange,
            onRateChange = vm::onRateChange,
            onTenureUnitToggle = vm::onTenureUnitToggle,
            onEmploymentTypeSelect = vm::onEmploymentTypeSelect,
        )

        // Section D: Improve Your CIBIL Score (5 Practical Tips)
        ImproveScoreSection()

        // Section E: Privacy & Security Commitment
        PrivacyCommitmentSection()

        DisclaimerText()
    }

    // Modal: CIBIL Partner Setup Information
    if (state.showCibilSetupInfo) {
        CibilIntegrationRequiredDialog(
            onDismiss = vm::dismissCibilSetupInfo,
            onTrySandbox = {
                vm.dismissCibilSetupInfo()
                vm.loadSandboxTestData()
            },
        )
    }

    // Modal: Account Aggregator Setup Information
    if (state.showAaSetupInfo) {
        AccountAggregatorSetupDialog(
            onDismiss = vm::dismissAaSetupInfo,
        )
    }

    // Modal: Identity & Consent Bottom Sheet for CIBIL inquiry
    if (state.showConsentDialog) {
        CibilConsentDialog(
            onDismiss = vm::dismissConsentDialog,
            onSubmit = vm::submitCibilInquiry,
        )
    }
}

/**
 * Section A: Credit score display with Gauge, brackets, source attribution, and actions.
 */
@Composable
private fun CreditScoreSection(
    state: CreditHealthUiState,
    onCheckScore: () -> Unit,
    onLoadSandbox: () -> Unit,
) {
    val cred = state.creditHealthState.creditScore
    val isVerified = cred.score != null && cred.status == CreditScoreStatus.VERIFIED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Navy.copy(alpha = 0.1f), spotColor = Navy.copy(alpha = 0.15f))
            .background(Navy, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = EmeraldSoft,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "CIBIL CREDIT SCORE",
                    color = EmeraldSoft,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            }

            if (isVerified) {
                StatusChip(
                    text = if (cred.isOfficialBureauScore) "OFFICIAL BUREAU" else "SANDBOX VERIFIED",
                    background = if (cred.isOfficialBureauScore) Emerald.copy(alpha = 0.25f) else Amber.copy(alpha = 0.25f),
                    content = if (cred.isOfficialBureauScore) EmeraldSoft else Amber,
                )
            }
        }

        // Main Score visualization
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CreditScoreGauge(
                score = cred.score,
                size = 130.dp,
                strokeWidth = 12.dp,
                isDarkTheme = true,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isVerified) {
                    Text(
                        text = "${cred.score} / ${cred.maxRange}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    cred.band?.let { band ->
                        Text(
                            text = "${band.label} Credit Profile",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = band.badgeContent,
                        )
                        Text(
                            text = band.summary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    if (!cred.retrievalDate.isNullOrBlank()) {
                        Text(
                            text = "Source: ${cred.provider} (${cred.retrievalDate})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    Text(
                        text = "Official Score Not Fetched",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        text = "Connect with authorized TransUnion CIBIL verification to view your official score and score range.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Text(
                        text = "Standard Range: 300 – 900",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = EmeraldSoft,
                    )
                }
            }
        }

        // Range Legend
        ScoreRangeLegend()

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(EmeraldButton, RoundedCornerShape(14.dp))
                    .clickable(onClick = onCheckScore),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (isVerified) Icons.Filled.Refresh else Icons.Filled.Verified,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isVerified) "Refresh CIBIL Score" else "Check CIBIL Score",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            if (!isVerified) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onLoadSandbox)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Sandbox Test",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    )
                }
            }
        }
    }
}

/**
 * Score Range Legend according to TransUnion CIBIL standards.
 */
@Composable
private fun ScoreRangeLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "TRANSUNION CIBIL SCORE BRACKETS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.5f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RangeItem("Poor", "<550", Rose)
            RangeItem("Average", "550-649", Amber)
            RangeItem("Good", "650-749", InfoBlue)
            RangeItem("Excellent", "750-900", Emerald)
        }
    }
}

@Composable
private fun RangeItem(label: String, range: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(7.dp).background(color, RoundedCornerShape(50)))
            Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
        }
        Text(range, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
    }
}

/**
 * Section B: Connect Financial Accounts (RBI Account Aggregator)
 */
@Composable
private fun AccountAggregatorSection(
    state: CreditHealthUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val aa = state.creditHealthState.accountAggregator
    val isConnected = aa.status == AAConnectionStatus.CONNECTED

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(InfoBlueTint, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AccountBalance, null, tint = InfoBlue, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = "Connect Bank Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                        )
                        Text(
                            text = "RBI Account Aggregator Framework",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                        )
                    }
                }

                StatusChip(
                    text = if (isConnected) "CONNECTED" else "NOT CONNECTED",
                    background = if (isConnected) EmeraldTint else Color(0xFFF1F5F9),
                    content = if (isConnected) EmeraldDark else TextSecondary,
                )
            }

            Text(
                text = "Securely verify your cash flow using the RBI-regulated Account Aggregator ecosystem. FinShield never sees, requests, or stores your internet banking passwords, UPI PINs, ATM PINs, or OTPs.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            // Transparency box: What data is requested and why
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Lock, null, tint = EmeraldDark, modifier = Modifier.size(14.dp))
                    Text("Consent Scope & Purpose", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
                Text("• Purpose: Cash flow analysis to verify repayment capacity for loan eligibility.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
                Text("• Data Scope: Read-only transaction summary (last 6 months) and bank account status.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
                Text("• Important: Bank account linking is not equivalent to obtaining a CIBIL score.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
            }

            if (isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Linked Bank: ${aa.connectedBankName ?: "Primary Account"}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                        )
                        Text(
                            text = "Last synced: ${aa.lastSyncedTime ?: "Recently"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                        )
                    }
                    TextButton(onClick = onDisconnect) {
                        Text("Revoke Consent / Disconnect", color = Rose, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(CardWhite, RoundedCornerShape(14.dp))
                        .border(1.dp, Outline, RoundedCornerShape(14.dp))
                        .clickable(onClick = onConnect),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.AccountBalance, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Connect via Account Aggregator", color = TextPrimary, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/**
 * Section C: Interactive Loan Eligibility Calculator
 */
@Composable
private fun LoanEligibilitySection(
    state: CreditHealthUiState,
    onIncomeChange: (String) -> Unit,
    onExistingEmiChange: (String) -> Unit,
    onLoanAmountChange: (String) -> Unit,
    onTenureChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onTenureUnitToggle: (Boolean) -> Unit,
    onEmploymentTypeSelect: (EmploymentType) -> Unit,
) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(EmeraldTint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Calculate, null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        text = "Loan Eligibility Calculator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Text(
                        text = "Evaluate your debt-to-income (FOIR) capacity",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
            }

            // Inputs
            FinTextField(
                label = "Monthly Net In-Hand Income",
                value = state.monthlyIncomeStr,
                onValueChange = onIncomeChange,
                placeholder = "75000",
                prefix = "₹ ",
                keyboardType = KeyboardType.Number,
            )

            FinTextField(
                label = "Existing Monthly EMI Obligations",
                value = state.existingEmiStr,
                onValueChange = onExistingEmiChange,
                placeholder = "12000",
                prefix = "₹ ",
                keyboardType = KeyboardType.Number,
            )

            // Employment Type
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Employment Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                SegmentedToggle(
                    options = EmploymentType.entries.map {
                        when (it) {
                            EmploymentType.SALARIED -> "Salaried"
                            EmploymentType.SELF_EMPLOYED_BUSINESS -> "Business"
                            EmploymentType.SELF_EMPLOYED_PROFESSIONAL -> "Professional"
                        }
                    },
                    selectedIndex = state.employmentType.ordinal,
                    onSelect = { onEmploymentTypeSelect(EmploymentType.entries[it]) },
                )
            }

            FinTextField(
                label = "Requested Loan Amount",
                value = state.loanAmountStr,
                onValueChange = onLoanAmountChange,
                placeholder = "500000",
                prefix = "₹ ",
                keyboardType = KeyboardType.Number,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinTextField(
                    label = "Loan Tenure",
                    value = state.tenureMonthsStr,
                    onValueChange = onTenureChange,
                    placeholder = if (state.tenureInYears) "3" else "36",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                FinTextField(
                    label = "Expected Interest Rate (p.a.)",
                    value = state.interestRateStr,
                    onValueChange = onRateChange,
                    placeholder = "10.5",
                    suffix = "%",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }

            SegmentedToggle(
                options = listOf("Tenure in Months", "Tenure in Years"),
                selectedIndex = if (state.tenureInYears) 1 else 0,
                onSelect = { onTenureUnitToggle(it == 1) },
            )

            // Output Card
            EligibilityResultCard(state.eligibilityResult)
        }
    }
}

/**
 * Breakdown and Result Card for Loan Eligibility.
 */
@Composable
private fun EligibilityResultCard(res: LoanEligibilityResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "ELIGIBILITY ASSESSMENT",
                color = EmeraldSoft,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
            StatusChip(
                text = res.status.label,
                background = res.status.badgeBackground,
                content = res.status.badgeContent,
            )
        }

        // Summary Metric Highlight
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Estimated New EMI",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = Money.inr(res.estimatedNewEmi),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Estimated FOIR (DTI)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = "${res.foirPercent.toInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (res.foirPercent <= 45.0) EmeraldSoft else if (res.foirPercent <= 60.0) Amber else Rose,
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

        // Breakdown lines
        MetricRow("Monthly Net Income", Money.inr(res.monthlyIncome))
        MetricRow("Existing Monthly EMI", Money.inr(res.existingMonthlyEmi))
        MetricRow("Total Monthly Obligation", Money.inr(res.totalMonthlyEmiObligation))
        MetricRow("Remaining Monthly Capacity", Money.inr(res.remainingMonthlyCapacity))

        // Explanation text
        Text(
            text = res.summaryExplanation,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Color.White.copy(alpha = 0.85f),
        )

        // Reducing-balance formula indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Info, null, tint = EmeraldSoft, modifier = Modifier.size(13.dp))
            Text(
                text = "Calculated using standard reducing-balance formula: EMI = P × r × (1+r)ⁿ / ((1+r)ⁿ − 1)",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        // Factors influencing lender decision
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Key Factors Lenders Evaluate:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            res.keyDecisionFactors.forEach { factor ->
                Text(
                    text = "• $factor",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

/**
 * Section D: Improve Your CIBIL Score Educational Tips
 */
@Composable
private fun ImproveScoreSection() {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(EmeraldTint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        text = "Improve Your CIBIL Score",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Text(
                        text = "Actionable habits to maintain a 750+ score",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
            }

            TipItem(
                number = "1",
                icon = Icons.Filled.CheckCircle,
                title = "Pay EMIs and Credit Card Bills on Time",
                description = "Payment history accounts for approximately 35% of your credit score. Set up autopay to never miss a due date.",
            )

            TipItem(
                number = "2",
                icon = Icons.Filled.CreditCard,
                title = "Keep Credit Card Utilization Low",
                description = "Try to use less than 30% of your total card limits. High utilization signals debt stress to bureau algorithms.",
            )

            TipItem(
                number = "3",
                icon = Icons.Filled.Speed,
                title = "Avoid Applying for Multiple Loans at Once",
                description = "Every formal application triggers a hard inquiry. Multiple inquiries in a short window can lower your score.",
            )

            TipItem(
                number = "4",
                icon = Icons.Filled.Warning,
                title = "Review Your Credit Report & Dispute Errors",
                description = "Check your report regularly for clerical errors, wrong balances, or accounts you didn't open, and raise a dispute on CIBIL.",
            )

            TipItem(
                number = "5",
                icon = Icons.Filled.History,
                title = "Maintain a Healthy Credit Mix & Long History",
                description = "A combination of secured loans (home/car) and unsecured lines (cards), along with seasoned accounts, builds trust.",
            )
        }
    }
}

@Composable
private fun TipItem(number: String, icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(EmeraldTint, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = EmeraldDark,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary,
            )
        }
    }
}

/**
 * Section E: Privacy & Security Commitment
 */
@Composable
private fun PrivacyCommitmentSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmeraldTint, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Shield, null, tint = EmeraldDark, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "USER PRIVACY & REGULATORY ISOLATION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = EmeraldDark,
            )
            Text(
                text = "FinShield never logs personal identity documents, credentials, or bank passwords. All financial inquiries are isolated to your authenticated account with end-to-end authorization.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextPrimary,
            )
        }
    }
}

/**
 * Dialog: CIBIL Partner Integration Setup Explanation
 */
@Composable
private fun CibilIntegrationRequiredDialog(
    onDismiss: () -> Unit,
    onTrySandbox: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Security, null, tint = InfoBlue)
                Text("CIBIL Integration Required", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Official live TransUnion CIBIL score checks require authorized institutional partner credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Production Onboarding Requirements:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text("1. Licensed Financial Entity (RBI-regulated NBFC / Bank / Registered Fintech Partner).", style = MaterialTheme.typography.bodySmall)
                Text("2. Secure server backend proxy with client mutual TLS (mTLS) to protect secrets off mobile clients.", style = MaterialTheme.typography.bodySmall)
                Text("3. CIR Regulations 2006 compliance for explicit consumer consent recording.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "You can test the full verification flow using sandbox test data in this environment.",
                    style = MaterialTheme.typography.bodySmall.copy(color = EmeraldDark, fontWeight = FontWeight.SemiBold),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onTrySandbox) {
                Text("Load Sandbox Data", color = EmeraldDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
    )
}

/**
 * Dialog: Account Aggregator Setup Explanation
 */
@Composable
private fun AccountAggregatorSetupDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AccountBalance, null, tint = InfoBlue)
                Text("Account Aggregator Setup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "The RBI Account Aggregator (AA) framework allows consumers to share financial data with registered Financial Information Users (FIUs) without revealing login credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Integration Status: Setup Required",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AmberDark),
                )
                Text("• FIU backend connector (e.g. Setu, Finvu, or Anumati) must be provisioned on your cloud service.", style = MaterialTheme.typography.bodySmall)
                Text("• FinShield does not collect or store bank passwords or OTPs in homemade forms.", style = MaterialTheme.typography.bodySmall)
                Text("• When configured, the user is redirected to the licensed AA portal for verified consent.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood", color = EmeraldDark, fontWeight = FontWeight.Bold)
            }
        },
    )
}

/**
 * Dialog: Consumer Consent & Identity Verification for CIBIL inquiry
 */
@Composable
private fun CibilConsentDialog(
    onDismiss: () -> Unit,
    onSubmit: (pan: String, dob: String, name: String, consent: Boolean) -> Unit,
) {
    var pan by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var consentGranted by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("CIBIL Score Verification", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Under the Credit Information Companies (Regulation) Act, 2005, your explicit consent and identity are required to retrieve your CIBIL score.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                FinTextField(
                    label = "Full Name (as per PAN)",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Rahul Sharma",
                )

                FinTextField(
                    label = "PAN Number",
                    value = pan,
                    onValueChange = { pan = it.uppercase() },
                    placeholder = "ABCDE1234F",
                )

                FinTextField(
                    label = "Date of Birth (DD-MM-YYYY)",
                    value = dob,
                    onValueChange = { dob = it },
                    placeholder = "15-08-1990",
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { consentGranted = !consentGranted },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = consentGranted,
                        onCheckedChange = { consentGranted = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldDark),
                    )
                    Text(
                        text = "I hereby appoint FinShield and its authorized bureau partner to pull my credit information report from TransUnion CIBIL for credit health evaluation.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextPrimary,
                    )
                }

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Authorize & Check",
                onClick = {
                    if (name.isBlank() || pan.length < 10 || dob.isBlank()) {
                        errorText = "Please enter your full name, 10-digit PAN, and date of birth."
                    } else if (!consentGranted) {
                        errorText = "Explicit consent is required to request credit bureau information."
                    } else {
                        onSubmit(pan, dob, name, consentGranted)
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}
