package com.finshield.app.ui.screens.comparison

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import com.finshield.app.model.LoanCategory
import com.finshield.app.model.LoanOffer
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.EmptyState
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.FinTextField
import com.finshield.app.ui.components.PrimaryButton
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.Emerald
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.Outline
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
        title = "Compare loan offers",
        subtitle = "Same loan, clearer comparison",
        onBack = onBack,
        handleNavBarInset = false,
    ) {
        // 1. Info Banner
        DemonstrationNotice()

        // 2. Loan Category Selector
        LoanCategorySelector(
            selectedCategory = state.selectedCategory,
            onSelect = vm::onCategorySelected,
        )

        // 3. Compact Comparison Summary
        ComparisonSummaryBar(
            principal = state.principal,
            tenureMonths = state.tenureMonths,
            onEdit = { vm.setEditingTerms(true) },
        )

        // 4. Search and Filter Bar
        SearchAndFilterControls(
            searchQuery = state.searchQuery,
            onSearchChange = vm::onSearchQueryChanged,
            methodFilter = state.methodFilter,
            onCycleMethod = vm::cycleMethodFilter,
            sortOption = state.sortOption,
            onSelectSort = vm::onSortOptionChanged,
        )

        // 5. "At a glance" Comparison Table
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
                "Available offers (${state.displayedOffers.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            if (state.searchQuery.isNotBlank() || state.methodFilter != MethodFilter.ALL) {
                Text(
                    "Reset filters",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldDark,
                    modifier = Modifier.clickable { vm.resetFilters() },
                )
            }
        }

        // 7. Offer Catalogue Cards
        if (state.displayedOffers.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Search,
                title = "No loan offers found",
                message = "No offers match your search or filter criteria in ${state.selectedCategory.label}. Try adjusting filters or searching for another lender.",
            ) {
                SecondaryButton(
                    text = "Reset all filters",
                    onClick = vm::resetFilters,
                    modifier = Modifier.width(200.dp),
                )
            }
        } else {
            state.displayedOffers.forEach { offer ->
                OfferDiscoveryCard(
                    offer = offer,
                    onToggleCompare = { vm.toggleOfferSelection(offer.offer.id) },
                    onOfficialDetails = { vm.openOfficialDetails(offer.offer) },
                    onViewDetails = { vm.openOfferDetails(offer) },
                )
            }
        }

        // 8. Assumptions & Notes
        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Comparison assumptions & methodology", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                listOf(
                    "All offers are evaluated using the identical principal (${Money.inr(state.principal)}) and tenure (${state.tenureMonths} months).",
                    "Reducing balance loans use the standard amortisation formula. Flat rate loans compute total interest on the initial principal.",
                    "Estimated total cost equals total repayments plus disclosed processing fees and 18% GST.",
                    "Indicative APR reflects the annualised borrowing cost including upfront fees.",
                    "Lowest total cost indicates the mathematically lowest expense based on sample terms; it is not a loan approval or recommendation.",
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

    // Dialog: Official Lender Details Notice
    state.selectedOfferForOfficialDetails?.let { offer ->
        OfficialLenderDetailsDialog(
            offer = offer,
            onDismiss = { vm.openOfficialDetails(null) },
        )
    }

    // Dialog: Full Offer Breakdown
    state.selectedOfferForDetails?.let { computedOffer ->
        OfferDetailsDialog(
            computedOffer = computedOffer,
            onDismiss = { vm.openOfferDetails(null) },
            onOfficialDetails = {
                vm.openOfferDetails(null)
                vm.openOfficialDetails(computedOffer.offer)
            },
        )
    }
}

// ---------------------------------------------------------------------------
// SUB-COMPONENTS
// ---------------------------------------------------------------------------

