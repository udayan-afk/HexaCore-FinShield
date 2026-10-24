package com.finshield.app.ui.screens.analyzer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.finshield.app.domain.analyzer.PageExtraction
import com.finshield.app.model.AnalysisReport
import com.finshield.app.model.CompletenessStatus
import com.finshield.app.model.TextSourceKind
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.StatusChip
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
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

/**
 * Full Extracted Text & Completeness Viewer.
 *
 * Provides users with complete transparency over:
 * 1. Full raw text captured from every page, with page boundaries & navigation.
 * 2. Search within extracted document text with keyword highlighting.
 * 3. Accurate provenance badges (Native PDF vs ML Kit OCR vs Hybrid).
 * 4. Warning indicators for pages with low confidence or blurriness.
 * 5. Direct evidence links mapping structured fields back to exact source text.
 */
@Composable
fun FullDocumentTextViewerDialog(
    report: AnalysisReport,
    initialPageIndex: Int = 0,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pages = if (report.rawPages.isNotEmpty()) report.rawPages else listOf(
        PageExtraction(
            pageNumber = 1,
            text = "No raw page text available for this document.",
            charCount = 0,
            isBlank = true,
        ),
    )

    var selectedPageIndex by remember { mutableIntStateOf(initialPageIndex.coerceIn(0, pages.size - 1)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Raw Document Text, 1: Field Evidence & Completeness

    val currentPage = pages.getOrElse(selectedPageIndex) { pages.first() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            color = AppBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Full Extracted Text",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                        )
                        Text(
                            text = "${report.documentName} • ${pages.size} page${if (pages.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Navy,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CardWhite,
                    contentColor = Navy,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = EmeraldDark,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp)),
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Page Text (${pages.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) EmeraldDark else TextSecondary,
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Completeness (${report.completenessReport?.completenessPercentage ?: 0}%)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) EmeraldDark else TextSecondary,
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search text across agreement…", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldDark,
                            unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardWhite,
                            unfocusedContainerColor = CardWhite,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Page Navigation Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            itemsIndexed(pages) { index, page ->
                                val isSelected = index == selectedPageIndex
                                val hasWarning = page.isBlank || page.isUnreadable || page.warnings.isNotEmpty()
                                val matchesSearch = searchQuery.isNotBlank() && page.text.contains(searchQuery, ignoreCase = true)

                                val chipBg = when {
                                    isSelected -> Navy
                                    matchesSearch -> EmeraldTint
                                    hasWarning -> AmberTint
                                    else -> CardWhite
                                }
                                val textColor = when {
                                    isSelected -> Color.White
                                    matchesSearch -> EmeraldDark
                                    hasWarning -> AmberDark
                                    else -> TextPrimary
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipBg)
                                        .border(1.dp, if (isSelected) Navy else DividerColor, RoundedCornerShape(8.dp))
                                        .clickable { selectedPageIndex = index }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Page ${page.pageNumber}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor,
                                        )
                                        if (hasWarning) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.WarningAmber,
                                                contentDescription = "Warning",
                                                tint = if (isSelected) Amber else AmberDark,
                                                modifier = Modifier.size(12.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Copy Page Text Button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Page ${currentPage.pageNumber} Text", currentPage.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Page ${currentPage.pageNumber} text copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CardWhite)
                                .border(1.dp, DividerColor, CircleShape),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = Navy, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Page Meta & Source Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val sourceLabel = when (currentPage.sourceKind) {
                                TextSourceKind.NATIVE_PDF -> "Native PDF Text"
                                TextSourceKind.OCR -> "ML Kit OCR v2"
                                TextSourceKind.HYBRID -> "Hybrid (Native + OCR)"
                                TextSourceKind.AI_ASSISTED -> "Gemini AI Assisted"
                                TextSourceKind.USER_CORRECTED -> "User Corrected"
                            }
                            StatusChip(
                                text = sourceLabel,
                                background = if (currentPage.sourceKind == TextSourceKind.NATIVE_PDF) EmeraldTint else InfoBlueTint,
                                content = if (currentPage.sourceKind == TextSourceKind.NATIVE_PDF) EmeraldDark else InfoBlue,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentPage.charCount} characters",
                                fontSize = 11.sp,
                                color = TextSecondary,
                            )
                        }

                        Text(
                            text = "Page ${currentPage.pageNumber} of ${pages.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy,
                        )
                    }

                    // Warning banner if unreadable
                    if (currentPage.isUnreadable || currentPage.isBlank || currentPage.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AmberTint),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentPage.warnings.firstOrNull() ?: "Page appeared empty or had low recognition confidence. Please inspect.",
                                    fontSize = 12.sp,
                                    color = AmberDark,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Extracted Text Card
                    FinCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                if (currentPage.text.isBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "No readable text detected on Page ${currentPage.pageNumber}.",
                                            color = TextHint,
                                            fontSize = 13.sp,
                                        )
                                    }
                                } else {
                                    val annotatedText = if (searchQuery.isNotBlank()) {
                                        highlightSearchQuery(currentPage.text, searchQuery)
                                    } else {
                                        buildAnnotatedString { append(currentPage.text) }
                                    }

                                    Text(
                                        text = annotatedText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(4.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Completeness & Field Evidence Tab
                    CompletenessReportView(report = report)
                }
            }
        }
    }
}

