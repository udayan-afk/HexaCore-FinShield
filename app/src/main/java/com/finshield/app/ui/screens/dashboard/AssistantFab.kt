package com.finshield.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.Navy

/**
 * Compact floating action button for the FinShield Assistant on the Home Dashboard.
 * Positioned cleanly near bottom-right without obscuring primary navigation or content.
 */
@Composable
fun AssistantFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .shadow(8.dp, shape, ambientColor = Navy.copy(alpha = 0.25f), spotColor = Navy.copy(alpha = 0.35f))
            .background(Navy, shape)
            .border(1.5.dp, EmeraldSoft.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "Open FinShield Assistant FAQ chatbot" },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                ShieldLogo(size = 24.dp)
                // Small online status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(EmeraldButton, CircleShape)
                        .border(1.dp, Navy, CircleShape),
                )
            }

            Text(
                text = "Assistant",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    letterSpacing = 0.2.sp,
                ),
            )
        }
    }
}
