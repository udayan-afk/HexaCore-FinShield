package com.finshield.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.Emerald
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.ErrorRed
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Outline
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

/** The FinShield shield-with-check mark. Drawn in code so it scales crisply. */
@Composable
fun ShieldLogo(size: Dp, modifier: Modifier = Modifier, shieldColor: Color = EmeraldButton, checkColor: Color = Color.White) {
    Canvas(modifier = modifier.size(size).semantics { contentDescription = "FinShield logo" }) {
        val w = this.size.width
        val h = this.size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.04f)
            lineTo(w * 0.92f, h * 0.2f)
            lineTo(w * 0.92f, h * 0.52f)
            cubicTo(w * 0.92f, h * 0.76f, w * 0.74f, h * 0.9f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.26f, h * 0.9f, w * 0.08f, h * 0.76f, w * 0.08f, h * 0.52f)
            lineTo(w * 0.08f, h * 0.2f)
            close()
        }
        drawPath(shield, shieldColor)
        val check = Path().apply {
            moveTo(w * 0.3f, h * 0.5f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.72f, h * 0.36f)
        }
        drawPath(check, checkColor, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Full-width emerald call-to-action. Shows a spinner while [loading]. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    trailingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    val active = enabled && !loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (active) 10.dp else 0.dp, shape, ambientColor = EmeraldButton, spotColor = EmeraldButton)
            .background(if (active) EmeraldButton else EmeraldButton.copy(alpha = 0.5f), shape)
            .clickable(enabled = active, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
                if (trailingIcon != null) {
                    Icon(trailingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Compact text link in the emerald brand colour. */
@Composable
fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = EmeraldDark,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    )
}

/** Labelled, rounded input matching the reference design. Supports password toggle and inline errors. */
@Composable
fun FinTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    prefix: String? = null,
    suffix: String? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            isError = error != null,
            placeholder = { Text(placeholder, color = TextHint, style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            shape = RoundedCornerShape(16.dp),
            prefix = prefix?.let { { Text(it, color = TextPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) } },
            suffix = suffix?.let { { Text(it, color = TextSecondary, style = MaterialTheme.typography.bodyMedium) } },
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "Hide password" else "Show password",
                            tint = TextSecondary,
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
                disabledContainerColor = CardWhite,
                focusedBorderColor = Emerald,
                unfocusedBorderColor = Outline,
                errorBorderColor = ErrorRed,
                cursorColor = Emerald,
            ),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
        )
        if (error != null) {
            Text(
                error,
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

/** White app bar with back arrow, title and subtitle, followed by a hairline divider. */
@Composable
fun FinTopBar(title: String, subtitle: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, leading: @Composable (() -> Unit)? = null) {
    Column(modifier = modifier.fillMaxWidth().background(CardWhite)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            } else {
                Spacer(Modifier.size(16.dp))
            }
            if (leading != null) {
                leading()
                Spacer(Modifier.size(10.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
            }
        }
        HorizontalDivider(color = DividerColor)
    }
}

/** Soft blue informational banner. */
@Composable
fun InfoBanner(title: String, message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoBlueTint, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(18.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Text(message, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextSecondary)
        }
    }
}

/** White rounded card with a hairline border. */
@Composable
fun FinCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(20.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) { content() }
}

/** Small disclaimer used under primary flows. */
@Composable
fun DisclaimerText(modifier: Modifier = Modifier, color: Color = TextHint) {
    Text(
        "FinShield provides informational estimates, not legal, lending or financial advice.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        color = color,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
