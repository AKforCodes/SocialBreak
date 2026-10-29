package com.socialbreak.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.AppShortcutManager
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.ui.components.SocialLiteRibbonIcon
import com.socialbreak.android.ui.screens.*
import com.socialbreak.android.ui.theme.SocialBreakTheme
import com.socialbreak.android.ui.theme.SocialTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ScreenTab(val label: String, val icon: ImageVector?) {
    SLEEP("Sleep", Icons.Outlined.Bedtime),
    BLOCKING("Blocking", Icons.Outlined.Shield),
    HOME("Home", Icons.Outlined.Home),
    WEBAPPS("WebApps", null), // Uses custom SocialLite 'S' ribbon icon
    PROFILE("Profile", Icons.Outlined.Person)
}

enum class SubRoute {
    NONE,
    WEB_BLOCKING,
    TODAY_ANALYTICS,
    SETTINGS,
    ACCOUNT
}

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager
    private val pendingIntentFlow = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsManager = PreferencesManager(applicationContext)
        pendingIntentFlow.value = intent

        setContent {
            val prefs by prefsManager.preferencesFlow.collectAsState(initial = SocialBreakPreferences())
            val latestIntent by pendingIntentFlow.collectAsState()

            SocialBreakTheme(
                themeMode = prefs.themeMode,
                globalGrayscale = prefs.globalGrayscale
            ) {
                SocialBreakMainApp(
                    prefs = prefs,
                    prefsManager = prefsManager,
                    incomingIntent = latestIntent,
                    onConsumeIntent = { pendingIntentFlow.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentFlow.value = intent
    }
}

@Composable
fun SocialBreakMainApp(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    incomingIntent: Intent? = null,
    onConsumeIntent: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val palette = SocialTheme.colors

    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var currentSubRoute by remember { mutableStateOf(SubRoute.NONE) }
    var initialWebAppPlatform by remember { mutableStateOf<String?>(null) }
    var didAutoLaunchCheck by remember { mutableStateOf(false) }

    // Handle Auto-Launch on first cold open
    LaunchedEffect(prefs.autoLaunchEnabled, prefs.autoLaunchPlatform) {
        if (!didAutoLaunchCheck && prefs.autoLaunchEnabled) {
            didAutoLaunchCheck = true
            initialWebAppPlatform = prefs.autoLaunchPlatform
            currentTab = ScreenTab.WEBAPPS
        }
    }

    // Handle Deep Links (socialbreak://open?platform=...) and App Shortcuts
    LaunchedEffect(incomingIntent) {
        val intent = incomingIntent ?: return@LaunchedEffect
        val action = intent.action
        val data = intent.data
        when {
            data != null && data.scheme == "socialbreak" -> {
                val platform = data.getQueryParameter("platform")?.lowercase() ?: "instagram"
                currentSubRoute = SubRoute.NONE
                initialWebAppPlatform = platform
                currentTab = ScreenTab.WEBAPPS
                onConsumeIntent()
            }
            action == AppShortcutManager.ACTION_START_FOCUS_25 -> {
                scope.launch { prefsManager.startTimedBlock(25, untilStopped = false) }
                currentSubRoute = SubRoute.NONE
                currentTab = ScreenTab.BLOCKING
                onConsumeIntent()
            }
            action == AppShortcutManager.ACTION_OPEN_ANALYTICS -> {
                currentSubRoute = SubRoute.TODAY_ANALYTICS
                onConsumeIntent()
            }
        }
    }

    BackHandler(enabled = currentSubRoute != SubRoute.NONE) {
        currentSubRoute = SubRoute.NONE
    }

    val isBubbleNav = prefs.navigationStyle == "bubble"
    var bubbleOffsetX by remember { mutableFloatStateOf(0f) }
    var bubbleOffsetY by remember { mutableFloatStateOf(0f) }
    var bubbleExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = palette.background,
        bottomBar = {
            if (!isBubbleNav) {
                // Authentic Floating Pill Bottom Navigation Bar matching Views 1–30
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .shadow(
                                elevation = if (palette.isDark) 0.dp else 6.dp,
                                shape = RoundedCornerShape(28.dp)
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(palette.navBarBackground)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScreenTab.values().forEach { tab ->
                            val isSelected = currentTab == tab && currentSubRoute == SubRoute.NONE
                            val itemColor = if (isSelected) palette.navBarSelectedText else palette.navBarUnselectedText

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        currentSubRoute = SubRoute.NONE
                                        currentTab = tab
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (tab == ScreenTab.WEBAPPS) {
                                    SocialLiteRibbonIcon(
                                        size = 20.dp,
                                        tint = itemColor
                                    )
                                } else if (tab.icon != null) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = itemColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = tab.label,
                                    color = itemColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(palette.background)
        ) {
            when (currentSubRoute) {
                SubRoute.WEB_BLOCKING -> {
                    WebBlockingScreen(
                        prefs = prefs,
                        prefsManager = prefsManager,
                        onBack = { currentSubRoute = SubRoute.NONE }
                    )
                }
                SubRoute.TODAY_ANALYTICS -> {
                    TodayAnalyticsScreen(
                        prefs = prefs,
                        onBack = { currentSubRoute = SubRoute.NONE }
                    )
                }
                SubRoute.SETTINGS -> {
                    SettingsScreen(
                        prefs = prefs,
                        prefsManager = prefsManager,
                        onBack = { currentSubRoute = SubRoute.NONE },
                        onOpenAccount = { currentSubRoute = SubRoute.ACCOUNT }
                    )
                }
                SubRoute.ACCOUNT -> {
                    AccountScreen(
                        prefs = prefs,
                        prefsManager = prefsManager,
                        onBack = { currentSubRoute = SubRoute.NONE }
                    )
                }
                SubRoute.NONE -> {
                    when (currentTab) {
                        ScreenTab.SLEEP -> SleepModeScreen(
                            prefs = prefs,
                            prefsManager = prefsManager
                        )
                        ScreenTab.BLOCKING -> FullBlockingScreen(
                            prefs = prefs,
                            prefsManager = prefsManager,
                            onNavigateSettings = { currentSubRoute = SubRoute.SETTINGS }
                        )
                        ScreenTab.HOME -> HomeDashboardScreen(
                            prefs = prefs,
                            prefsManager = prefsManager,
                            onOpenCleanWebApp = { platformId ->
                                initialWebAppPlatform = platformId
                                currentTab = ScreenTab.WEBAPPS
                            },
                            onNavigateWebBlocking = { currentSubRoute = SubRoute.WEB_BLOCKING },
                            onNavigateTodayStats = { currentSubRoute = SubRoute.TODAY_ANALYTICS }
                        )
                        ScreenTab.WEBAPPS -> WebAppsScreen(
                            prefs = prefs,
                            prefsManager = prefsManager,
                            initialPlatformId = initialWebAppPlatform,
                            onClearInitialPlatform = { initialWebAppPlatform = null },
                            onNavigateSettings = { currentSubRoute = SubRoute.SETTINGS }
                        )
                        ScreenTab.PROFILE -> ProfileScreen(
                            prefs = prefs,
                            prefsManager = prefsManager,
                            onOpenSettings = { currentSubRoute = SubRoute.SETTINGS },
                            onOpenAccount = { currentSubRoute = SubRoute.ACCOUNT },
                            onOpenWebBlocking = { currentSubRoute = SubRoute.WEB_BLOCKING },
                            onOpenTodayStats = { currentSubRoute = SubRoute.TODAY_ANALYTICS }
                        )
                    }
                }
            }

            // Draggable Floating Bubble Navigation when Navigation == "Bubble"
            if (isBubbleNav) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 24.dp)
                        .offset { IntOffset(bubbleOffsetX.roundToInt(), bubbleOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                bubbleOffsetX += dragAmount.x
                                bubbleOffsetY += dragAmount.y
                            }
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (bubbleExpanded) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(0xFF17171F))
                                    .border(1.dp, Color(0xFF2E3045), RoundedCornerShape(22.dp))
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ScreenTab.values().forEach { tab ->
                                    val isSelected = currentTab == tab && currentSubRoute == SubRoute.NONE
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) Color(0xFF6366F1) else Color.Transparent)
                                            .clickable {
                                                currentSubRoute = SubRoute.NONE
                                                currentTab = tab
                                                bubbleExpanded = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (tab == ScreenTab.WEBAPPS) {
                                            SocialLiteRibbonIcon(size = 18.dp, tint = Color.White)
                                        } else if (tab.icon != null) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.label,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = tab.label,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Floating Bubble Button
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFF17171F))
                                .border(1.5.dp, Color(0xFF6366F1), CircleShape)
                                .clickable { bubbleExpanded = !bubbleExpanded },
                            contentAlignment = Alignment.Center
                        ) {
                            SocialLiteRibbonIcon(size = 24.dp, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
