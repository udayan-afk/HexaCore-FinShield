package com.finshield.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.domain.credit.CreditScoreBand
import com.finshield.app.ui.theme.Amber
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.Emerald
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.NavyLight
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-precision circular arc gauge for displaying credit scores (300 to 900).
 * Supports compact (home card) and expanded (detail screen) modes.
 */
@Composable
fun CreditScoreGauge(
    score: Int?,
    minScore: Int = 300,
    maxScore: Int = 900,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 14.dp,
    isDarkTheme: Boolean = false,
) {
    val targetProgress = if (score != null) {
        ((score.coerceIn(minScore, maxScore) - minScore).toFloat() / (maxScore - minScore)).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(targetProgress) {
        animatedProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    val band = score?.let { CreditScoreBand.fromScore(it) }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        val trackColor = if (isDarkTheme) NavyLight else Color(0xFFE2E8F0)

        // Arc gauge sweep angles: Start at 145 degrees, sweep 250 degrees
        val startAngle = 145f
        val totalSweep = 250f

        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // 1. Background full track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // 2. Active colored progress arc
            if (animatedProgress.value > 0.01f) {
                val progressSweep = totalSweep * animatedProgress.value
                val gradientBrush = Brush.sweepGradient(
                    0.0f to Rose,
                    0.35f to Amber,
                    0.65f to InfoBlue,
                    1.0f to Emerald,
                )

                drawArc(
                    brush = gradientBrush,
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            if (score != null) {
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.22f).sp,
                        letterSpacing = (-1).sp,
                    ),
                    color = if (isDarkTheme) Color.White else TextPrimary,
                )
                if (band != null) {
                    Box(
                        modifier = Modifier
                            .background(band.badgeBackground, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = band.label.uppercase(),
                            color = band.badgeContent,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.24f).sp,
                    ),
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else TextSecondary,
                )
                Text(
                    text = "Not checked",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else TextSecondary,
                )
            }
        }
    }
}
