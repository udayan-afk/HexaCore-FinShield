package com.finshield.app.ui.screens.analyzer

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.IconTile
import com.finshield.app.ui.components.LinkText
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.components.StatusChip
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.Outline
import com.finshield.app.ui.theme.RoseTint
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

@Composable
fun AnalyzerScreen(
    onOpenSampleReport: () -> Unit,
    onBack: () -> Unit = {},
    vm: AnalyzerViewModel = viewModel(),
) {
    StatusBarIcons(darkIcons = true)
    val context = LocalContext.current
    val ui by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    BackHandler { onBack() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val info = readDocumentInfo(context, uri)
            if (info == null) vm.onPickerFailed() else vm.onDocumentPicked(info)
        }
    }
    val launchPicker = { picker.launch(arrayOf("application/pdf")) }

    LaunchedEffect(ui.error) {
        ui.error?.let { snackbar.showSnackbar(it); vm.dismissError() }
    }

    ScreenScaffold(
        title = "Analyze agreement",
        subtitle = "Secure document review",
        onBack = onBack,
        snackbarHostState = snackbar,
        handleNavBarInset = false,
    ) {
        val doc = ui.document
        StepIndicator(current = if (doc == null) 0 else 1)

        if (doc == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Upload your loan agreement", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, textAlign = TextAlign.Center)
                Text(
                    "We'll extract key terms, verify repayment figures and flag clauses that deserve your attention.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            DropZone(onSelect = launchPicker)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = DividerColor)
                Text("or", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                HorizontalDivider(Modifier.weight(1f), color = DividerColor)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                LinkText("Try a sample agreement", onOpenSampleReport)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
            }
        } else {
            SelectedDocumentCard(doc, onChange = launchPicker, onRemove = vm::clear, onOpenSampleReport = onOpenSampleReport)
        }

        Row(
            Modifier.fillMaxWidth().background(EmeraldTint, RoundedCornerShape(16.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Lock, null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
            Column {
                Text("Your privacy comes first", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Text(
                    "The PDF is opened only through Android's file picker. In this build it is not uploaded anywhere.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                )
            }
        }
        Text(
            "FinShield provides independent explanations, not legal or financial advice.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = TextHint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DropZone(onSelect: () -> Unit) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(24.dp))
            .drawBehind {
                drawRoundRect(Outline, cornerRadius = CornerRadius(24.dp.toPx()), style = Stroke(width = 1.5.dp.toPx(), pathEffect = dash))
            }
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconTile(Icons.Filled.FileUpload, EmeraldDark, EmeraldTint, size = 64)
        Spacer(Modifier.height(6.dp))
        Text("Select your agreement", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text("Choose a PDF from your device", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        SecondaryButton("Select PDF", onSelect, modifier = Modifier.padding(horizontal = 40.dp))
        Text("PDF only · Maximum 20 MB", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextHint)
    }
}

@Composable
private fun SelectedDocumentCard(
    doc: DocumentInfo,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    onOpenSampleReport: () -> Unit,
) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(Icons.Filled.PictureAsPdf, Rose, RoseTint)
                Column(Modifier.weight(1f)) {
                    Text(doc.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 2)
                    Text(
                        doc.sizeBytes?.let { AnalyzerViewModel.formatSize(it) } ?: "Size unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            HorizontalDivider(color = DividerColor)
            StatusChip("Extraction pending", AmberTint, AmberDark)
            Text(
                "PDF text extraction and analysis are not built into this frontend preview yet, so no values have been read from this file and no report has been generated for it.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            SecondaryButton("Choose a different PDF", onChange, leadingIcon = Icons.Filled.Description)
            SecondaryButton("Remove selection", onRemove)
            LinkText("Preview the report layout (sample data)", onOpenSampleReport)
        }
    }
}

@Composable
private fun StepIndicator(current: Int) {
    val labels = listOf("Upload", "Analyze", "Review")
    Box(Modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.padding(top = 14.dp, start = 36.dp, end = 36.dp), color = DividerColor)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, label ->
                val done = i < current
                val active = i == current
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(if (done || active) EmeraldButton else CardWhite, RoundedCornerShape(50))
                            .border(1.dp, if (done || active) EmeraldButton else Outline, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        else Text("${i + 1}", color = if (active) Color.White else TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = if (active || done) FontWeight.Bold else FontWeight.Normal), color = if (active || done) EmeraldDark else TextSecondary)
                }
            }
        }
    }
}
