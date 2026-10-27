package com.socialbreak.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.screens.*
import com.socialbreak.android.ui.theme.*

enum class ScreenTab(val title: String, val icon: ImageVector) {
    SLEEP("Sleep", Icons.Default.Bedtime),
    BLOCKING("Blocking", Icons.Default.Lock),
    HOME("Home", Icons.Default.Home),
    WEBAPPS("WebApps", Icons.Default.Public),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager(applicationContext)

        setContent {
            SocialBreakTheme {
                var selectedTab by remember { mutableStateOf(ScreenTab.HOME) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BgPrimary,
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                            containerColor = BgSecondary
                        ) {
                            listOf(
                                ScreenTab.SLEEP,
                                ScreenTab.BLOCKING,
                                ScreenTab.HOME,
                                ScreenTab.WEBAPPS,
                                ScreenTab.ANALYTICS,
                                ScreenTab.SETTINGS
                            ).forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (selected) AccentBlue else TextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) AccentBlue else TextMuted
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = BgTertiary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (selectedTab) {
                            ScreenTab.SLEEP -> SleepModeScreen(prefs)
                            ScreenTab.BLOCKING -> FullBlockingScreen(prefs)
                            ScreenTab.HOME -> HomeDashboardScreen(prefs)
                            ScreenTab.WEBAPPS -> WebAppsScreen(prefs)
                            ScreenTab.ANALYTICS -> TodayAnalyticsScreen(prefs)
                            ScreenTab.SETTINGS -> SettingsScreen(prefs)
                        }
                    }
                }
            }
        }
    }
}
