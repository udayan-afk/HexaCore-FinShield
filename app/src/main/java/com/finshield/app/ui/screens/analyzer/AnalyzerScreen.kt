package com.finshield.app.ui.screens.analyzer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.IconTile
import com.finshield.app.ui.components.LinkText
import com.finshield.app.ui.components.PrimaryButton
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
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Outline
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.RoseTint
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@Composable
fun AnalyzerScreen(
    onOpenSampleReport: () -> Unit,
    onOpenReport: (String) -> Unit = { onOpenSampleReport() },
    onBack: () -> Unit = {},
    vm: AnalyzerViewModel = viewModel(),
) {
    StatusBarIcons(darkIcons = true)
    val context = LocalContext.current
    val ui by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    BackHandler { onBack() }

    // 1. PDF Picker
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.onPdfPicked(uri, context)
        }
    }
    val launchPdfPicker = { pdfPicker.launch(arrayOf("application/pdf")) }

    // 2. Google ML Kit Document Scanner
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            val pages = result?.pages?.map { it.imageUri } ?: emptyList()
            val pdfUri = result?.pdf?.uri
            vm.onPagesScanned(pages, pdfUri)
        } else if (activityResult.resultCode == Activity.RESULT_CANCELED) {
            vm.onScanCancelled()
        } else {
            vm.onScanError("Scanning was cancelled or could not be completed.")
        }
    }

    val activity = remember(context) { context.findActivity() }
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val launchScanner: () -> Unit = {
        if (activity != null) {
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { error ->
                    vm.onScanError(error.localizedMessage ?: "Could not open document scanner on this device.")
                }
        } else {
            vm.onScanError("Activity context unavailable for scanner.")
        }
    }

    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
    }

    ScreenScaffold(
        title = "Analyze agreement",
        subtitle = "Secure document review",
        onBack = onBack,
        snackbarHostState = snackbar,
        handleNavBarInset = false,
    ) {
        val doc = ui.document

        // Step indicator: 1. Input -> 2. Review & Process -> 3. Analysis
        StepIndicator(
            current = when {
                ui.isAnalyzing -> 1
                doc != null -> 1
                else -> 0
            },
        )

        if (doc == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Submit loan agreement",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Choose how you want to provide your loan agreement for automated clause and mathematical verification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            // Action 1: Upload PDF
            ActionCard(
                icon = Icons.Filled.FileUpload,
                iconColor = EmeraldDark,
                iconBg = EmeraldTint,
                title = "Upload PDF",
                description = "Select a digital agreement PDF from your phone storage",
                badgeText = "PDF only · Max 20 MB",
                buttonText = "Select PDF",
                onAction = launchPdfPicker,
            )

            // Action 2: Scan with Camera
            ActionCard(
                icon = Icons.Filled.CameraAlt,
                iconColor = InfoBlue,
                iconBg = InfoBlueTint,
                title = "Scan with Camera",
                description = "Capture multi-page paper agreements with auto-crop, edge detection & enhancement",
                badgeText = "ML Kit Scanner · Up to 20 pages",
                buttonText = "Start Camera Scan",
                onAction = launchScanner,
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider(Modifier.weight(1f), color = DividerColor)
                Text("or", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                HorizontalDivider(Modifier.weight(1f), color = DividerColor)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinkText("Try a sample agreement", onOpenSampleReport)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            // Document selected state (PDF or Scan)
            SelectedDocumentCard(
                doc = doc,
                isAnalyzing = ui.isAnalyzing,
                progressMessage = ui.progressMessage,
                onAnalyze = {
                    vm.startAnalysis(context) { reportId ->
                        onOpenReport(reportId)
                    }
                },
                onChangePdf = launchPdfPicker,
                onRescan = launchScanner,
                onRemove = vm::clear,
                onOpenSampleReport = onOpenSampleReport,
            )
        }

        // Privacy Banner
        Row(
            Modifier
                .fillMaxWidth()
                .background(EmeraldTint, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Your privacy comes first",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text(
                    "Documents are processed 100% on-device using Android's native tools and ML Kit. Your files and captured pages are never sent to external AI servers or logged.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                )
            }
        }

        DisclaimerText()
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    description: String,
    badgeText: String,
    buttonText: String,
    onAction: () -> Unit,
) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconTile(icon, iconColor, iconBg, size = 48)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    badgeText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextHint,
                )
                SecondaryButton(
                    text = buttonText,
                    onClick = onAction,
                    modifier = Modifier.width(160.dp),
                )
            }
        }
    }
}

@Composable
private fun SelectedDocumentCard(
    doc: SelectedDocument,
    isAnalyzing: Boolean,
    progressMessage: String,
    onAnalyze: () -> Unit,
    onChangePdf: () -> Unit,
    onRescan: () -> Unit,
    onRemove: () -> Unit,
    onOpenSampleReport: () -> Unit,
) {
    val isPdf = doc is SelectedDocument.Pdf

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(
                    if (isPdf) Icons.Filled.PictureAsPdf else Icons.Filled.CameraAlt,
                    if (isPdf) Rose else InfoBlue,
                    if (isPdf) RoseTint else InfoBlueTint,
                )
                Column(Modifier.weight(1f)) {
                    Text(doc.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 2)
                    Text(
                        doc.pageCountDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                StatusChip(
                    if (isPdf) "PDF File" else "Camera Scan",
                    if (isPdf) RoseTint else InfoBlueTint,
                    if (isPdf) Rose else InfoBlue,
                )
            }

            HorizontalDivider(color = DividerColor)

            if (isAnalyzing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldTint)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        color = EmeraldDark,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        progressMessage.ifEmpty { "Extracting and verifying document terms…" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldDark,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Running on-device OCR and cross-checking repayments",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip("Ready for OCR", EmeraldTint, EmeraldDark)
                    Text(
                        "All pages ready to be read and cross-checked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                PrimaryButton(
                    text = "Analyze Agreement",
                    onClick = onAnalyze,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SecondaryButton(
                        text = if (isPdf) "Change PDF" else "Rescan Pages",
                        onClick = if (isPdf) onChangePdf else onRescan,
                        leadingIcon = if (isPdf) Icons.Filled.Description else Icons.Filled.Refresh,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "Remove",
                        onClick = onRemove,
                        leadingIcon = Icons.Filled.Delete,
                        modifier = Modifier.weight(0.8f),
                    )
                }
            }

            LinkText("Or preview the sample report layout", onOpenSampleReport)
        }
    }
}

@Composable
private fun StepIndicator(current: Int) {
    val labels = listOf("Input", "OCR & Check", "Report")
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
                        if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        else Text("${i + 1}", color = if (active) Color.White else TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (active || done) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (active || done) EmeraldDark else TextSecondary,
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
