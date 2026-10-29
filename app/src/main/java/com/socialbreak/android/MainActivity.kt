package com.socialbreak.android

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.socialbreak.android.data.AppShortcutManager
import com.socialbreak.android.data.AppUpdateManager
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.data.UpdateCheckResult
import com.socialbreak.android.data.UpdateInfo
import com.socialbreak.android.ui.components.SocialBreakRibbonIcon
import com.socialbreak.android.ui.screens.*
import com.socialbreak.android.ui.theme.SocialBreakTheme
import com.socialbreak.android.ui.theme.SocialTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ScreenTab(val label: String, val icon: ImageVector?) {
    SLEEP("Sleep", Icons.Outlined.Bedtime),
    BLOCKING("Blocking", Icons.Outlined.Shield),
    HOME("Home", Icons.Outlined.Home),
    WEBAPPS("WebApps", null), // Uses custom SocialBreak 'S' ribbon icon
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocialBreakMainApp(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    incomingIntent: Intent? = null,
    onConsumeIntent: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
    val palette = SocialTheme.colors

    val tabs = remember { ScreenTab.values().toList() }
    val pagerState = rememberPagerState(
        initialPage = ScreenTab.HOME.ordinal,
        pageCount = { tabs.size }
    )
    val currentTab = tabs[pagerState.currentPage]

    var currentSubRoute by remember { mutableStateOf(SubRoute.NONE) }
    var initialWebAppPlatform by remember { mutableStateOf<String?>(null) }
    var didAutoLaunchCheck by remember { mutableStateOf(false) }
    var updateBannerInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    // Check for updates on startup if enabled
    LaunchedEffect(prefs.autoCheckUpdates) {
        if (prefs.autoCheckUpdates) {
            val now = System.currentTimeMillis()
            if (now - prefs.lastUpdateCheckTs > 86400000L || prefs.lastUpdateCheckTs == 0L) {
                try {
                    val mgr = AppUpdateManager(context)
                    val res = mgr.checkForUpdate()
                    if (res is UpdateCheckResult.UpdateAvailable) {
                        updateBannerInfo = res.info
                    }
                    prefsManager.setLongPreference(PreferencesManager.LAST_UPDATE_CHECK_TS, now)
                } catch (_: Exception) {}
            }
        }
    }

    // Interactive Landing Tour state (me.f0.a in me_f0.java)
    var showLandingTour by remember(prefs.hasSeenLandingTour) {
        mutableStateOf(!prefs.hasSeenLandingTour)
    }
    var tourStep by remember { mutableIntStateOf(0) }

    // Trigger haptic tick when swiping between tabs in HorizontalPager
    LaunchedEffect(pagerState.currentPage) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    // Handle Auto-Launch on first cold open
    LaunchedEffect(prefs.autoLaunchEnabled, prefs.autoLaunchPlatform) {
        if (!didAutoLaunchCheck && prefs.autoLaunchEnabled) {
            didAutoLaunchCheck = true
            initialWebAppPlatform = prefs.autoLaunchPlatform
            pagerState.scrollToPage(ScreenTab.WEBAPPS.ordinal)
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
                pagerState.scrollToPage(ScreenTab.WEBAPPS.ordinal)
                onConsumeIntent()
            }
            action == AppShortcutManager.ACTION_START_FOCUS_25 -> {
                scope.launch { prefsManager.startTimedBlock(25, untilStopped = false) }
                currentSubRoute = SubRoute.NONE
                pagerState.scrollToPage(ScreenTab.BLOCKING.ordinal)
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
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = pagerState.currentPage == index && currentSubRoute == SubRoute.NONE
                            val itemColor = if (isSelected) palette.navBarSelectedText else palette.navBarUnselectedText

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        currentSubRoute = SubRoute.NONE
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (tab == ScreenTab.WEBAPPS) {
                                    SocialBreakRibbonIcon(
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
            updateBannerInfo?.let { info ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF23253A))
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable {
                            currentSubRoute = SubRoute.SETTINGS
                            updateBannerInfo = null
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .zIndex(50f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "SocialBreak ${info.latestVersion} available",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to review changelog & install update",
                                color = Color(0xFFA0A3B5),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { updateBannerInfo = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF8E91A6),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

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
                        onOpenAccount = { currentSubRoute = SubRoute.ACCOUNT },
                        onStartTour = {
                            currentSubRoute = SubRoute.NONE
                            tourStep = 0
                            showLandingTour = true
                            scope.launch {
                                prefsManager.setHasSeenLandingTour(false)
                                pagerState.animateScrollToPage(0)
                            }
                        }
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
                    // HorizontalPager across all 5 main tabs (he.c1 in he_c1.java)
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        when (tabs[pageIndex]) {
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
                                    scope.launch {
                                        pagerState.animateScrollToPage(ScreenTab.WEBAPPS.ordinal)
                                    }
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
            }

            // Interactive 5-Step Landing Tour Overlay with Curved Bezier Arrow (me.f0.a in me_f0.java)
            if (showLandingTour && currentSubRoute == SubRoute.NONE) {
                LandingTourOverlay(
                    step = tourStep,
                    onNextStep = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        if (tourStep < 4) {
                            val next = tourStep + 1
                            tourStep = next
                            scope.launch { pagerState.animateScrollToPage(next) }
                        } else {
                            showLandingTour = false
                            scope.launch {
                                prefsManager.setHasSeenLandingTour(true)
                                pagerState.animateScrollToPage(ScreenTab.HOME.ordinal)
                            }
                        }
                    },
                    onSkip = {
                        showLandingTour = false
                        scope.launch {
                            prefsManager.setHasSeenLandingTour(true)
                        }
                    }
                )
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
                                tabs.forEachIndexed { index, tab ->
                                    val isSelected = currentTab == tab && currentSubRoute == SubRoute.NONE
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) Color(0xFF6366F1) else Color.Transparent)
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                currentSubRoute = SubRoute.NONE
                                                bubbleExpanded = false
                                                scope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (tab == ScreenTab.WEBAPPS) {
                                            SocialBreakRibbonIcon(size = 18.dp, tint = Color.White)
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
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    bubbleExpanded = !bubbleExpanded
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            SocialBreakRibbonIcon(size = 24.dp, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandingTourOverlay(
    step: Int,
    onNextStep: () -> Unit,
    onSkip: () -> Unit
) {
    val titles = listOf(
        "Sleep Mode",
        "Full App Blocking",
        "Constellation Home",
        "Inside SocialBreak",
        "Profile & Analytics"
    )
    val descriptions = listOf(
        "Automatically block distracting apps during bedtime and wind-down hours.",
        "Start a timed focus sprint or deep work session whenever you need zero distractions.",
        "Drag apps around the orbit to reorder, or drag them down to the bank to unprotect.",
        "Use clean, distraction-free Instagram & YouTube with Reels, Shorts, and Ads stripped out.",
        "Track your 7-petal weekly streak, configure Web Blocking, and lock in Hard Mode."
    )

    val safeStep = step.coerceIn(0, 4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(40f)
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(enabled = true, onClick = {})
    ) {
        // Curved quadratic Bezier arrow pointing from card to the active bottom nav tab (me.f0.a)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startX = size.width * 0.5f
            val startY = size.height - 170.dp.toPx()
            val targetX = size.width * ((safeStep + 0.5f) / 5f)
            val targetY = size.height - 14.dp.toPx()
            val ctrlX = (startX + targetX) / 2f + (targetX - startX) * 0.35f
            val ctrlY = (startY + targetY) / 2f + 24.dp.toPx()

            val path = Path().apply {
                moveTo(startX, startY)
                quadraticTo(ctrlX, ctrlY, targetX, targetY)
            }
            drawPath(
                path = path,
                color = Color(0xFF818CF8),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Arrowhead at (targetX, targetY)
            val angle = atan2((targetY - ctrlY).toDouble(), (targetX - ctrlX).toDouble())
            val headLen = 10.dp.toPx()
            val wing1 = Offset(
                x = targetX - (headLen * cos(angle - 0.55)).toFloat(),
                y = targetY - (headLen * sin(angle - 0.55)).toFloat()
            )
            val wing2 = Offset(
                x = targetX - (headLen * cos(angle + 0.55)).toFloat(),
                y = targetY - (headLen * sin(angle + 0.55)).toFloat()
            )
            drawLine(Color(0xFF818CF8), Offset(targetX, targetY), wing1, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color(0xFF818CF8), Offset(targetX, targetY), wing2, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 175.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF171824))
                .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "STEP ${safeStep + 1} OF 5",
                color = Color(0xFF818CF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = titles[safeStep],
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descriptions[safeStep],
                color = Color(0xFFA0A3B5),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skip tour",
                    color = Color(0xFF8E91A6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onSkip() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF6366F1))
                        .clickable { onNextStep() }
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (safeStep == 4) "Done" else "Next",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
