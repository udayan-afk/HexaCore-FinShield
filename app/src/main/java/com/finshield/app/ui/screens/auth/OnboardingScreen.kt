package com.finshield.app.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.ui.components.PrimaryButton
import com.finshield.app.ui.components.SecondaryButton
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.Emerald
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.Outline
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val panelBackground: Color,
    val ringColor: Color,
    val tileBackground: Color,
    val icon: ImageVector,
    val eyebrow: String = "PROTECTION, MADE SIMPLE",
    val title: String,
    val description: String,
)

val onboardingPages = listOf(
    OnboardingPageData(
        panelBackground = Color(0xFFE8F7F0), // Pale mint
        ringColor = Color(0xFFC7EBD9),
        tileBackground = Navy,
        icon = Icons.Filled.Description,
        title = "Understand your loan agreement.",
        description = "Turn complex financial clauses into clear, simple explanations you can act on.",
    ),
    OnboardingPageData(
        panelBackground = Color(0xFFEAF3FC), // Pale blue
        ringColor = Color(0xFFCDE2F8),
        tileBackground = Color(0xFF1E40AF),
        icon = Icons.Filled.Calculate,
        title = "Verify every repayment figure.",
        description = "Independently check your EMI, interest and total repayment before you sign.",
    ),
    OnboardingPageData(
        panelBackground = Color(0xFFFEF5E7), // Pale warm cream
        ringColor = Color(0xFFFBE5C3),
        tileBackground = Emerald,
        icon = Icons.Filled.SwapHoriz,
        title = "Spot risks. Compare with confidence.",
        description = "Review potential charges and compare the true cost of sample loan offers.",
    ),
)

@Composable
fun OnboardingScreen(
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    StatusBarIcons(darkIcons = true)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    // Handle back button on device: Page 2 & 3 go to previous page; Page 1 exits to Welcome
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Navigation: Back button on the left, Skip on the right in emerald
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    } else {
                        onBack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Text(
                text = "Skip",
                color = EmeraldDark,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Pager with content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = onboardingPages[pageIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Illustration Panel with concentric rings, central icon tile, and floating chips
                OnboardingIllustrationPanel(page = page)

                Spacer(Modifier.height(12.dp))

                // Eyebrow
                Text(
                    text = page.eyebrow,
                    color = EmeraldDark,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Heading
                Text(
                    text = page.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    )
                )

                // Description
                Text(
                    text = page.description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Pagination Indicators (first, second, third active based on page)
                OnboardingPaginationIndicators(
                    pageCount = onboardingPages.size,
                    currentPage = pagerState.currentPage
                )

                Spacer(Modifier.height(16.dp))
            }
        }

        // Bottom Actions based on current page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            when (pagerState.currentPage) {
                0 -> {
                    // Screen 1: Full-width Next button with right arrow
                    PrimaryButton(
                        text = "Next",
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
                    )
                }
                1 -> {
                    // Screen 2: Back button on the left, Emerald Next button on the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecondaryButton(
                            text = "Back",
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            text = "Next",
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            },
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.weight(1.35f)
                        )
                    }
                }
                2 -> {
                    // Screen 3: Back button on the left, Emerald Get Started button on the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecondaryButton(
                            text = "Back",
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            text = "Get Started",
                            onClick = onFinish,
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.weight(1.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingIllustrationPanel(page: OnboardingPageData, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(270.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .background(page.panelBackground, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Concentric soft circular rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            // Outer ring
            drawCircle(
                color = page.ringColor.copy(alpha = 0.55f),
                radius = size.height * 0.44f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Middle ring
            drawCircle(
                color = page.ringColor.copy(alpha = 0.85f),
                radius = size.height * 0.32f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Inner soft filled circle
            drawCircle(
                color = page.ringColor.copy(alpha = 0.4f),
                radius = size.height * 0.22f,
                center = center
            )
        }

        // Central rounded square with icon
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.16f)
                )
                .background(page.tileBackground, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        // Floating Chip 1: Top-Right (Check icon + "Clear terms")
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 16.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .background(Color.White, RoundedCornerShape(50))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(EmeraldTint, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Text(
                    text = "Clear terms",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = TextPrimary
                )
            }
        }

        // Floating Chip 2: Bottom-Left ("₹ EMI verified")
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 18.dp, start = 16.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .background(Color.White, RoundedCornerShape(50))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = EmeraldDark
                )
                Text(
                    text = "EMI verified",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun OnboardingPaginationIndicators(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isActive) 24.dp else 8.dp,
                animationSpec = tween(durationMillis = 250),
                label = "indicatorWidth"
            )
            val color by animateColorAsState(
                targetValue = if (isActive) EmeraldButton else Outline.copy(alpha = 0.6f),
                animationSpec = tween(durationMillis = 250),
                label = "indicatorColor"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}
