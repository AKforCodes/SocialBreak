package com.socialbreak.android.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.DomainFilterRepository
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.ui.components.FrictionGauntletDialog
import com.socialbreak.android.ui.components.ProtectionPermissionHelper
import com.socialbreak.android.ui.components.ProtectionSetupBottomSheet
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class EditingWindDownTarget {
    NONE, BEFORE_BED, AFTER_WAKE
}

private enum class EditingTimeTarget {
    NONE, BEDTIME, WAKE_UP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepModeScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = SocialTheme.colors

    var showScheduleSheet by remember { mutableStateOf(false) }
    var showWindDownSheet by remember { mutableStateOf(false) }
    var showAllowedAppsSheet by remember { mutableStateOf(false) }
    var showProtectionSheet by remember { mutableStateOf(false) }
    var showDisableGauntlet by remember { mutableStateOf(false) }
    var windDownPickerTarget by remember { mutableStateOf(EditingWindDownTarget.NONE) }
    var editingTimeTarget by remember { mutableStateOf(EditingTimeTarget.NONE) }

    val allProtectionsReady = remember(showProtectionSheet) {
        ProtectionPermissionHelper.areAllProtectionsActive(context)
    }

    val totalMinutes = remember(prefs.bedtimeHour, prefs.bedtimeMinute, prefs.wakeHour, prefs.wakeMinute) {
        val bedTotal = prefs.bedtimeHour * 60 + prefs.bedtimeMinute
        val wakeTotal = prefs.wakeHour * 60 + prefs.wakeMinute
        if (wakeTotal >= bedTotal) wakeTotal - bedTotal else (1440 - bedTotal) + wakeTotal
    }
    val sleepHours = totalMinutes / 60
    val sleepRemMinutes = totalMinutes % 60

    val windDownSummary = remember(
        prefs.beforeBedWindDownEnabled,
        prefs.beforeBedWindDownMinutes,
        prefs.afterWakeWindDownEnabled,
        prefs.afterWakeWindDownMinutes
    ) {
        when {
            prefs.beforeBedWindDownEnabled && prefs.afterWakeWindDownEnabled ->
                "${prefs.beforeBedWindDownMinutes}m / ${prefs.afterWakeWindDownMinutes}m"
            prefs.beforeBedWindDownEnabled -> "${prefs.beforeBedWindDownMinutes} min"
            prefs.afterWakeWindDownEnabled -> "${prefs.afterWakeWindDownMinutes} min"
            else -> "Off"
        }
    }

    val scheduleSummary = remember(prefs.sleepRepeatEveryDay, prefs.sleepScheduleDays) {
        if (prefs.sleepRepeatEveryDay || prefs.sleepScheduleDays.size == 7) {
            "Every day"
        } else if (prefs.sleepScheduleDays.isEmpty()) {
            "Off"
        } else {
            "Set per day"
        }
    }

    val allowedSummary = remember(prefs.sleepAllowedApps) {
        if (prefs.sleepAllowedApps.isEmpty()) "None" else "${prefs.sleepAllowedApps.size} apps"
    }

