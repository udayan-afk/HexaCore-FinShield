package com.finshield.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.data.auth.Session
import com.finshield.app.data.auth.greetingName
import com.finshield.app.data.history.HistoryState
import com.finshield.app.ui.components.EmptyState
import com.finshield.app.ui.components.ErrorState
import com.finshield.app.ui.components.IconTile
import com.finshield.app.ui.components.LoadingBlock
import com.finshield.app.ui.components.SectionHeader
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.screens.history.AnalysisRow
import com.finshield.app.ui.screens.history.HistoryViewModel
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.Amber
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.NavyLight
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun DashboardScreen(
    session: Session,
    onAnalyze: () -> Unit,
    onCalculator: () -> Unit,
    onCompare: () -> Unit,
    onHistory: () -> Unit,
    onOpenReport: (String) -> Unit,
    onProfile: () -> Unit,
    onCreditHealth: () -> Unit = {},
    vm: HistoryViewModel = viewModel(),
) {
    StatusBarIcons(darkIcons = true)
    val history by vm.state.collectAsState()
    val creditHealthState by com.finshield.app.data.AppContainer.creditHealth.state.collectAsState()
    val name = session.greetingName()
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "GOOD MORNING"
            in 12..16 -> "GOOD AFTERNOON"
            else -> "GOOD EVENING"
        }
    }

    Column(Modifier.fillMaxSize().background(AppBackground).verticalScroll(rememberScrollState())) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(CardWhite).statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(greeting, color = EmeraldDark, style = MaterialTheme.typography.labelSmall)
                Text("Hi, $name", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Let's make your next loan clearer.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Box(
                Modifier.size(48.dp).background(EmeraldTint, RoundedCornerShape(50)).clickable(onClick = onProfile),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials(name), color = Navy, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            HeroCard(onAnalyze)

            CreditHealthCard(
                creditScoreData = creditHealthState.creditScore,
                onCheckScore = onCreditHealth,
                onCheckEligibility = onCreditHealth,
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quick actions", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(Icons.Filled.Calculate, EmeraldDark, EmeraldTint, "Loan\nCalculator", "Verify your EMI", onCalculator)
                    QuickAction(Icons.Filled.SwapHoriz, InfoBlue, InfoBlueTint, "Compare\nLoans", "Find better value", onCompare)
                    QuickAction(Icons.Filled.History, Amber, AmberTint, "Analysis\nHistory", "Past reviews", onHistory)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Recent analyses", action = "View all", onAction = onHistory)
                when (val h = history) {
                    HistoryState.Loading -> LoadingBlock()
                    is HistoryState.Error -> ErrorState(h.message, onRetry = vm::retry)
                    is HistoryState.Content -> {
                        if (h.items.isEmpty()) {
                            EmptyState(Icons.Filled.Description, "No analyses yet", "Analyze an agreement to see it here.") {
                                SecondaryButton("Analyze Agreement", onAnalyze, modifier = Modifier.padding(horizontal = 32.dp))
                            }
                        } else {
                            h.items.take(2).forEach { AnalysisRow(it, onClick = { onOpenReport(it.id) }) }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().background(InfoBlueTint, RoundedCornerShape(18.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Lightbulb, null, tint = InfoBlue, modifier = Modifier.size(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("FINSHIELD TIP", color = InfoBlue, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    Text(
                        "A lower interest rate may not mean a cheaper loan. Always compare processing fees and total repayment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}

private fun initials(name: String): String =
    name.trim().split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
private fun HeroCard(onAnalyze: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(26.dp), ambientColor = Navy, spotColor = Navy)
            .background(Navy, RoundedCornerShape(26.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShieldLogo(size = 48.dp)
            Text(
                "DOCUMENT REVIEW",
                color = Navy,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                modifier = Modifier.background(Color(0xFFD1FAE5), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Text("Analyze your loan agreement", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(
            "Upload your PDF to extract key terms, verify EMI calculations and review important clauses.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(EmeraldButton, RoundedCornerShape(16.dp))
                .clickable(onClick = onAnalyze),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Analyze Agreement", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.size(10.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.size(6.dp))
            Text("Documents are opened only on your device", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
        }
    }
}

@Composable
private fun RowScope.QuickAction(
    icon: ImageVector,
    tint: Color,
    tileBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .weight(1f)
            .background(CardWhite, RoundedCornerShape(18.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconTile(icon, tint, tileBackground, size = 40)
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp), color = TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextSecondary)
    }
}
