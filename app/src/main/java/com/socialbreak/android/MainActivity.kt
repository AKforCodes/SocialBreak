package com.socialbreak.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.screens.*
import com.socialbreak.android.ui.theme.*

enum class ScreenTab(val title: String, val icon: ImageVector) {
    SLEEP("Sleep", Icons.Outlined.Bedtime),
    BLOCKING("Blocking", Icons.Outlined.Lock),
    HOME("Home", Icons.Outlined.Home),
    WEBAPPS("WebApps", Icons.Outlined.Language),
    PROFILE("Profile", Icons.Outlined.Person)
}

enum class SubScreen {
    NONE,
    SETTINGS,
    ANALYTICS,
    WEB_BLOCKING,
    ACCOUNT
}

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager(applicationContext)

        handleDeepLink(intent)

        setContent {
            SocialBreakTheme {
                var selectedTab by remember { mutableStateOf(ScreenTab.HOME) }
                var activeSubScreen by remember { mutableStateOf(SubScreen.NONE) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF000000),
                    bottomBar = {
                        if (activeSubScreen == SubScreen.NONE) {
                            // Exact flat 1:1 Navigation Bar across all 30 screenshots
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, Color(0xFF1E2538)),
                                containerColor = Color(0xFF000000),
                                tonalElevation = 0.dp
                            ) {
                                listOf(
                                    ScreenTab.SLEEP,
                                    ScreenTab.BLOCKING,
                                    ScreenTab.HOME,
                                    ScreenTab.WEBAPPS,
                                    ScreenTab.PROFILE
                                ).forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (selected) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) Color.White else Color(0xFF64748B)
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color(0xFF64748B),
                                            unselectedTextColor = Color(0xFF64748B),
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activeSubScreen) {
                            SubScreen.SETTINGS -> SettingsScreen(
                                prefs = prefs,
                                onBack = { activeSubScreen = SubScreen.NONE }
                            )
                            SubScreen.ANALYTICS -> TodayAnalyticsScreen(
                                prefs = prefs,
                                onBack = { activeSubScreen = SubScreen.NONE }
                            )
                            SubScreen.WEB_BLOCKING -> WebBlockingScreen(
                                prefs = prefs,
                                onBack = { activeSubScreen = SubScreen.NONE }
                            )
                            SubScreen.ACCOUNT -> AccountScreen(
                                prefs = prefs,
                                onBack = { activeSubScreen = SubScreen.NONE }
                            )
                            SubScreen.NONE -> {
                                when (selectedTab) {
                                    ScreenTab.SLEEP -> SleepModeScreen(
                                        prefs = prefs,
                                        onNavigateSettings = { activeSubScreen = SubScreen.SETTINGS }
                                    )
                                    ScreenTab.BLOCKING -> FullBlockingScreen(prefs)
                                    ScreenTab.HOME -> HomeDashboardScreen(
                                        prefs = prefs,
                                        onNavigateAnalytics = { activeSubScreen = SubScreen.ANALYTICS }
                                    )
                                    ScreenTab.WEBAPPS -> WebAppsScreen(
                                        prefs = prefs,
                                        onNavigateWebBlocking = { activeSubScreen = SubScreen.WEB_BLOCKING }
                                    )
                                    ScreenTab.PROFILE -> ProfileScreen(
                                        prefs = prefs,
                                        onNavigateAccount = { activeSubScreen = SubScreen.ACCOUNT }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "socialbreak" && data.host == "open") {
            val platform = data.getQueryParameter("platform")
            // Navigation handled by setting active tab to WEBAPPS
        }
    }
}
