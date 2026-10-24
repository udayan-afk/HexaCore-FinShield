package com.finshield.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.TextSecondary

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, "Home", Icons.Filled.Home),
    Tab(Routes.ANALYZE, "Analyze", Icons.Filled.Description),
    Tab(Routes.COMPARE, "Compare", Icons.Filled.SwapHoriz),
    Tab(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

@Composable
fun FinBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = CardWhite) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldDark,
                    selectedTextColor = EmeraldDark,
                    indicatorColor = EmeraldTint,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                ),
            )
        }
    }
}
