package com.finshield.app.ui.screens.dashboard

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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.domain.credit.CreditScoreData
import com.finshield.app.domain.credit.CreditScoreStatus
import com.finshield.app.ui.components.CreditScoreGauge
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.Emerald
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

/**
 * Prominent, responsive Credit Health card on the FinShield dashboard.
 * Uses FinShield navy, emerald, and cyan visual identity.
 * Does NOT invent or show fake scores when unverified.
 */
@Composable
fun CreditHealthCard(
    creditScoreData: CreditScoreData,
    onCheckScore: () -> Unit,
    onCheckEligibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val hasVerifiedScore = creditScoreData.score != null && creditScoreData.status == CreditScoreStatus.VERIFIED

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Navy.copy(alpha = 0.12f), spotColor = Navy.copy(alpha = 0.16f))
            .background(CardWhite, shape)
            .border(1.dp, DividerColor, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top row: Header, subtitle & Gauge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .background(EmeraldTint, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "CREDIT BUREAU & FOIR",
                            color = EmeraldDark,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                        )
                    }
                }

                Text(
                    text = "Credit Health",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )

                Text(
                    text = "Understand your credit. Borrow smarter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Spacer(Modifier.height(2.dp))

                // Score status & source label
                if (hasVerifiedScore) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (creditScoreData.isOfficialBureauScore) Icons.Filled.Verified else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (creditScoreData.isOfficialBureauScore) EmeraldDark else InfoBlue,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (creditScoreData.isOfficialBureauScore) {
                                "Official ${creditScoreData.provider}"
                            } else {
                                creditScoreData.provider
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            color = TextPrimary,
                        )
                    }
                    if (!creditScoreData.retrievalDate.isNullOrBlank()) {
                        Text(
                            text = "Updated: ${creditScoreData.retrievalDate}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                        )
                    }
                } else {
                    Text(
                        text = "Official bureau score not fetched yet",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
            }

            // Compact Gauge
            CreditScoreGauge(
                score = creditScoreData.score,
                size = 96.dp,
                strokeWidth = 9.dp,
            )
        }

        HorizontalDivider(color = DividerColor.copy(alpha = 0.8f))

        // Actions: Primary and Secondary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Primary Action: "Check CIBIL Score"
            Row(
                modifier = Modifier
                    .weight(1.15f)
                    .height(46.dp)
                    .background(Navy, RoundedCornerShape(14.dp))
                    .clickable(onClick = onCheckScore)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = EmeraldSoft,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (hasVerifiedScore) "View CIBIL Score" else "Check CIBIL Score",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    maxLines = 1,
                )
            }

            // Secondary Action: "Check Loan Eligibility"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .background(InfoBlueTint, RoundedCornerShape(14.dp))
                    .border(1.dp, InfoBlue.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onCheckEligibility)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Calculate,
                    contentDescription = null,
                    tint = InfoBlue,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Loan Eligibility",
                    color = InfoBlue,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }

        // Credit Health Tip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EmeraldTint.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = EmeraldDark,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = "Keeping credit utilization under 30% and paying EMIs on time protects your score.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = EmeraldDark,
            )
        }
    }
}
