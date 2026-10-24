package com.finshield.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.data.history.HistoryState
import com.finshield.app.model.AnalysisSummary
import com.finshield.app.ui.components.EmptyState
import com.finshield.app.ui.components.ErrorState
import com.finshield.app.ui.components.LinkText
import com.finshield.app.ui.components.LoadingBlock
import com.finshield.app.ui.components.IconTile
import com.finshield.app.ui.components.SampleNotice
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.StatusChip
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.RoseTint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenReport: (String) -> Unit, onAnalyze: () -> Unit, vm: HistoryViewModel = viewModel()) {
    StatusBarIcons(darkIcons = true)
    val state by vm.state.collectAsState()

    ScreenScaffold(title = "Analysis history", subtitle = "Your saved reviews", onBack = onBack) {
        SampleNotice(
            "Sample analyses",
            "These reference analyses illustrate review capabilities. Your analyzed documents will appear here.",
        )
        when (val s = state) {
            HistoryState.Loading -> LoadingBlock(message = "Loading analyses…")
            is HistoryState.Error -> ErrorState(s.message, onRetry = vm::retry)
            is HistoryState.Content -> {
                if (s.items.isEmpty()) {
                    EmptyState(Icons.Filled.Description, "No saved analyses", "Analyses you save will appear here.") {
                        SecondaryButton("Restore sample entries", vm::restoreSamples)
                        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                        SecondaryButton("Analyze an agreement", onAnalyze)
                    }
                } else {
                    s.items.forEach { AnalysisRow(it, onClick = { onOpenReport(it.id) }) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        LinkText("Clear sample entries", onClick = vm::clear)
                    }
                }
            }
        }
    }
}

/** Reusable list row used by History and the Dashboard. */
@Composable
fun AnalysisRow(item: AnalysisSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(18.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(Icons.Filled.PictureAsPdf, Rose, RoseTint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.documentName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1)
            Text(
                (if (item.isSample) "Sample · " else "") + "Reviewed ${item.reviewedOn} · ${item.pages} pages",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        if (item.itemsToReview > 0) StatusChip("${item.itemsToReview} to review", AmberTint, AmberDark)
        else StatusChip("Clear", EmeraldTint, EmeraldDark)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}
