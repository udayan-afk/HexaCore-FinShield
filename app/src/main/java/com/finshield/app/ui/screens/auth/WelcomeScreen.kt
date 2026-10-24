package com.finshield.app.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.LinkText
import com.finshield.app.ui.components.PrimaryButton
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onHaveAccount: () -> Unit) {
    StatusBarIcons(darkIcons = false)
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        // Navy hero with a diagonal lower edge
        Canvas(Modifier.fillMaxWidth().fillMaxHeight(0.62f)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.82f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, Navy)
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Lock, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                Text("Built to protect borrowers", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(36.dp))
            Box(Modifier.size(92.dp).background(Color.White, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                ShieldLogo(size = 58.dp)
            }
            Spacer(Modifier.height(18.dp))
            Text("FINSHIELD", color = EmeraldSoft, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(14.dp))
            Text(
                "Understand before\nyou borrow.",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Understand your loan. Verify your repayment. Borrow with confidence.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            FeatureRow(Icons.Filled.Description, "Clear, clause-by-clause insights", "Know exactly what you are signing")
            Spacer(Modifier.height(14.dp))
            FeatureRow(Icons.Filled.Calculate, "Independent repayment check", "Verify lender calculations in seconds")
            Spacer(Modifier.height(32.dp))
            PrimaryButton("Get Started", onClick = onGetStarted, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
            Spacer(Modifier.height(6.dp))
            LinkText("I already have an account", onClick = onHaveAccount)
            Spacer(Modifier.height(4.dp))
            DisclaimerText()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(44.dp).background(EmeraldTint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = EmeraldDark, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
        }
    }
}
