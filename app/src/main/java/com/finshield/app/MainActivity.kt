package com.finshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finshield.app.ui.navigation.FinShieldNavHost
import com.finshield.app.ui.theme.FinShieldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinShieldTheme {
                FinShieldNavHost()
            }
        }
    }
}
