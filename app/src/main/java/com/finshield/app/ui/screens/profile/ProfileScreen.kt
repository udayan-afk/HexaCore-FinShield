package com.finshield.app.ui.screens.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.data.auth.Session
import com.finshield.app.data.auth.greetingName
import com.finshield.app.ui.components.DisclaimerText
import com.finshield.app.ui.components.FinCard
import com.finshield.app.ui.components.IconTile
import com.finshield.app.ui.components.ScreenScaffold
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.ErrorRed
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

/**
 * Constants for official support contacts and regulatory disclaimers.
 */
object ContactSupportConfig {
    const val PHONE_NUMBER_DISPLAY = "+91 9689718945"
    const val PHONE_NUMBER_TEL_URI = "tel:+919689718945"
    const val GUIDANCE_DISCLAIMER =
        "Contact our team for FinShield support and general guidance on understanding loan terms and financial concepts. For formal legal advice, credit bureau disputes or lender-specific decisions, consult the relevant qualified professional or official institution."
    const val PRIVACY_REMINDER =
        "Security Reminder: Never share your OTP, UPI PIN, ATM PIN, passwords, or complete banking credentials with anyone over the phone."
}

private enum class InfoDialog { ABOUT, PRIVACY }

@Composable
fun ProfileScreen(
    session: Session,
    onHistory: () -> Unit,
    onSignOut: () -> Unit,
    onAssistant: () -> Unit = {},
) {
    StatusBarIcons(darkIcons = true)
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<InfoDialog?>(null) }
    var dialErrorDialogMessage by remember { mutableStateOf<String?>(null) }

    val name = session.greetingName()
    val email = (session as? Session.Authenticated)?.email ?: ""

    ScreenScaffold(title = "Profile", subtitle = "Account and settings", onBack = null, handleNavBarInset = false) {
        // 1. User Profile Card
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

        // 2. FinShield Assistant Chatbot Entry Point
        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(EmeraldTint, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            ShieldLogo(size = 28.dp)
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(EmeraldButton, CircleShape)
                                    .border(1.dp, CardWhite, CircleShape),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
                                    "FINSHIELD GUIDE",
                                    color = EmeraldDark,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                    ),
                                )
                            }
                        }

                        Text(
                            text = "FinShield Assistant",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                        )
                        Text(
                            text = "Get help with loans, EMIs, CIBIL and banking",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Navy, RoundedCornerShape(14.dp))
                        .clickable(onClick = onAssistant),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = EmeraldSoft,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ask Assistant",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // 3. Contact Us Section with Direct Dialing
        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(Icons.Filled.SupportAgent, EmeraldDark, EmeraldTint, size = 44)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contact Us",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                        )
                        Text(
                            text = "Need help understanding your loan or using FinShield? Contact our team.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary,
                        )
                    }
                }

                // Phone Number and Call Team Button Container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppBackground, RoundedCornerShape(16.dp))
                        .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "TELEPHONE SUPPORT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = TextSecondary,
                            ),
                        )
                        Text(
                            text = ContactSupportConfig.PHONE_NUMBER_DISPLAY,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.3.sp,
                            ),
                        )
                    }

                    // Call Team action using Intent.ACTION_DIAL
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .background(EmeraldButton, RoundedCornerShape(12.dp))
                            .clickable {
                                val dialIntent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse(ContactSupportConfig.PHONE_NUMBER_TEL_URI),
                                )
                                try {
                                    context.startActivity(dialIntent)
                                } catch (e: ActivityNotFoundException) {
                                    dialErrorDialogMessage =
                                        "No telephone dialer app found on this device to place a call to ${ContactSupportConfig.PHONE_NUMBER_DISPLAY}."
                                }
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Call Team",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                        )
                    }
                }

                // Privacy reminder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberTint, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = AmberDark,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = ContactSupportConfig.PRIVACY_REMINDER,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = AmberDark,
                    )
                }

                // Financial and legal guidance disclaimer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InfoBlueTint, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = InfoBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = ContactSupportConfig.GUIDANCE_DISCLAIMER,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = TextPrimary,
                    )
                }
            }
        }

        // 4. Settings and Information Card
        FinCard {
            Column {
                SettingsRow(Icons.Filled.History, "Analysis history", "View saved analyses", onHistory)
                RowDivider()
                SettingsRow(Icons.Filled.Info, "About FinShield", "Purpose and disclaimer") { dialog = InfoDialog.ABOUT }
                RowDivider()
                SettingsRow(Icons.Filled.Policy, "Privacy & Cloud", "How your account and documents are handled") { dialog = InfoDialog.PRIVACY }
            }
        }

        // 5. Sign Out Button
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

        Text(
            "FinShield 0.1.0 · Firebase Connected",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
        DisclaimerText()
    }

    when (dialog) {
        InfoDialog.ABOUT -> InfoAlert(
            "About FinShield",
            "FinShield helps first-time borrowers understand loan offers: it verifies EMI figures, compares offers and reads loan agreements.\n\n" +
                "FinShield provides informational estimates, not legal, lending or financial advice.",
        ) { dialog = null }
        InfoDialog.PRIVACY -> InfoAlert(
            "Privacy & Cloud",
            "Authentication and user profiles are securely managed via Google Firebase Authentication and Cloud Firestore.\n\n" +
                "Your account profile is protected by strict per-user Firestore security rules.",
        ) { dialog = null }
        null -> Unit
    }

    // Dialing error dialog if no dialer activity is found on the device
    if (dialErrorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { dialErrorDialogMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Call, null, tint = EmeraldDark)
                    Text("Dialer Unavailable", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Text(
                    text = dialErrorDialogMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { dialErrorDialogMessage = null }) {
                    Text("OK", color = EmeraldDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardWhite,
        )
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