@Composable
private fun DemonstrationNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InfoBlueTint)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Sample demonstration data",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Text(
                "These are illustrative offers, not live quotes.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun LoanCategorySelector(
    selectedCategory: LoanCategory,
    onSelect: (LoanCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Loan category",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
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
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        cat.shortLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selected) TextPrimary else TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonSummaryBar(
    principal: Double,
    tenureMonths: Int,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val summaryText = buildAnnotatedString {
            withStyle(SpanStyle(color = TextSecondary)) {
                append("Comparing ")
            }
            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                append("${Money.inr(principal)} over $tenureMonths months")
            }
        }
        Text(summaryText, style = MaterialTheme.typography.bodyMedium)

        Text(
            "Edit",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = EmeraldDark,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onEdit() }
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SearchAndFilterControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    methodFilter: MethodFilter,
    onCycleMethod: () -> Unit,
    sortOption: ComparisonSortOption,
    onSelectSort: (ComparisonSortOption) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Compact search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by lender, offer or keywords", style = MaterialTheme.typography.bodySmall, color = TextHint) },
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
            modifier = Modifier.fillMaxWidth().height(48.dp),
        )

        // Filter and Sort row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Method filter chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardWhite)
                    .border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(10.dp))
                    .clickable { onCycleMethod() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    methodFilter.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                )
            }

            // Sort dropdown
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
                        "Sort by ",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        sortOption.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
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
            // Label Column (Fixed width: 84.dp)
            Column(
                modifier = Modifier.width(90.dp),
            ) {
                TableCell(
                    text = "At a glance",
                    isHeader = true,
                    textColor = TextSecondary,
                    fontSize = 11,
                )
                HorizontalDivider(color = DividerColor)
                TableCell(text = "Rate", isHeader = false, textColor = TextSecondary)
                HorizontalDivider(color = DividerColor)
                TableCell(text = "Method", isHeader = false, textColor = TextSecondary)
                HorizontalDivider(color = DividerColor)
                TableCell(text = "Total", isHeader = false, textColor = TextSecondary)
            }

            // Data Columns for each selected offer
            offers.forEach { computed ->
                VerticalDivider(
                    color = DividerColor,
                    modifier = Modifier.height(176.dp),
                )

                Column(
                    modifier = Modifier.width(115.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header: Lender Name with remove icon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            computed.offer.lenderName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    HorizontalDivider(color = DividerColor)

                    // Rate
                    TableCell(
                        text = "${String.format(Locale.US, "%.2f", computed.offer.annualRatePercent)}%",
                        isHeader = false,
                        textColor = TextPrimary,
                        isBold = true,
                    )
                    HorizontalDivider(color = DividerColor)

                    // Method
                    TableCell(
                        text = if (computed.offer.interestMethod == InterestMethod.REDUCING_BALANCE) "Reducing" else "Flat rate",
                        isHeader = false,
                        textColor = TextPrimary,
                        isBold = true,
                    )
                    HorizontalDivider(color = DividerColor)

                    // Total
                    TableCell(
                        text = Money.inr(computed.totalCost),
                        isHeader = false,
                        textColor = if (computed.isLowestCost) EmeraldDark else TextPrimary,
                        isBold = true,
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
            if (selectedCount == 0) "Select at least 2 offers from the list below to compare side-by-side."
            else "1 offer selected. Select 1 more offer to enable side-by-side comparison.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OfferDiscoveryCard(
    offer: ComputedLoanOffer,
    onToggleCompare: () -> Unit,
    onOfficialDetails: () -> Unit,
    onViewDetails: () -> Unit,
) {
    val isLowest = offer.isLowestCost

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
        // Green Banner if lowest cost
        if (isLowest) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Emerald)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
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
                    "Lowest estimated total cost",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Row: Avatar, Lender Name & Offer, Rate Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Initial Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8EEF8)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        offer.offer.lenderInitial,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                        ),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        offer.offer.lenderName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Text(
                        offer.offer.offerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                // Rate Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SegmentTrack)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "${String.format(Locale.US, "%.2f", offer.offer.annualRatePercent)}% p.a.",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                }
            }

            HorizontalDivider(color = DividerColor)

            // 2-Column Specs Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                // Column 1
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpecItem(
                        label = "Interest method",
                        value = if (offer.offer.interestMethod == InterestMethod.REDUCING_BALANCE) "Reducing balance" else "Flat rate",
                    )
                    SpecItem(
                        label = "Monthly installment",
                        value = Money.inr(offer.installment),
                        valueBold = true,
                    )
                    SpecItem(
                        label = "Tenure",
                        value = "${offer.tenureMonths} months",
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Column 2
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpecItem(
                        label = "Processing fee",
                        value = offer.feeDisplay,
                    )
                    SpecItem(
                        label = "Indicative APR",
                        value = "${String.format(Locale.US, "%.2f", offer.indicativeApr)}%",
                        valueBold = true,
                    )
                    SpecItem(
                        label = "Sample eligibility",
                        value = offer.offer.eligibilityNotes,
                    )
                }
            }

            // Green Highlight Box: Estimated Total Cost
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFECFDF5))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Estimated total cost",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Text(
                        "Repayment + disclosed fee & GST",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }

                Text(
                    Money.inr(offer.totalCost),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    color = TextPrimary,
                )
            }

            // Official Lender Details Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardWhite)
                    .border(BorderStroke(1.dp, Outline), RoundedCornerShape(12.dp))
                    .clickable { onOfficialDetails() },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Official lender details",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Secondary Actions: Compare toggle + View details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Compare Selection Toggle
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
                        if (offer.isSelectedForComparison) "In comparison" else "+ Add to compare",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (offer.isSelectedForComparison) EmeraldDark else TextPrimary,
                        ),
                    )
                }

                // View Details
                Text(
                    "View calculations",
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
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// DIALOGS
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
                "Edit comparison terms",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Change the shared loan amount and tenure. All comparable offers will update automatically.",
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
                Text("Apply & Recalculate", color = EmeraldDark, fontWeight = FontWeight.Bold)
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
private fun OfficialLenderDetailsDialog(
    offer: LoanOffer,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue)
                Text(
                    "Sample demonstration offer",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Lender: ${offer.lenderName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text(
                    "Live banking API integrations and verified external web links are unavailable for illustrative hackathon offers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    "FinShield does not fabricate external application URLs or submit unverified credit requests on your behalf.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood", color = EmeraldDark, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun OfferDetailsDialog(
    computedOffer: ComputedLoanOffer,
    onDismiss: () -> Unit,
    onOfficialDetails: () -> Unit,
) {
    val o = computedOffer.offer

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    o.lenderName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text(
                    o.offerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DetailRow("Category", o.category.label)
                DetailRow("Annual Interest Rate", "${String.format(Locale.US, "%.2f", o.annualRatePercent)}% p.a.")
                DetailRow(
                    "Interest Calculation Method",
                    if (o.interestMethod == InterestMethod.REDUCING_BALANCE) "Reducing balance" else "Flat rate",
                )
                DetailRow("Shared Principal Amount", Money.inr(computedOffer.principal))
                DetailRow("Comparison Tenure", "${computedOffer.tenureMonths} months")
                HorizontalDivider(color = DividerColor)
                DetailRow("Monthly Installment (EMI)", Money.inr(computedOffer.installment), isBold = true)
                DetailRow("Total Repayment Amount", Money.inr(computedOffer.totalRepayment))
                DetailRow("Total Interest Paid", Money.inr(computedOffer.totalInterest))
                DetailRow("Processing Fee & GST", computedOffer.feeDisplay)
                DetailRow("Net Fee with 18% GST", Money.inr(computedOffer.feeWithGst))
                HorizontalDivider(color = DividerColor)
                DetailRow(
                    "Estimated Total Borrowing Cost",
                    Money.inr(computedOffer.totalCost),
                    isBold = true,
                    highlightColor = EmeraldDark,
                )
                DetailRow("Indicative APR", "${String.format(Locale.US, "%.2f", computedOffer.indicativeApr)}%")
                DetailRow("Eligibility Notes", o.eligibilityNotes)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onOfficialDetails) {
                Text("Lender Info", color = EmeraldDark, fontWeight = FontWeight.Bold)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
            modifier = Modifier.weight(1f),
        )
    }
}
