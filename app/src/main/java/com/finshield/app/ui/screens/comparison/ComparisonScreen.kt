package com.finshield.app.ui.screens.comparison

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.domain.calculator.InterestMethod
import com.finshield.app.domain.calculator.Money
import com.finshield.app.model.ComputedLoanOffer
import com.finshield.app.model.LenderClassification
import com.finshield.app.model.LoanCategory
import com.finshield.app.model.LoanOffer
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.EmptyState
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.FinTextField
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
import com.finshield.app.ui.theme.Emerald
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.Outline
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.RoseTint
import com.finshield.app.ui.theme.SegmentTrack
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ComparisonScreen(
    onBack: () -> Unit = {},
    vm: ComparisonViewModel = viewModel(),
) {
    StatusBarIcons(darkIcons = true)
    BackHandler { onBack() }

    val state by vm.state.collectAsState()

    ScreenScaffold(
        title = "Compare Loan Offers",
        subtitle = "Research catalogue & rate comparison",
        onBack = onBack,
        handleNavBarInset = false,
    ) {
        // 1. Dataset Information Banner (Feature 3: Source & Freshness Indicators)
        DatasetInfoBanner(
            datasetVersion = state.datasetVersion,
            refreshDate = state.datasetRefreshDate,
            totalLenders = state.allOffers.size,
        )

        // 2. Loan Category Selector
        LoanCategorySelector(
            selectedCategory = state.selectedCategory,
            onSelect = vm::onCategorySelected,
        )

        // 3. Comparison Principal & Tenure Terms Bar
        ComparisonSummaryBar(
            principal = state.principal,
            tenureMonths = state.tenureMonths,
            onEdit = { vm.setEditingTerms(true) },
        )

        // 4. Search and Filter Controls (Search + Lender Type Filter + Sort)
        SearchAndFilterControls(
            searchQuery = state.searchQuery,
            onSearchChange = vm::onSearchQueryChanged,
            lenderTypeFilter = state.lenderTypeFilter,
            onSelectLenderType = vm::onLenderTypeFilterChanged,
            sortOption = state.sortOption,
            onSelectSort = vm::onSortOptionChanged,
        )

        // 5. Multi-Offer "At a Glance" Comparison Table
        if (state.comparisonTableOffers.size >= 2) {
            AtAGlanceComparisonTable(
                offers = state.comparisonTableOffers,
                onRemoveOffer = vm::toggleOfferSelection,
            )
        } else {
            SelectMoreOffersPrompt(
                selectedCount = state.comparisonTableOffers.size,
            )
        }

        // 6. Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Available Lenders (${state.displayedOffers.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Navy,
            )
            if (state.searchQuery.isNotBlank() || state.lenderTypeFilter != LenderTypeFilter.ALL) {
                Text(
                    "Reset filters",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldDark,
                    modifier = Modifier.clickable { vm.resetFilters() },
                )
            }
        }

        // 7. Offer Discovery Cards
        if (state.displayedOffers.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Search,
                title = "No lenders found",
                message = "No lenders match your search or filter criteria. Try adjusting the lender type filter or searching another keyword.",
            ) {
                SecondaryButton(
                    text = "Reset all filters",
                    onClick = vm::resetFilters,
                    modifier = Modifier.width(200.dp),
                )
            }
        } else {
            state.displayedOffers.forEach { offer ->
                LenderDiscoveryCard(
                    offer = offer,
                    onToggleCompare = { vm.toggleOfferSelection(offer.offer.id) },
                    onViewDetails = { vm.openOfferDetails(offer) },
                )
            }
        }

        // 8. Methodology & Regulatory Notes
        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Methodology & Regulatory Notes",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Navy,
                )
                listOf(
                    "Starting rates shown are indicative rack rates published by lenders or research aggregators. Final rates depend on individual credit score, income and employer assessment.",
                    "Reducing balance loans use the standard EMI formula: P × r × (1+r)ⁿ / ((1+r)ⁿ − 1).",
                    "Monthly rates published by certain digital lenders or NBFCs are shown as-is and never converted to annual rates without verified lender APR.",
                    "NBFCs and digital credit platforms are regulated non-banking entities and are not commercial banks.",
                    "Lowest total cost indicates the mathematically lowest cost on sample terms; it is not a loan approval or recommendation.",
                ).forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
                }
            }
        }

        DisclaimerText()
        Spacer(Modifier.height(16.dp))
    }

    // Dialog: Edit Comparison Terms
    if (state.isEditingTerms) {
        EditTermsDialog(
            currentPrincipal = state.principal,
            currentTenure = state.tenureMonths,
            onDismiss = { vm.setEditingTerms(false) },
            onApply = vm::updateTerms,
        )
    }

    // Dialog: Full Offer Details & Research Notes
    state.selectedOfferForDetails?.let { computedOffer ->
        LenderDetailsDialog(
            computedOffer = computedOffer,
            onDismiss = { vm.openOfferDetails(null) },
        )
    }
}

