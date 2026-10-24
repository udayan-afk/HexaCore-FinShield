package com.finshield.app.ui.screens.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finshield.app.data.auth.Session
import com.finshield.app.data.auth.greetingName
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.IconTile
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.ErrorRed
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.RoseTint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

private enum class InfoDialog { ABOUT, PRIVACY }

@Composable
fun ProfileScreen(session: Session, onHistory: () -> Unit, onSignOut: () -> Unit) {
    StatusBarIcons(darkIcons = true)
    var dialog by rememberSaveable { mutableStateOf<InfoDialog?>(null) }
    val name = session.greetingName()
    val email = (session as? Session.Authenticated)?.email ?: ""

    ScreenScaffold(title = "Profile", subtitle = "Account and settings", onBack = null, handleNavBarInset = false) {
        FinCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).background(EmeraldTint, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Text(
                        name.take(2).uppercase(),
                        color = Navy,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(email.ifBlank { "Firebase Authenticated User" }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(EmeraldTint, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(EmeraldDark, RoundedCornerShape(50))
                        )
                        Text(
                            "Firebase Account",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = EmeraldDark,
                        )
                    }
                }
            }
        }

        FinCard {
            Column {
                SettingsRow(Icons.Filled.History, "Analysis history", "View saved analyses", onHistory)
                RowDivider()
                SettingsRow(Icons.Filled.Info, "About FinShield", "Purpose and disclaimer") { dialog = InfoDialog.ABOUT }
                RowDivider()
                SettingsRow(Icons.Filled.Policy, "Privacy & Cloud", "How your account and documents are handled") { dialog = InfoDialog.PRIVACY }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(18.dp))
                .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .clickable(onClick = onSignOut)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = ErrorRed)
            Text(
                "Sign out",
                style = MaterialTheme.typography.titleSmall,
                color = ErrorRed,
            )
        }

        Text("FinShield 0.1.0 · Firebase Connected", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.fillMaxWidth())
        DisclaimerText()
    }

    when (dialog) {
        InfoDialog.ABOUT -> InfoAlert(
            "About FinShield",
            "FinShield helps first-time borrowers understand loan offers: it verifies EMI figures, compares offers and (in a later build) reads loan agreements.\n\n" +
                "FinShield provides informational estimates, not legal, lending or financial advice.",
        ) { dialog = null }
        InfoDialog.PRIVACY -> InfoAlert(
            "Privacy & Cloud",
            "Authentication and user profiles are securely managed via Google Firebase Authentication and Cloud Firestore.\n\n" +
                "Your account profile is protected by strict per-user Firestore security rules.",
        ) { dialog = null }
        null -> Unit
    }
}

@Composable
private fun InfoAlert(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { ShieldLogo(size = 40.dp) },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = EmeraldDark) } },
        containerColor = CardWhite,
    )
}

@Composable
private fun RowDivider() {
    androidx.compose.material3.HorizontalDivider(color = DividerColor)
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, InfoBlue, InfoBlueTint, size = 40)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSecondary)
    }
}
