package com.socialbreak.android.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.socialbreak.android.data.GrayscaleController
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.ui.components.ProtectionPermissionHelper
import com.socialbreak.android.ui.components.SocialAppIcon
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onStartTour: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = SocialTheme.colors

    var showNavStyleInfoDialog by remember { mutableStateOf(false) }
    var showGrayscaleInfoDialog by remember { mutableStateOf(false) }
    var showAppLaunchSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Header Row: Back Arrow + "Settings" (Views 2 & 13)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = palette.textPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onBack() }
            )
            Text(
                text = "Settings",
                color = palette.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Appearance
        Text(
            text = "Appearance",
            color = palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Navigation: Bubble | Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Navigation",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Navigation style info",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { showNavStyleInfoDialog = true }
                    )
                }

                SegmentedPillToggle(
                    options = listOf("Bubble" to "bubble", "Bar" to "bar"),
                    selectedValue = prefs.navigationStyle,
                    onSelect = { style -> scope.launch { prefsManager.setNavigationStyle(style) } }
                )
            }

            // 2. Theme: Light | Dark | System
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Theme",
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                SegmentedPillToggle(
                    options = listOf("Light" to "light", "Dark" to "dark", "System" to "system"),
                    selectedValue = prefs.themeMode,
                    onSelect = { mode -> scope.launch { prefsManager.setThemeMode(mode) } }
                )
            }

            // 3. Grayscale Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Grayscale",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Grayscale info",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { showGrayscaleInfoDialog = true }
                    )
                }

                Switch(
                    checked = prefs.globalGrayscale,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            prefsManager.setGlobalGrayscale(enabled)
                            GrayscaleController.setHardwareGrayscale(context, enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6366F1),
                        uncheckedThumbColor = Color(0xFF8E91A6),
                        uncheckedTrackColor = palette.switchTrackOff
                    )
                )
            }

            // 4. Icon Theme: Classic | Modern | Painted (3 bordered cards matching Views 2 & 13)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Icon Theme",
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val iconThemes = listOf("Classic" to "classic", "Modern" to "modern", "Painted" to "painted")
                    iconThemes.forEach { (label, value) ->
                        val isSelected = prefs.iconTheme == value
                        val cardBg = if (palette.isDark) Color(0xFF1F202D) else Color(0xFFF0F1F6)
                        val selectedColor = if (palette.isDark) SkyBlueSelected else Color(0xFF4F46E5)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) selectedColor else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { scope.launch { prefsManager.setIconTheme(value) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) selectedColor else palette.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Section: Behavior (Views 2 & 13)
        Text(
            text = "Behavior",
            color = palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // App Launch Row Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardBackground)
                .clickable { showAppLaunchSheet = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "App Launch",
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Notifications Row Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardBackground)
                .clickable { showNotificationsSheet = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // App Tour Row Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardBackground)
                .clickable { onStartTour() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Replay App Tour",
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    // Navigation Style Info Dialog (from strings.xml line 440)
    if (showNavStyleInfoDialog) {
        Dialog(onDismissRequest = { showNavStyleInfoDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text("Navigation Style", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Bubble — a floating button you can drag anywhere; tap to expand quick actions.\n\nBar — fixed to the bottom, controls always visible.",
                    color = Color(0xFFA0A3B5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6366F1))
                        .clickable { showNavStyleInfoDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Why Grayscale Info Dialog (from strings.xml line 438)
    if (showGrayscaleInfoDialog) {
        Dialog(onDismissRequest = { showGrayscaleInfoDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text("Why Grayscale?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Research from the University of Virginia found that grayscale screens reduce compulsive phone usage by up to 37%. Colorful notifications and icons are engineered to hijack your attention — removing color makes your phone noticeably less tempting to pick up.",
                    color = Color(0xFFA0A3B5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6366F1))
                        .clickable { showGrayscaleInfoDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // App Launch Bottom Sheet (from strings.xml lines 417-424)
    if (showAppLaunchSheet) {
        val launchApps = listOf(
            "instagram" to "Instagram",
            "youtube" to "YouTube",
            "tiktok" to "TikTok",
            "snapchat" to "Snapchat",
            "facebook" to "Facebook",
            "x" to "X"
        )
        ModalBottomSheet(
            onDismissRequest = { showAppLaunchSheet = false },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 30.dp)
            ) {
                Text("App Launch", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Auto-launch on open", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (prefs.autoLaunchEnabled) {
                                "Opens directly into ${prefs.autoLaunchPlatform.replaceFirstChar { it.uppercase() }}"
                            } else {
                                "Off — opens to the SocialBreak home screen"
                            },
                            color = Color(0xFF8E91A6),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = prefs.autoLaunchEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { prefsManager.setAutoLaunch(enabled, prefs.autoLaunchPlatform) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6366F1)
                        )
                    )
                }

                if (prefs.autoLaunchEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Default app", color = Color(0xFF8E91A6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    launchApps.forEach { (appId, label) ->
                        val isChosen = prefs.autoLaunchPlatform == appId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChosen) Color(0xFF252738) else Color(0xFF1D1E2B))
                                .clickable {
                                    scope.launch { prefsManager.setAutoLaunch(true, appId) }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SocialAppIcon(
                                    appId = appId,
                                    iconTheme = prefs.iconTheme,
                                    size = 30.dp,
                                    cornerRadius = 8.dp
                                )
                                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            if (isChosen) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF818CF8))
                            }
                        }
                    }
                }
            }
        }
    }

    // Notifications Bottom Sheet (from strings.xml lines 449-454)
    if (showNotificationsSheet) {
        val notifGranted = remember { ProtectionPermissionHelper.isNotificationListenerEnabled(context) }
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 30.dp)
            ) {
                Text("Notifications", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Redirect notifications through SocialBreak",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Tapping a notification opens it inside SocialBreak instead of the native app.",
                            color = Color(0xFF8E91A6),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = prefs.interceptNotifications,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                prefsManager.setBooleanPreference(PreferencesManager.INTERCEPT_NOTIFICATIONS, enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6366F1)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .clickable {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification Access Permission", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (notifGranted) "Granted and active" else "Tap to grant Notification Access in Android Settings",
                            color = if (notifGranted) EmeraldSuccess else AmberGold,
                            fontSize = 12.sp
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF8E91A6))
                }
            }
        }
    }
}

@Composable
private fun SegmentedPillToggle(
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    val palette = SocialTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(palette.segmentedTrack)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (label, value) ->
            val isSelected = selectedValue == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) palette.segmentedThumb else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) palette.textPrimary else palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}