// ---------------------------------------------------------------------------
// 1. DATASET INFO BANNER (FEATURE 3: SOURCE & FRESHNESS INDICATORS)
// ---------------------------------------------------------------------------

@Composable
private fun DatasetInfoBanner(
    datasetVersion: String,
    refreshDate: String,
    totalLenders: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InfoBlueTint)
            .border(1.dp, InfoBlue.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(InfoBlue.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = InfoBlue,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Lender Research Catalogue ($totalLenders Lenders)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Navy,
                )
                StatusChip("v$datasetVersion", SegmentTrack, TextSecondary)
            }
            Text(
                "Static research dataset as of $refreshDate. Not live bank rates. Displayed rates are indicative starting terms and must be verified with the lender before applying.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. LOAN CATEGORY SELECTOR
// ---------------------------------------------------------------------------

@Composable
private fun LoanCategorySelector(
    selectedCategory: LoanCategory,
    onSelect: (LoanCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Loan Category",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Navy,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(12.dp))
                .background(SegmentTrack)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LoanCategory.entries.forEach { cat ->
                val selected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (selected) {
                                Modifier
                                    .shadow(2.dp, RoundedCornerShape(10.dp))
                                    .background(CardWhite)
                            } else {
                                Modifier.background(Color.Transparent)
                            },
                        )
                        .clickable { onSelect(cat) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        cat.shortLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selected) Navy else TextSecondary,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. COMPARISON SUMMARY BAR
// ---------------------------------------------------------------------------

@Composable
private fun ComparisonSummaryBar(
    principal: Double,
    tenureMonths: Int,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val summaryText = buildAnnotatedString {
            withStyle(SpanStyle(color = TextSecondary, fontSize = 12.sp)) {
                append("Simulating terms: ")
            }
            withStyle(SpanStyle(color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                append("${Money.inr(principal)} over $tenureMonths months")
            }
        }
        Text(summaryText)

        Text(
            "Change Terms",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = EmeraldDark,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onEdit() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 4. SEARCH AND FILTER CONTROLS
// ---------------------------------------------------------------------------

@Composable
private fun SearchAndFilterControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    lenderTypeFilter: LenderTypeFilter,
    onSelectLenderType: (LenderTypeFilter) -> Unit,
    sortOption: ComparisonSortOption,
    onSelectSort: (ComparisonSortOption) -> Unit,
) {
    var lenderTypeMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by lender name, fee or keywords", style = MaterialTheme.typography.bodySmall, color = TextHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
                focusedBorderColor = Emerald,
                unfocusedBorderColor = DividerColor,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        )

        // Filter by Lender Type & Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Lender Type Filter Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardWhite)
                        .border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(10.dp))
                        .clickable { lenderTypeMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Navy,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        lenderTypeFilter.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }

                DropdownMenu(
                    expanded = lenderTypeMenuExpanded,
                    onDismissRequest = { lenderTypeMenuExpanded = false },
                    modifier = Modifier.background(CardWhite),
                ) {
                    LenderTypeFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (option == lenderTypeFilter) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                    color = if (option == lenderTypeFilter) EmeraldDark else TextPrimary,
                                )
                            },
                            onClick = {
                                onSelectLenderType(option)
                                lenderTypeMenuExpanded = false
                            },
                        )
                    }
                }
            }

            // Sort Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Sort: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        sortOption.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextPrimary,
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(CardWhite),
                ) {
                    ComparisonSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                    color = if (option == sortOption) EmeraldDark else TextPrimary,
                                )
                            },
                            onClick = {
                                onSelectSort(option)
                                sortMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. AT A GLANCE COMPARISON TABLE (MULTI-OFFER COMPARISON)
// ---------------------------------------------------------------------------

@Composable
private fun AtAGlanceComparisonTable(
    offers: List<ComputedLoanOffer>,
    onRemoveOffer: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(14.dp)),
    ) {
        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
        ) {
            // Label Column
            Column(
                modifier = Modifier.width(105.dp),
            ) {
                TableCell(text = "At a glance", isHeader = true, textColor = TextSecondary, fontSize = 11)
                HorizontalDivider(color = DividerColor)
                TableCell(text = "Lender Type", isHeader = false, textColor = TextSecondary)
                HorizontalDivider(color = DividerColor)
                TableCell(text = "Starting Rate", isHeader = false, textColor = TextSecondary)
                HorizontalDivider(color = DividerColor)
                TableCell(text = "Estimated EMI", isHeader = false, textColor = TextSecondary)
                HorizontalDivider(color = DividerColor)
                TableCell(text = "Max Loan", isHeader = false, textColor = TextSecondary)
                HorizontalDivider(color = DividerColor)
                TableCell(text = "Total Cost", isHeader = false, textColor = TextSecondary)
            }

            // Data Columns for each selected offer
            offers.forEach { computed ->
                VerticalDivider(color = DividerColor, modifier = Modifier.height(264.dp))

                Column(
                    modifier = Modifier.width(135.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header: Lender Name with remove action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            computed.offer.lenderName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = Navy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onRemoveOffer(computed.offer.id) },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextHint, modifier = Modifier.size(14.dp))
                        }
                    }
                    HorizontalDivider(color = DividerColor)

                    // Lender Type
                    TableCell(
                        text = computed.offer.lenderClassification.shortLabel,
                        isHeader = false,
                        textColor = when (computed.offer.lenderClassification) {
                            LenderClassification.PUBLIC_BANK -> Navy
                            LenderClassification.PRIVATE_BANK -> EmeraldDark
                            LenderClassification.NBFC -> AmberDark
                            LenderClassification.DIGITAL_LENDER -> InfoBlue
                        },
                        isBold = true,
                        fontSize = 11,
                    )
                    HorizontalDivider(color = DividerColor)

                    // Starting Rate
                    TableCell(
                        text = computed.offer.publishedInterestRate,
                        isHeader = false,
                        textColor = TextPrimary,
                        isBold = true,
                        fontSize = 11,
                    )
                    HorizontalDivider(color = DividerColor)

                    // Estimated EMI
                    TableCell(
                        text = computed.installment?.let { Money.inr2(it) } ?: "Verify APR",
                        isHeader = false,
                        textColor = if (computed.installment != null) TextPrimary else AmberDark,
                        isBold = computed.installment != null,
                        fontSize = 11,
                    )
                    HorizontalDivider(color = DividerColor)

                    // Max Loan
                    TableCell(
                        text = computed.offer.rawMaxLoanAmount.ifBlank { "—" },
                        isHeader = false,
                        textColor = TextPrimary,
                        fontSize = 11,
                    )
                    HorizontalDivider(color = DividerColor)

                    // Total Cost
                    TableCell(
                        text = computed.totalCost?.let { Money.inr(it) } ?: "Indicative",
                        isHeader = false,
                        textColor = if (computed.isLowestCost) EmeraldDark else TextPrimary,
                        isBold = true,
                        fontSize = 11,
                    )
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    isHeader: Boolean,
    textColor: Color,
    isBold: Boolean = false,
    fontSize: Int = 12,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isBold || isHeader) FontWeight.Bold else FontWeight.Medium,
                fontSize = fontSize.sp,
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SelectMoreOffersPrompt(selectedCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(12.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (selectedCount == 0) "Select at least 2 lenders from the list below to compare side-by-side."
            else "1 lender selected. Tap '+ Add to compare' on another lender for side-by-side comparison.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------
// 7. LENDER DISCOVERY CARD
// ---------------------------------------------------------------------------

@Composable
private fun LenderDiscoveryCard(
    offer: ComputedLoanOffer,
    onToggleCompare: () -> Unit,
    onViewDetails: () -> Unit,
) {
    val o = offer.offer
    val isLowest = offer.isLowestCost

    val (typeBg, typeColor) = when (o.lenderClassification) {
        LenderClassification.PUBLIC_BANK -> Pair(Color(0xFFE8EEF8), Navy)
        LenderClassification.PRIVATE_BANK -> Pair(EmeraldTint, EmeraldDark)
        LenderClassification.NBFC -> Pair(AmberTint, AmberDark)
        LenderClassification.DIGITAL_LENDER -> Pair(InfoBlueTint, InfoBlue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isLowest) 3.dp else 1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .border(
                BorderStroke(
                    width = if (isLowest) 1.5.dp else 1.dp,
                    color = if (isLowest) Emerald else DividerColor,
                ),
                RoundedCornerShape(16.dp),
            ),
    ) {
        if (isLowest) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Emerald)
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Lowest estimated total cost among comparable offers",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: Avatar, Name, Type Chip & Indicative Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(typeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        o.lenderInitial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = typeColor),
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        o.lenderName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Navy,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        StatusChip(o.lenderClassification.shortLabel, typeBg, typeColor)
                        StatusChip("Indicative", SegmentTrack, TextSecondary)
                    }
                }

                // Published Rate Pill
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SegmentTrack)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            o.publishedInterestRate,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Navy,
                        )
                    }
                    Text(
                        if (o.isMonthlyRate) "Monthly Rate" else "Published Rate",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextHint,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            HorizontalDivider(color = DividerColor)

            // Monthly rate notice (CASHe, Home Credit, MoneyTap, PaySense)
            if (o.isMonthlyRate) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberTint.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = AmberDark, modifier = Modifier.size(16.dp))
                    Text(
                        "Monthly rate: not comparable directly to annual bank rates. Request verified APR from lender.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = AmberDark,
                    )
                }
            }

            // 2-Column Key Specs Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecItem(
                        label = "Estimated Monthly EMI",
                        value = offer.installment?.let { Money.inr2(it) } ?: "Verify APR with lender",
                        valueBold = offer.installment != null,
                        color = if (offer.installment != null) Navy else AmberDark,
                    )
                    SpecItem(
                        label = "Max Loan Amount",
                        value = o.rawMaxLoanAmount.ifBlank { "Subject to profile" },
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecItem(
                        label = "Processing Fee",
                        value = o.publishedProcessingFee.ifBlank { "Disclosed in agreement" },
                    )
                    SpecItem(
                        label = "Max Tenure",
                        value = "${o.maxTenureMonths} months",
                    )
                }
            }

            // Estimated Total Cost Highlight Box (only when calculable)
            if (offer.totalCost != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFECFDF5))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Estimated Total Cost (Sample)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Navy,
                        )
                        Text(
                            "Repayment on ${Money.inr(offer.principal)} / ${offer.tenureMonths} mo + fee",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                        )
                    }

                    Text(
                        Money.inr(offer.totalCost),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                        color = EmeraldDark,
                    )
                }
            }

            // Freshness & Source Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Source: Paisabazaar (${o.sourceAsOf.ifBlank { "14 Oct 2026" }})",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextHint,
                )
                Text(
                    "Status: Indicative",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic),
                    color = TextHint,
                )
            }

            HorizontalDivider(color = DividerColor)

            // Bottom Actions: Compare toggle + View details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Compare Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (offer.isSelectedForComparison) {
                                Modifier
                                    .background(EmeraldTint)
                                    .border(BorderStroke(1.dp, EmeraldDark), RoundedCornerShape(8.dp))
                            } else {
                                Modifier
                                    .background(SegmentTrack)
                                    .border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(8.dp))
                            },
                        )
                        .clickable { onToggleCompare() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        if (offer.isSelectedForComparison) Icons.Default.Check else Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = if (offer.isSelectedForComparison) EmeraldDark else TextPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        if (offer.isSelectedForComparison) "In Comparison" else "+ Add to Compare",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (offer.isSelectedForComparison) EmeraldDark else TextPrimary,
                        ),
                    )
                }

                // View Details
                Text(
                    "View Details & Notes",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldDark,
                    modifier = Modifier
                        .clickable { onViewDetails() }
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun SpecItem(
    label: String,
    value: String,
    valueBold: Boolean = false,
    color: Color = TextPrimary,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextSecondary,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (valueBold) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// DIALOGS: EDIT TERMS & LENDER DETAILS
// ---------------------------------------------------------------------------

@Composable
private fun EditTermsDialog(
    currentPrincipal: Double,
    currentTenure: Int,
    onDismiss: () -> Unit,
    onApply: (Double, Int) -> Unit,
) {
    var principalText by remember { mutableStateOf(currentPrincipal.toLong().toString()) }
    var tenureText by remember { mutableStateOf(currentTenure.toString()) }
    var principalError by remember { mutableStateOf<String?>(null) }
    var tenureError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Comparison Simulation Terms",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Navy,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Update the shared loan amount and tenure. All comparable lender calculations will recompute automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                FinTextField(
                    label = "Principal amount",
                    value = principalText,
                    onValueChange = {
                        principalText = it.filter { c -> c.isDigit() }
                        principalError = null
                    },
                    placeholder = "500000",
                    prefix = "₹ ",
                    error = principalError,
                    keyboardType = KeyboardType.Number,
                )

                FinTextField(
                    label = "Tenure",
                    value = tenureText,
                    onValueChange = {
                        tenureText = it.filter { c -> c.isDigit() }
                        tenureError = null
                    },
                    placeholder = "36",
                    suffix = "months",
                    error = tenureError,
                    keyboardType = KeyboardType.Number,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = principalText.toDoubleOrNull() ?: 0.0
                    val t = tenureText.toIntOrNull() ?: 0

                    if (p < 10_000) {
                        principalError = "Enter at least ₹10,000"
                        return@TextButton
                    }
                    if (t < 3 || t > 360) {
                        tenureError = "Tenure must be between 3 and 360 months"
                        return@TextButton
                    }

                    onApply(p, t)
                },
            ) {
                Text("Apply Terms", color = EmeraldDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun LenderDetailsDialog(
    computedOffer: ComputedLoanOffer,
    onDismiss: () -> Unit,
) {
    val o = computedOffer.offer
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    o.lenderName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Navy,
                )
                Text(
                    o.lenderClassification.label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DetailRow("Lender Classification", o.lenderClassification.label)
                DetailRow("Published Interest Rate", o.publishedInterestRate, isBold = true)
                DetailRow("Rate Type", if (o.isMonthlyRate) "Monthly Rate (Non-Annualised)" else "Annual Reducing Rate")
                DetailRow("Published Processing Fee", o.publishedProcessingFee)
                DetailRow("Maximum Loan Amount", o.rawMaxLoanAmount)
                DetailRow("Maximum Tenure", "${o.maxTenureMonths} months")
                HorizontalDivider(color = DividerColor)

                if (computedOffer.installment != null) {
                    DetailRow("Simulated Monthly EMI", Money.inr2(computedOffer.installment), isBold = true)
                    DetailRow("Simulated Total Repayment", Money.inr2(computedOffer.totalRepayment ?: 0.0))
                    DetailRow("Simulated Total Cost", Money.inr2(computedOffer.totalCost ?: 0.0), isBold = true, highlightColor = EmeraldDark)
                } else {
                    Text(
                        "Direct numeric comparison is not generated because the published rate is either monthly or offer-dependent. Confirm verified annual APR directly with ${o.lenderName}.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontStyle = FontStyle.Italic),
                        color = AmberDark,
                    )
                }

                HorizontalDivider(color = DividerColor)
                DetailRow("Eligibility / Research Notes", o.eligibilityNotes)
                DetailRow("Dataset Verification Status", o.datasetStatus)
                DetailRow("Source As-Of Date", o.sourceAsOf)
                DetailRow("Dataset Version", o.datasetVersion)

                if (!o.officialUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SegmentTrack)
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(o.officialUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "View Source Page on Paisabazaar",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Navy,
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Navy,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextPrimary)
            }
        },
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    highlightColor: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            ),
            color = highlightColor ?: TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f),
        )
    }
}