    val view = LocalView.current
    // Dusk ambient background color transition (oe_kp.java: 950ms tween)
    val sleepBgColor by animateColorAsState(
        targetValue = if (prefs.sleepModeEnabled) Color(0xFF06060D) else palette.background,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
        label = "sleep-dusk-bg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sleepBgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Top Title
        Text(
            text = "Sleep Mode",
            color = palette.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Bedtime & Wake Up Header Row (clickable to open time picker)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { editingTimeTarget = EditingTimeTarget.BEDTIME }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = "Bedtime",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bedtime,
                        contentDescription = "Bedtime",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = format12Hour(prefs.bedtimeHour, prefs.bedtimeMinute),
                        color = palette.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { editingTimeTarget = EditingTimeTarget.WAKE_UP }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = "Wake Up",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = "Wake Up",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = format12Hour(prefs.wakeHour, prefs.wakeMinute),
                        color = palette.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive 24-Hour Circular Arc Dial
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(265.dp),
            contentAlignment = Alignment.Center
        ) {
            InteractiveSleepArcDial(
                bedtimeHour = prefs.bedtimeHour,
                bedtimeMinute = prefs.bedtimeMinute,
                wakeHour = prefs.wakeHour,
                wakeMinute = prefs.wakeMinute,
                durationLabel = if (sleepRemMinutes == 0) "${sleepHours}h" else "${sleepHours}h ${sleepRemMinutes}m",
                onScheduleChanged = { newBedH, newBedM, newWakeH, newWakeM ->
                    scope.launch {
                        prefsManager.updateSleepSchedule(newBedH, newBedM, newWakeH, newWakeM)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3 Action Cards: Schedule | Wind down | Allowed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SleepSettingCard(
                label = "Schedule",
                value = scheduleSummary,
                modifier = Modifier.weight(1f),
                onClick = { showScheduleSheet = true }
            )
            SleepSettingCard(
                label = "Wind down",
                value = windDownSummary,
                modifier = Modifier.weight(1f),
                onClick = { showWindDownSheet = true }
            )
            SleepSettingCard(
                label = "Allowed",
                value = allowedSummary,
                modifier = Modifier.weight(1f),
                onClick = { showAllowedAppsSheet = true }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Finish protection setup warning banner
        if (!allProtectionsReady) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.cardBackground)
                    .clickable { showProtectionSheet = true }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AmberGold)
                    )
                    Text(
                        text = "Finish protection setup",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Fix",
                    color = AmberGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Turn on / Turn off Sleep Mode Pill CTA (View 1 vs View 28)
        val ctaGradient = if (prefs.sleepModeEnabled) {
            // View 28: Deep muted purple gradient
            Brush.horizontalGradient(
                colors = listOf(Color(0xFF5D5292), Color(0xFF27223E))
            )
        } else {
            // View 1: Soft lavender to warm cream gradient
            Brush.horizontalGradient(
                colors = listOf(Color(0xFFB8A9F8), Color(0xFFF5EFE0))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(ctaGradient)
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    if (prefs.sleepModeEnabled && prefs.hardModeEnabled) {
                        showDisableGauntlet = true
                    } else {
                        scope.launch { prefsManager.setSleepMode(!prefs.sleepModeEnabled) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (prefs.sleepModeEnabled) "Turn off Sleep Mode" else "Turn on Sleep Mode",
                    color = if (prefs.sleepModeEnabled) Color.White else Color(0xFF14121E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (prefs.sleepModeEnabled) Color.White else Color(0xFF14121E),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // View 24: Schedule Bottom Sheet
    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            containerColor = Color(0xFF171822),
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            var repeatEveryDay by remember(prefs.sleepRepeatEveryDay) { mutableStateOf(prefs.sleepRepeatEveryDay) }
            var selectedDays by remember(prefs.sleepScheduleDays) { mutableStateOf(prefs.sleepScheduleDays) }
            val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schedule",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Done",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            scope.launch {
                                prefsManager.setSleepRepeatEveryDay(repeatEveryDay, selectedDays)
                            }
                            showScheduleSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Repeat every day",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = repeatEveryDay,
                        onCheckedChange = { checked ->
                            repeatEveryDay = checked
                            if (checked) {
                                selectedDays = setOf("0", "1", "2", "3", "4", "5", "6")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6366F1),
                            uncheckedThumbColor = Color(0xFF8E91A6),
                            uncheckedTrackColor = Color(0xFF282A3A)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLabels.forEachIndexed { idx, day ->
                        val dayKey = idx.toString()
                        val isDaySelected = selectedDays.contains(dayKey)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDaySelected) Color(0xFF6366F1) else Color(0xFF252636))
                                .clickable {
                                    val updated = if (isDaySelected) selectedDays - dayKey else selectedDays + dayKey
                                    selectedDays = updated
                                    repeatEveryDay = updated.size == 7
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // View 25: Wind Down Bottom Sheet
    if (showWindDownSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWindDownSheet = false },
            containerColor = Color(0xFF171822),
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wind Down",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Done",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showWindDownSheet = false }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Before bed row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Block apps", color = Color.White, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF282A3A))
                                .clickable { windDownPickerTarget = EditingWindDownTarget.BEFORE_BED }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${prefs.beforeBedWindDownMinutes}m",
                                color = Color(0xFF818CF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("before bed time", color = Color.White, fontSize = 14.sp)
                    }
                    Switch(
                        checked = prefs.beforeBedWindDownEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                prefsManager.setBeforeBedWindDown(enabled, prefs.beforeBedWindDownMinutes)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6366F1),
                            uncheckedThumbColor = Color(0xFF8E91A6),
                            uncheckedTrackColor = Color(0xFF282A3A)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // After wake up row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Block apps", color = Color.White, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF282A3A))
                                .clickable { windDownPickerTarget = EditingWindDownTarget.AFTER_WAKE }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${prefs.afterWakeWindDownMinutes}m",
                                color = Color(0xFF818CF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("after you wake up", color = Color.White, fontSize = 14.sp)
                    }
                    Switch(
                        checked = prefs.afterWakeWindDownEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                prefsManager.setAfterWakeWindDown(enabled, prefs.afterWakeWindDownMinutes)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6366F1),
                            uncheckedThumbColor = Color(0xFF8E91A6),
                            uncheckedTrackColor = Color(0xFF282A3A)
                        )
                    )
                }
            }
        }
    }

    // Views 26 & 27: Scrollable Wheel Minute Picker Sheet
    if (windDownPickerTarget != EditingWindDownTarget.NONE) {
        val options = listOf(0, 5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
        val initialMinutes = if (windDownPickerTarget == EditingWindDownTarget.BEFORE_BED) {
            prefs.beforeBedWindDownMinutes
        } else {
            prefs.afterWakeWindDownMinutes
        }
        var selectedMin by remember(windDownPickerTarget) { mutableIntStateOf(initialMinutes) }

        ModalBottomSheet(
            onDismissRequest = { windDownPickerTarget = EditingWindDownTarget.NONE },
            containerColor = Color(0xFF171822),
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF8E91A6),
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { windDownPickerTarget = EditingWindDownTarget.NONE }
                    )
                    Text(
                        text = "Done",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            val target = windDownPickerTarget
                            scope.launch {
                                if (target == EditingWindDownTarget.BEFORE_BED) {
                                    prefsManager.setBeforeBedWindDown(
                                        enabled = selectedMin > 0 || prefs.beforeBedWindDownEnabled,
                                        minutes = if (selectedMin == 0) 0 else selectedMin
                                    )
                                } else if (target == EditingWindDownTarget.AFTER_WAKE) {
                                    prefsManager.setAfterWakeWindDown(
                                        enabled = selectedMin > 0 || prefs.afterWakeWindDownEnabled,
                                        minutes = if (selectedMin == 0) 0 else selectedMin
                                    )
                                }
                            }
                            windDownPickerTarget = EditingWindDownTarget.NONE
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = options.indexOf(selectedMin).coerceAtLeast(0)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(options) { mins ->
                        val isChosen = mins == selectedMin
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isChosen) Color(0xFF252738) else Color.Transparent)
                                .clickable { selectedMin = mins },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$mins min",
                                color = if (isChosen) Color.White else Color(0xFF7B7E94),
                                fontSize = if (isChosen) 17.sp else 15.sp,
                                fontWeight = if (isChosen) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    // Bedtime / Wake Up Time Picker Sheet
    if (editingTimeTarget != EditingTimeTarget.NONE) {
        val isBedtime = editingTimeTarget == EditingTimeTarget.BEDTIME
        var tempHour by remember(editingTimeTarget) {
            mutableIntStateOf(if (isBedtime) prefs.bedtimeHour else prefs.wakeHour)
        }
        var tempMinute by remember(editingTimeTarget) {
            mutableIntStateOf(if (isBedtime) prefs.bedtimeMinute else prefs.wakeMinute)
        }

        ModalBottomSheet(
            onDismissRequest = { editingTimeTarget = EditingTimeTarget.NONE },
            containerColor = Color(0xFF171822),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF8E91A6),
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { editingTimeTarget = EditingTimeTarget.NONE }
                    )
                    Text(
                        text = if (isBedtime) "Bedtime" else "Wake Up",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Done",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            scope.launch {
                                if (isBedtime) {
                                    prefsManager.updateSleepSchedule(tempHour, tempMinute, prefs.wakeHour, prefs.wakeMinute)
                                } else {
                                    prefsManager.updateSleepSchedule(prefs.bedtimeHour, prefs.bedtimeMinute, tempHour, tempMinute)
                                }
                            }
                            editingTimeTarget = EditingTimeTarget.NONE
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = format12Hour(tempHour, tempMinute),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hour & Minute steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF222330))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Hour", color = Color(0xFF8E91A6), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E3042))
                                    .clickable { tempHour = (tempHour + 23) % 24 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("—", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "${if (tempHour % 12 == 0) 12 else tempHour % 12}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E3042))
                                    .clickable { tempHour = (tempHour + 1) % 24 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF222330))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Minute", color = Color(0xFF8E91A6), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E3042))
                                    .clickable { tempMinute = (tempMinute + 45) % 60 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("—", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = tempMinute.toString().padStart(2, '0'),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E3042))
                                    .clickable { tempMinute = (tempMinute + 15) % 60 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AM / PM Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222330))
                        .padding(4.dp)
                ) {
                    val isPm = tempHour >= 12
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!isPm) Color(0xFF6366F1) else Color.Transparent)
                            .clickable { if (tempHour >= 12) tempHour -= 12 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AM", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isPm) Color(0xFF6366F1) else Color.Transparent)
                            .clickable { if (tempHour < 12) tempHour += 12 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PM", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Allowed Apps Bottom Sheet
    if (showAllowedAppsSheet) {
        val essentialCandidates = listOf(
            "Phone & Dialer",
            "Messages (SMS)",
            "Alarm & Clock",
            "Calendar",
            "Maps & Navigation",
            "Uber / Lyft",
            "Spotify / Music",
            "Notes"
        ) + DomainFilterRepository.SOCIAL_APPS.map { it.displayName }

        var selectedAllowed by remember(prefs.sleepAllowedApps) { mutableStateOf(prefs.sleepAllowedApps) }

        ModalBottomSheet(
            onDismissRequest = { showAllowedAppsSheet = false },
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
                    Column {
                        Text(
                            text = "Always allowed",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "These apps still open while Sleep Mode is on.",
                            color = Color(0xFF8E91A6),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Done",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            scope.launch { prefsManager.setSleepAllowedApps(selectedAllowed) }
                            showAllowedAppsSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(essentialCandidates) { appName ->
                        val isAllowed = selectedAllowed.contains(appName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1D1E2B))
                                .clickable {
                                    selectedAllowed = if (isAllowed) selectedAllowed - appName else selectedAllowed + appName
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isAllowed) Color(0xFF6366F1) else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isAllowed) Color(0xFF6366F1) else Color(0xFF4A4D62),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isAllowed) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProtectionSheet) {
        ProtectionSetupBottomSheet(onDismiss = { showProtectionSheet = false })
    }

    if (showDisableGauntlet) {
        FrictionGauntletDialog(
            actionLabel = "Sleep Mode",
            onDismiss = { showDisableGauntlet = false },
            onPassed = {
                showDisableGauntlet = false
                scope.launch { prefsManager.setSleepMode(false) }
            }
        )
    }
}

@Composable
private fun SleepSettingCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = SocialTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.cardBackground)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun InteractiveSleepArcDial(
    bedtimeHour: Int,
    bedtimeMinute: Int,
    wakeHour: Int,
    wakeMinute: Int,
    durationLabel: String,
    onScheduleChanged: (Int, Int, Int, Int) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    val currentBedH by rememberUpdatedState(bedtimeHour)
    val currentBedM by rememberUpdatedState(bedtimeMinute)
    val currentWakeH by rememberUpdatedState(wakeHour)
    val currentWakeM by rememberUpdatedState(wakeMinute)

    // Track which handle is being dragged: 0 = none, 1 = bedtime, 2 = wake
    var draggingHandle by remember { mutableIntStateOf(0) }
    var lastHapticMinutes by remember { mutableIntStateOf(-1) }

    Box(
        modifier = Modifier.size(256.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val strokeW = 28.dp.toPx()
                            val minDim = minOf(size.width, size.height).toFloat()
                            val radius = (minDim - strokeW) / 2f

                            val bedFrac = (currentBedH * 60 + currentBedM) / 1440f
                            val wakeFrac = (currentWakeH * 60 + currentWakeM) / 1440f

                            val startRad = (-90f + bedFrac * 360f) * (PI / 180f).toFloat()
                            val endRad = (-90f + wakeFrac * 360f) * (PI / 180f).toFloat()

                            val bedPos = Offset(
                                center.x + radius * cos(startRad),
                                center.y + radius * sin(startRad)
                            )
                            val wakePos = Offset(
                                center.x + radius * cos(endRad),
                                center.y + radius * sin(endRad)
                            )

                            val distBed = hypot(offset.x - bedPos.x, offset.y - bedPos.y)
                            val distWake = hypot(offset.x - wakePos.x, offset.y - wakePos.y)

                            draggingHandle = if (distBed <= distWake) 1 else 2
                        },
                        onDragEnd = { draggingHandle = 0 },
                        onDragCancel = { draggingHandle = 0 },
                        onDrag = { change, _ ->
                            change.consume()
                            if (draggingHandle != 0) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (deg < 0f) deg += 360f

                                // Snap to 15-minute increments (96 steps in 24h)
                                val totalSteps = ((deg / 360f) * 96f).roundToInt() % 96
                                val totalMins = totalSteps * 15
                                val newH = (totalMins / 60) % 24
                                val newM = totalMins % 60

                                if (totalMins != lastHapticMinutes) {
                                    lastHapticMinutes = totalMins
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }

                                if (draggingHandle == 1) {
                                    onScheduleChanged(newH, newM, currentWakeH, currentWakeM)
                                } else {
                                    onScheduleChanged(currentBedH, currentBedM, newH, newM)
                                }
                            }
                        }
                    )
                }
        ) {
            val strokeWidth = 28.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1. Outer Ring Track
            drawCircle(
                color = Color(0xFF353746),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. Inner Dial Plate
            drawCircle(
                color = Color(0xFF141624),
                radius = radius - strokeWidth / 2f,
                center = center
            )

            // 3. 24-Hour Clock Ticks inside the inner plate
            val tickOuterR = radius - strokeWidth / 2f - 8.dp.toPx()
            for (h in 0 until 24) {
                val angleRad = (-90f + h * 15f) * (PI / 180f).toFloat()
                val isCardinal = h % 6 == 0
                val tickLen = if (isCardinal) 7.dp.toPx() else 4.dp.toPx()
                val tickInnerR = tickOuterR - tickLen
                val startPt = Offset(
                    center.x + tickOuterR * cos(angleRad),
                    center.y + tickOuterR * sin(angleRad)
                )
                val endPt = Offset(
                    center.x + tickInnerR * cos(angleRad),
                    center.y + tickInnerR * sin(angleRad)
                )
                drawLine(
                    color = if (isCardinal) Color(0xFF8E91A6) else Color(0xFF434659),
                    start = startPt,
                    end = endPt,
                    strokeWidth = if (isCardinal) 2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. Cardinal Hour Labels (12AM, 6AM, 12, 6PM)
            val labels = listOf(
                Triple("12AM", 0f, -1f),
                Triple("6AM", 1f, 0f),
                Triple("12", 0f, 1f),
                Triple("6PM", -1f, 0f)
            )
            val labelRadius = tickOuterR - 20.dp.toPx()
            labels.forEach { (txt, dx, dy) ->
                val measured = textMeasurer.measure(
                    text = txt,
                    style = TextStyle(
                        color = Color(0xFF7B7E94),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = center.x + dx * labelRadius - measured.size.width / 2f,
                        y = center.y + dy * labelRadius - measured.size.height / 2f
                    )
                )
            }

            // 5. Active Sleep Arc (24h Clockwise: 12AM = -90deg)
            val bedFraction = (bedtimeHour * 60 + bedtimeMinute) / 1440f
            val wakeFraction = (wakeHour * 60 + wakeMinute) / 1440f
            val startAngle = -90f + (bedFraction * 360f)
            val rawSweep = (wakeFraction - bedFraction) * 360f
            val sweepAngle = if (rawSweep <= 0f) rawSweep + 360f else rawSweep

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color(0xFFF2F0F8), Color(0xFFFDFBF0), Color(0xFFF2F0F8)),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 6. Bedtime Handle (Moon Icon on White Thumb)
            val startRad = startAngle * (PI / 180f).toFloat()
            val startThumb = Offset(
                center.x + radius * cos(startRad),
                center.y + radius * sin(startRad)
            )
            drawCircle(
                color = Color.White,
                radius = 12.5.dp.toPx(),
                center = startThumb
            )
            // Crescent moon inside bedtime thumb
            drawCircle(
                color = Color(0xFF6366F1),
                radius = 5.5.dp.toPx(),
                center = startThumb
            )
            drawCircle(
                color = Color.White,
                radius = 4.2.dp.toPx(),
                center = Offset(startThumb.x + 2.5.dp.toPx(), startThumb.y - 2.dp.toPx())
            )

            // 7. Wake Handle (Sun Icon on White Thumb)
            val endRad = (startAngle + sweepAngle) * (PI / 180f).toFloat()
            val endThumb = Offset(
                center.x + radius * cos(endRad),
                center.y + radius * sin(endRad)
            )
            drawCircle(
                color = Color.White,
                radius = 12.5.dp.toPx(),
                center = endThumb
            )
            drawCircle(
                color = Color(0xFF6366F1),
                radius = 4.5.dp.toPx(),
                center = endThumb
            )
            for (ray in 0 until 8) {
                val rAngle = ray * 45f * (PI / 180f).toFloat()
                val rIn = 6.2.dp.toPx()
                val rOut = 8.2.dp.toPx()
                drawLine(
                    color = Color(0xFF6366F1),
                    start = Offset(endThumb.x + rIn * cos(rAngle), endThumb.y + rIn * sin(rAngle)),
                    end = Offset(endThumb.x + rOut * cos(rAngle), endThumb.y + rOut * sin(rAngle)),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Duration Readout ("8h")
        Text(
            text = durationLabel,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

private fun format12Hour(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "am" else "pm"
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val mStr = minute.toString().padStart(2, '0')
    return "$h12:$mStr $amPm"
}
