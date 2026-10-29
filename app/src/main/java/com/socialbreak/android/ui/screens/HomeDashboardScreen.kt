package com.socialbreak.android.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.receivers.UninstallProtectionReceiver
import com.socialbreak.android.ui.components.FrictionGauntletDialog
import com.socialbreak.android.ui.components.ProtectionPermissionHelper
import com.socialbreak.android.ui.components.ProtectionSetupBottomSheet
import com.socialbreak.android.ui.components.SocialAppIcon
import com.socialbreak.android.ui.components.SocialLiteRibbonIcon
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    onOpenCleanWebApp: (String) -> Unit,
    onNavigateWebBlocking: () -> Unit,
    onNavigateTodayStats: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = SocialTheme.colors

    var showHardModeConfirmScreen by remember { mutableStateOf(false) }
    var preventUninstallChecked by remember { mutableStateOf(true) }
    var showDisableHardModeGauntlet by remember { mutableStateOf(false) }
    var showProtectionSheet by remember { mutableStateOf(false) }
    var showCustomizeAppsSheet by remember { mutableStateOf(false) }
    var selectedAppForAllowance by remember { mutableStateOf<String?>(null) }

    val allProtectionsReady = remember(showProtectionSheet) {
        ProtectionPermissionHelper.areAllProtectionsActive(context)
    }

    // View 6: Full-Screen "Turn on Hard Mode?" Confirmation View
    if (showHardModeConfirmScreen) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Hard Mode Lock",
                tint = PeachOrange,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Turn on Hard Mode?",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Locks your settings behind a typing challenge, 5 mental math problems, and a 60-second wait. You won't be able to impulsively turn off blocking or lower limits.",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "Prevent easy uninstall" checkbox card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF14141C))
                    .clickable { preventUninstallChecked = !preventUninstallChecked }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (preventUninstallChecked) PeachOrange else Color(0xFF282A3A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (preventUninstallChecked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "Prevent easy uninstall",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Peach-orange "Continue" pill button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PeachOrange)
                    .clickable {
                        scope.launch {
                            prefsManager.setHardMode(true, preventUninstallChecked)
                            if (preventUninstallChecked) {
                                prefsManager.setUninstallProtection(true)
                                try {
                                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                    val adminComp = ComponentName(context, UninstallProtectionReceiver::class.java)
                                    if (!dpm.isAdminActive(adminComp)) {
                                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComp)
                                            putExtra(
                                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                                "Keeps SocialLite on this phone so blocking can't be removed by deleting the app."
                                            )
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        showHardModeConfirmScreen = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    color = Color(0xFF1A140E),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Not yet",
                color = Color(0xFF8E91A6),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showHardModeConfirmScreen = false }
            )

            Spacer(modifier = Modifier.weight(1.2f))

            Text(
                text = "Sociallite",
                color = Color(0xFF555768),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    // Views 5 & 7: Main Home Dashboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: "SocialLite [S]" | "Customize apps"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SocialLite",
                    color = palette.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                SocialLiteRibbonIcon(
                    size = 22.dp,
                    tint = palette.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.cardBackground)
                    .border(0.5.dp, palette.border, RoundedCornerShape(10.dp))
                    .clickable { showCustomizeAppsSheet = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Customize apps",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // View 5: Protection alerts banner (shown if permissions aren't all granted)
        if (!allProtectionsReady) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.cardBackground)
                    .clickable { showProtectionSheet = true }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AmberGold)
                    )
                    Column {
                        Text(
                            text = "Protection alerts",
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "App and sleep blocking · Content blocking · Notification access",
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = "Fix",
                    color = AmberGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Centered "Hard Mode" Pill Switch
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(palette.cardBackground)
                .border(0.5.dp, palette.border, RoundedCornerShape(14.dp))
                .clickable {
                    if (!prefs.hardModeEnabled) {
                        showHardModeConfirmScreen = true
                    } else {
                        showDisableHardModeGauntlet = true
                    }
                }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (prefs.hardModeEnabled) PeachOrange else palette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Hard Mode",
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = prefs.hardModeEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        showHardModeConfirmScreen = true
                    } else {
                        showDisableHardModeGauntlet = true
                    }
                },
                modifier = Modifier.height(24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PeachOrange,
                    uncheckedThumbColor = Color(0xFF8E91A6),
                    uncheckedTrackColor = palette.switchTrackOff
                )
            )
        }

        // Central Connected Node Hub: YouTube (top), S-Hub (center), Instagram (bottom-left), TikTok (bottom-right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Diagonal & vertical connector lines
            Canvas(modifier = Modifier.size(240.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val topNode = Offset(cx, cy - 78.dp.toPx())
                val leftNode = Offset(cx - 78.dp.toPx(), cy + 62.dp.toPx())
                val rightNode = Offset(cx + 78.dp.toPx(), cy + 62.dp.toPx())

                val lineColor = Color(0xFF2D2F42)
                drawLine(lineColor, Offset(cx, cy), topNode, strokeWidth = 1.5.dp.toPx())
                drawLine(lineColor, Offset(cx, cy), leftNode, strokeWidth = 1.5.dp.toPx())
                drawLine(lineColor, Offset(cx, cy), rightNode, strokeWidth = 1.5.dp.toPx())
            }

            // Center S Hub Circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF151620))
                    .border(1.5.dp, Color(0xFF2E3045), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SocialLiteRibbonIcon(size = 24.dp, tint = Color.White)
            }

            // Top Node: YouTube
            if (!prefs.hiddenHomeApps.contains("youtube")) {
                val ytLimit = prefs.getAppDailyLimitMinutes("youtube")
                Box(modifier = Modifier.offset(y = (-82).dp)) {
                    ConnectedAppNode(
                        appId = "youtube",
                        iconTheme = prefs.iconTheme,
                        timerText = if (ytLimit > 0) "0:00 / ${ytLimit}m" else "0:00",
                        onClick = { selectedAppForAllowance = "youtube" }
                    )
                }
            }

            // Bottom-Left Node: Instagram
            if (!prefs.hiddenHomeApps.contains("instagram")) {
                val igLimit = prefs.getAppDailyLimitMinutes("instagram")
                Box(modifier = Modifier.offset(x = (-82).dp, y = 66.dp)) {
                    ConnectedAppNode(
                        appId = "instagram",
                        iconTheme = prefs.iconTheme,
                        timerText = if (igLimit > 0) "0:00 / ${igLimit}m" else "0:00",
                        onClick = { selectedAppForAllowance = "instagram" }
                    )
                }
            }

            // Bottom-Right Node: TikTok
            if (!prefs.hiddenHomeApps.contains("tiktok")) {
                val ttLimit = prefs.getAppDailyLimitMinutes("tiktok")
                Box(modifier = Modifier.offset(x = 82.dp, y = 66.dp)) {
                    ConnectedAppNode(
                        appId = "tiktok",
                        iconTheme = prefs.iconTheme,
                        timerText = if (ttLimit > 0) "0:00 / ${ttLimit}m" else "0:00",
                        onClick = { selectedAppForAllowance = "tiktok" }
                    )
                }
            }
        }

        // Bottom App Dock Card: Snapchat | Facebook | X
        val dockApps = listOf(
            "snapchat" to "Snapchat",
            "facebook" to "Facebook",
            "x" to "X"
        ).filter { !prefs.hiddenHomeApps.contains(it.first) }

        if (dockApps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.cardBackground)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.forEach { (appId, _) ->
                    val limit = prefs.getAppDailyLimitMinutes(appId)
                    DockAppItem(
                        appId = appId,
                        iconTheme = prefs.iconTheme,
                        limitText = if (limit > 0) "${limit}m limit" else "No limit",
                        onClick = { selectedAppForAllowance = appId }
                    )
                }
            }
        }
    }

    // Per-App Daily Scrolling Allowance & Native Settings Bottom Sheet
    selectedAppForAllowance?.let { appId ->
        val displayTitle = when (appId) {
            "instagram" -> "Instagram"
            "youtube" -> "YouTube"
            "tiktok" -> "TikTok"
            "snapchat" -> "Snapchat"
            "facebook" -> "Facebook"
            "x" -> "X"
            else -> appId.replaceFirstChar { it.uppercase() }
        }
        var limitMinutes by remember(appId) { mutableIntStateOf(prefs.getAppDailyLimitMinutes(appId)) }

        ModalBottomSheet(
            onDismissRequest = { selectedAppForAllowance = null },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            size = 40.dp,
                            cornerRadius = 11.dp
                        )
                        Column {
                            Text(
                                text = "$displayTitle Settings",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daily scrolling allowance & native protection",
                                color = Color(0xFF8E91A6),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = "Save",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            scope.launch { prefsManager.setAppDailyLimitMinutes(appId, limitMinutes) }
                            selectedAppForAllowance = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Daily scrolling allowance",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set a daily short-form limit, or keep Reels/Shorts/Spotlight completely blocked (0 min).",
                    color = Color(0xFF8E91A6),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets: No limit (0), 5 min, 15 min, 30 min
                val presets = listOf(0 to "No limit", 5 to "5 min", 15 to "15 min", 30 to "30 min")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (mins, label) ->
                        val isSelected = limitMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF6366F1) else Color(0xFF222330))
                                .clickable { limitMinutes = mins },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom minute stepper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { limitMinutes = (limitMinutes - 5).coerceAtLeast(0) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Less", tint = Color.White)
                    }
                    Text(
                        text = if (limitMinutes == 0) "No daily limit" else "$limitMinutes min / day",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { limitMinutes = (limitMinutes + 5).coerceAtMost(180) }) {
                        Icon(Icons.Filled.Add, contentDescription = "More", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Open inside SocialLite button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF252738))
                        .clickable {
                            scope.launch { prefsManager.setAppDailyLimitMinutes(appId, limitMinutes) }
                            selectedAppForAllowance = null
                            onOpenCleanWebApp(appId)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Open $displayTitle inside SocialLite",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Customize / Rearrange Apps Sheet
    if (showCustomizeAppsSheet) {
        val allHomeApps = listOf(
            "youtube" to "YouTube",
            "instagram" to "Instagram",
            "tiktok" to "TikTok",
            "snapchat" to "Snapchat",
            "facebook" to "Facebook",
            "x" to "X"
        )
        var tempHidden by remember(prefs.hiddenHomeApps) { mutableStateOf(prefs.hiddenHomeApps) }

        ModalBottomSheet(
            onDismissRequest = { showCustomizeAppsSheet = false },
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
                            text = "Rearrange Apps",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap an app to configure limits, or tap the eye to hide it on Home.",
                            color = Color(0xFF8E91A6),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Done",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            scope.launch { prefsManager.setHiddenHomeApps(tempHidden) }
                            showCustomizeAppsSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                allHomeApps.forEach { (appId, label) ->
                    val isHidden = tempHidden.contains(appId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1D1E2B))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
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
                                size = 34.dp,
                                cornerRadius = 9.dp
                            )
                            Text(
                                text = label,
                                color = if (isHidden) Color(0xFF6E7185) else Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = {
                                tempHidden = if (isHidden) tempHidden - appId else tempHidden + appId
                            }
                        ) {
                            Icon(
                                imageVector = if (isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = if (isHidden) Color(0xFF6E7185) else Color(0xFF818CF8)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProtectionSheet) {
        ProtectionSetupBottomSheet(onDismiss = { showProtectionSheet = false })
    }

    if (showDisableHardModeGauntlet) {
        FrictionGauntletDialog(
            actionLabel = "Hard Mode",
            onDismiss = { showDisableHardModeGauntlet = false },
            onPassed = {
                showDisableHardModeGauntlet = false
                scope.launch { prefsManager.setHardMode(false, false) }
            }
        )
    }
}

@Composable
private fun ConnectedAppNode(
    appId: String,
    iconTheme: String,
    timerText: String,
    onClick: () -> Unit
) {
    val palette = SocialTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            SocialAppIcon(
                appId = appId,
                iconTheme = iconTheme,
                size = 56.dp,
                cornerRadius = 15.dp,
                modifier = Modifier.align(Alignment.Center)
            )
            // Small top-right gear badge
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF232433))
                    .border(1.dp, Color(0xFF3E4056), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Color(0xFFB4B6C8),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = timerText,
            color = palette.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DockAppItem(
    appId: String,
    iconTheme: String,
    limitText: String,
    onClick: () -> Unit
) {
    val palette = SocialTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(54.dp)) {
            SocialAppIcon(
                appId = appId,
                iconTheme = iconTheme,
                size = 48.dp,
                cornerRadius = 13.dp,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF232433))
                    .border(1.dp, Color(0xFF3E4056), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Color(0xFFB4B6C8),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = limitText,
            color = palette.textSecondary,
            fontSize = 11.sp
        )
    }
}
