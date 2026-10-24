package com.finshield.app.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.Navy
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    StatusBarIcons(darkIcons = false)
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f)
        delay(900)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.alpha(alpha.value)) {
            ShieldLogo(size = 88.dp)
            Spacer(Modifier.height(16.dp))
            Text("FINSHIELD", color = EmeraldSoft, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = EmeraldSoft, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}