@Composable
private fun CompletenessReportView(report: AnalysisReport) {
    val completeness = report.completenessReport
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        if (completeness == null) {
            Text(
                "Completeness audit report is generating…",
                fontSize = 13.sp,
                color = TextSecondary,
            )
            return
        }

        // Summary Card
        FinCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Document Extraction Completeness",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                    )
                    Text(
                        text = "${completeness.foundCount} of ${completeness.totalFieldsChecked} standard loan terms identified",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (completeness.completenessPercentage >= 75) EmeraldTint else AmberTint)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "${completeness.completenessPercentage}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (completeness.completenessPercentage >= 75) EmeraldDark else AmberDark,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                MetricPill(label = "Identified", count = completeness.foundCount, color = EmeraldDark)
                MetricPill(label = "Not Stated", count = completeness.notStatedCount, color = TextSecondary)
                MetricPill(label = "Needs Review", count = completeness.needsVerificationCount + completeness.ambiguousCount, color = AmberDark)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Field-by-Field Evidence Inventory",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Navy,
        )
        Text(
            text = "Grounding snippets and page locations verified from document text",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        completeness.items.forEach { item ->
            FieldEvidenceCard(item = item)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (report.unrecognizedTerms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Other Detected Clauses & Annexure Terms",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Navy,
            )
            Spacer(modifier = Modifier.height(6.dp))
            report.unrecognizedTerms.forEach { finding ->
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Text(finding.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy)
                    if (finding.value != null) {
                        Text(finding.value, fontSize = 13.sp, color = TextPrimary)
                    }
                    if (finding.evidence != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${finding.evidence}\"",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = TextSecondary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetricPill(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun FieldEvidenceCard(item: com.finshield.app.model.FieldCompletenessItem) {
    val isFound = item.status == CompletenessStatus.FOUND
    val statusBg = if (isFound) EmeraldTint else CardWhite
    val statusColor = if (isFound) EmeraldDark else TextHint

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.fieldLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Navy,
                    modifier = Modifier.weight(1f),
                )

                StatusChip(
                    text = item.status.label,
                    background = if (isFound) EmeraldTint else DividerColor.copy(alpha = 0.3f),
                    content = if (isFound) EmeraldDark else TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isFound && !item.extractedValue.isNullOrBlank()) {
                Text(
                    text = item.extractedValue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                )
            } else {
                Text(
                    text = "Not stated in document",
                    fontSize = 12.sp,
                    color = TextHint,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }

            if (!item.evidenceSnippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "Source: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                    )
                    Text(
                        text = "\"${item.evidenceSnippet.trim()}\"${item.pageNumber?.let { " (Page $it)" } ?: ""}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    )
                }
            }

            if (!item.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Note: ${item.notes}",
                    fontSize = 10.sp,
                    color = InfoBlue,
                )
            }
        }
    }
}

private fun highlightSearchQuery(text: String, query: String) = buildAnnotatedString {
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var startIdx = 0

    while (startIdx < text.length) {
        val foundIdx = lowerText.indexOf(lowerQuery, startIdx)
        if (foundIdx == -1) {
            append(text.substring(startIdx))
            break
        }
        append(text.substring(startIdx, foundIdx))
        withStyle(SpanStyle(background = AmberTint, color = Navy, fontWeight = FontWeight.Bold)) {
            append(text.substring(foundIdx, foundIdx + query.length))
        }
        startIdx = foundIdx + query.length
    }
}
