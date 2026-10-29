package com.socialbreak.android.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.socialbreak.android.R
import com.socialbreak.android.data.DomainFilterRepository
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.ui.components.FrictionGauntletDialog
import com.socialbreak.android.ui.components.SocialAppIcon
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FullBlockingScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    onNavigateSettings: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = SocialTheme.colors

    var showInfoModal by remember { mutableStateOf(false) }
    var showSocialPickerSheet by remember { mutableStateOf(false) }
    var showOtherAppsPickerSheet by remember { mutableStateOf(false) }
    var showStopBlockGauntlet by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    // Live countdown ticker when timed block is active
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(prefs.fullBlockActive, prefs.fullBlockEndTimeMillis, prefs.untilStoppedMode) {
        while (prefs.fullBlockActive && !prefs.untilStoppedMode) {
            val diff = (prefs.fullBlockEndTimeMillis - System.currentTimeMillis()) / 1000L
            if (diff <= 0L) {
                remainingSeconds = 0L
                prefsManager.stopBlock()
                break
            } else {
                remainingSeconds = diff
            }
            delay(1000L)
        }
    }

    val isBlockingNow = prefs.fullBlockActive && (prefs.untilStoppedMode || remainingSeconds > 0L)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Top Header: "Full App Blocking" + (i) + Settings Gear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Full App Blocking",
                color = palette.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(21.dp)
                        .clickable { showInfoModal = true }
                )
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(21.dp)
                        .clickable { onNavigateSettings() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top 2 App Category Cards: Social (16 selected) | Other apps (0 selected)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCategoryCard(
                title = "Social",
                subtitle = "${prefs.blockedSocialApps.size} selected",
                modifier = Modifier.weight(1f),
                onClick = { showSocialPickerSheet = true }
            )
            AppCategoryCard(
                title = "Other apps",
                subtitle = "${prefs.blockedOtherApps.size} selected",
                modifier = Modifier.weight(1f),
                onClick = { showOtherAppsPickerSheet = true }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Pager: Page 0 = Timer View (View 29), Page 1 = Focus Sprint Sessions (View 4)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            if (page == 0) {
                // View 29: Timer View
                val durationSteps = listOf(5, 10, 15, 20, 25, 30, 45, 60, 90, 120, 180, 240, 480)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    // Single Dark Pill Container housing [ ∞   —   30 min   + ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF15151E))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Circular Infinity Button ("Until stopped")
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (prefs.untilStoppedMode) Color(0xFF6366F1) else Color(0xFF222330)
                                )
                                .clickable(enabled = !isBlockingNow) {
                                    scope.launch {
                                        prefsManager.setUntilStoppedMode(!prefs.untilStoppedMode)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AllInclusive,
                                contentDescription = "Until stopped",
                                tint = if (prefs.untilStoppedMode) Color.White else Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Minus Button
                        IconButton(
                            enabled = !isBlockingNow,
                            onClick = {
                                val curIdx = durationSteps.indexOfFirst { it >= prefs.selectedBlockDurationMinutes }
                                    .coerceAtLeast(0)
                                val prev = if (curIdx > 0) durationSteps[curIdx - 1] else durationSteps.first()
                                scope.launch { prefsManager.setSelectedBlockDuration(prev) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Less time",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Duration / Live Countdown Label
                        val centerDisplay = when {
                            isBlockingNow && prefs.untilStoppedMode -> "Until stopped"
                            isBlockingNow -> formatCountdown(remainingSeconds)
                            prefs.untilStoppedMode -> "Until stopped"
                            else -> formatDurationMinutes(prefs.selectedBlockDurationMinutes)
                        }

                        Text(
                            text = centerDisplay,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Plus Button
                        IconButton(
                            enabled = !isBlockingNow,
                            onClick = {
                                val curIdx = durationSteps.indexOfFirst { it >= prefs.selectedBlockDurationMinutes }
                                val next = if (curIdx in 0 until durationSteps.lastIndex) {
                                    durationSteps[curIdx + 1]
                                } else {
                                    durationSteps.last()
                                }
                                scope.launch { prefsManager.setSelectedBlockDuration(next) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "More time",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // White Pill "Start blocking" / Red Pill "Stop blocking"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isBlockingNow) CrimsonDanger else Color(0xFFF4F4F6)
                            )
                            .clickable {
                                if (isBlockingNow) {
                                    if (prefs.hardModeEnabled) {
                                        showStopBlockGauntlet = true
                                    } else {
                                        scope.launch { prefsManager.stopBlock() }
                                    }
                                } else {
                                    scope.launch {
                                        prefsManager.startTimedBlock(
                                            durationMinutes = prefs.selectedBlockDurationMinutes,
                                            untilStopped = prefs.untilStoppedMode
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBlockingNow) "Stop blocking" else "Start blocking",
                            color = if (isBlockingNow) Color.White else Color(0xFF111118),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                // View 4: Focus Sprint Sessions View (Authentic Photo Artwork Cards)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: 25 min Focus sprint | 1 hr Study session
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FocusSprintPhotoCard(
                            durationLabel = "25 min",
                            subtitle = "Focus sprint",
                            drawableRes = R.drawable.preset_focus_sprint,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                scope.launch {
                                    prefsManager.startTimedBlock(25, untilStopped = false)
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                        FocusSprintPhotoCard(
                            durationLabel = "1 hr",
                            subtitle = "Study session",
                            drawableRes = R.drawable.preset_study_session,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                scope.launch {
                                    prefsManager.startTimedBlock(60, untilStopped = false)
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }

                    // Row 2: 2 hr Deep work | 4 hr Half day
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FocusSprintPhotoCard(
                            durationLabel = "2 hr",
                            subtitle = "Deep work",
                            drawableRes = R.drawable.preset_deep_work,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                scope.launch {
                                    prefsManager.startTimedBlock(120, untilStopped = false)
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                        FocusSprintPhotoCard(
                            durationLabel = "4 hr",
                            subtitle = "Half day",
                            drawableRes = R.drawable.preset_half_day,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                scope.launch {
                                    prefsManager.startTimedBlock(240, untilStopped = false)
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }

                    // Row 3: 8 hr Work day (Full Width Banner)
                    FocusSprintPhotoCard(
                        durationLabel = "8 hr",
                        subtitle = "Work day",
                        drawableRes = R.drawable.preset_work_day,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(106.dp),
                        onClick = {
                            scope.launch {
                                prefsManager.startTimedBlock(480, untilStopped = false)
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Pager Indicator ("• •  swipe for sessions")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        pagerState.animateScrollToPage(if (pagerState.currentPage == 0) 1 else 0)
                    }
                }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (pagerState.currentPage == 0) palette.textPrimary else Color(0xFF434556))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (pagerState.currentPage == 1) palette.textPrimary else Color(0xFF434556))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "swipe for sessions",
                color = Color(0xFF6B6E82),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }

    // View 3: Full App Blocking Info Modal
    if (showInfoModal) {
        Dialog(onDismissRequest = { showInfoModal = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Full App Blocking",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Blocks the apps you pick on this phone. Pick a time or a preset and they won't open until the timer's up.",
                    color = Color(0xFFA0A3B5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF6366F1))
                        .clickable { showInfoModal = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Got it",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // View 30: Social App Multi-Select Bottom Sheet
    if (showSocialPickerSheet) {
        var tempSelected by remember(prefs.blockedSocialApps) { mutableStateOf(prefs.blockedSocialApps) }
        val allSocialApps = DomainFilterRepository.SOCIAL_APPS

        ModalBottomSheet(
            onDismissRequest = { showSocialPickerSheet = false },
            containerColor = Color(0xFF14141C),
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Social",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${tempSelected.size} of ${allSocialApps.size} selected",
                        color = Color(0xFF8E91A6),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allSocialApps) { app ->
                        val isChecked = tempSelected.contains(app.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    tempSelected = if (isChecked) tempSelected - app.id else tempSelected + app.id
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                SocialAppIcon(
                                    appId = app.id,
                                    iconTheme = prefs.iconTheme,
                                    size = 36.dp,
                                    cornerRadius = 10.dp
                                )
                                Text(
                                    text = app.displayName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) Color(0xFF818CF8) else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isChecked) Color(0xFF818CF8) else Color(0xFF4A4D62),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF111118),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF6366F1))
                        .clickable {
                            scope.launch { prefsManager.setBlockedSocialApps(tempSelected) }
                            showSocialPickerSheet = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Update blocked apps",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Other Apps Multi-Select Bottom Sheet with Search
    if (showOtherAppsPickerSheet) {
        var searchQuery by remember { mutableStateOf("") }
        var tempSelectedOther by remember(prefs.blockedOtherApps) { mutableStateOf(prefs.blockedOtherApps) }

        val installedAndCommonApps = remember {
            val commonApps = listOf(
                "Chrome",
                "Netflix",
                "Prime Video",
                "Disney+",
                "Twitch",
                "Roblox",
                "Brawl Stars",
                "Clash Royale",
                "Chess.com",
                "Steam",
                "Amazon Shopping",
                "Temu",
                "Shein",
                "eBay",
                "Tinder",
                "Hinge",
                "Bumble",
                "Telegram",
                "WhatsApp"
            )
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val dynamicLabels = try {
                pm.queryIntentActivities(launcherIntent, 0)
                    .mapNotNull { it.loadLabel(pm)?.toString() }
                    .filter { it.isNotBlank() && !it.contains("SocialBreak", ignoreCase = true) }
            } catch (_: Exception) {
                emptyList()
            }
            (commonApps + dynamicLabels).distinct().sorted()
        }

        val filteredApps = remember(searchQuery, installedAndCommonApps) {
            if (searchQuery.isBlank()) installedAndCommonApps
            else installedAndCommonApps.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = { showOtherAppsPickerSheet = false },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Other apps",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${tempSelectedOther.size} selected",
                        color = Color(0xFF8E91A6),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps", color = Color(0xFF6E7185), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF8E91A6)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF282A3A)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps) { appLabel ->
                        val isChecked = tempSelectedOther.contains(appLabel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    tempSelectedOther = if (isChecked) {
                                        tempSelectedOther - appLabel
                                    } else {
                                        tempSelectedOther + appLabel
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color(0xFF232536)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appLabel.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = appLabel,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) Color(0xFF818CF8) else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isChecked) Color(0xFF818CF8) else Color(0xFF4A4D62),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF111118),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF6366F1))
                        .clickable {
                            scope.launch { prefsManager.setBlockedOtherApps(tempSelectedOther) }
                            showOtherAppsPickerSheet = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Update blocked apps",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showStopBlockGauntlet) {
        FrictionGauntletDialog(
            actionLabel = "full app blocking",
            onDismiss = { showStopBlockGauntlet = false },
            onPassed = {
                showStopBlockGauntlet = false
                scope.launch { prefsManager.stopBlock() }
            }
        )
    }
}

@Composable
private fun AppCategoryCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = SocialTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }
        // 2x2 grid of 4 rounded squares matching Views 4, 29, 30
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(Color(0xFF1D1E2C))
                )
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(Color(0xFF1D1E2C))
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(Color(0xFF1D1E2C))
                )
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(Color(0xFF1D1E2C))
                )
            }
        }
    }
}

@Composable
private fun FocusSprintPhotoCard(
    durationLabel: String,
    subtitle: String,
    drawableRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = subtitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Subtle bottom-left vignette for crisp legibility matching View 4
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = durationLabel,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

private fun formatDurationMinutes(minutes: Int): String {
    return if (minutes < 60) {
        "$minutes min"
    } else {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "$h hr" else "${h}h ${m}m"
    }
}

private fun formatCountdown(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
