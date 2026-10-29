package com.socialbreak.android.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.socialbreak.android.R
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.receivers.UninstallProtectionReceiver
import com.socialbreak.android.ui.components.FrictionGauntletDialog
import com.socialbreak.android.ui.components.SocialLiteRibbonIcon
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    onOpenSettings: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenWebBlocking: () -> Unit = {},
    onOpenTodayStats: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = SocialTheme.colors

    var referralExpanded by remember { mutableStateOf(false) }
    var showDeviceAdminSheet by remember { mutableStateOf(false) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showGoalPickerDialog by remember { mutableStateOf(false) }
    var showResetGoalsDialog by remember { mutableStateOf(false) }
    var showResetGauntlet by remember { mutableStateOf(false) }

    fun shareSocialLite() {
        val shareText = "I found this app that strips Reels from Instagram and Shorts from YouTube so I can stay connected without the mindless scrolling. Still have DMs, posts, everything I actually use — just none of the stuff designed to waste my time."
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share SocialLite"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Header Row: "Profile" | Share + Settings Gear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                color = palette.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Help a friend",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { shareSocialLite() }
                )
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(21.dp)
                        .clickable { onOpenSettings() }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7-Petal Weekly Goal Flower Chart (Views 20 & 21)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
                .clickable { showGoalPickerDialog = true },
            contentAlignment = Alignment.Center
        ) {
            SevenPetalGoalChart(
                streakCount = prefs.currentStreakDays,
                goalMinutes = prefs.dailyGoalMinutes
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // "Today | See all" Card (Views 20 & 21)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .clickable { onOpenTodayStats() }
                .padding(horizontal = 16.dp, vertical = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See all",
                    color = Color(0xFF818CF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (prefs.blocksLandedToday > 0) {
                    "${prefs.blocksLandedToday} Reels & Shorts stripped today · ${prefs.inSocialLiteMinutesToday}m in SocialLite"
                } else {
                    "Nothing stripped yet today. This fills in as the blocker catches reels and shorts."
                },
                color = palette.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Referral Gift Banner Card with authentic 3D purple gift box drawable (Views 20 & 21)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .clickable { referralExpanded = !referralExpanded }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.referral_card_art),
                        contentDescription = "Gift",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("Give your friends\n")
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append("a week of SocialLite Pro")
                            }
                        },
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp
                    )
                }
                Icon(
                    imageVector = if (referralExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }

            if (referralExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No more Reels. No more Shorts. No more doomscrolling. Just the parts of social media that actually matter.",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF6366F1))
                            .clickable { shareSocialLite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Send to a Friend", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF252738))
                            .clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("SocialLite Link", "https://trysociallite.com"))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Copy Link", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // "Stop SocialLite being deleted" Card (Views 20, 21, 22)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .clickable { showDeviceAdminSheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Stop SocialLite being deleted",
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (prefs.uninstallProtectionEnabled) {
                        "On · can't be deleted directly"
                    } else {
                        "Off · anyone can delete the app"
                    },
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = prefs.uninstallProtectionEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        showDeviceAdminSheet = true
                    } else {
                        scope.launch { prefsManager.setUninstallProtection(false) }
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

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation List Card: Web Blocking | Help | About | Account (View 21)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            ProfileNavRow(
                title = "Web Blocking",
                subtitle = "Block sites across all browsers",
                onClick = onOpenWebBlocking
            )
            Divider(color = Color(0xFF1F202D), thickness = 0.75.dp)
            ProfileNavRow(
                title = "Help",
                subtitle = "FAQ, feedback and support",
                onClick = { showHelpSheet = true }
            )
            Divider(color = Color(0xFF1F202D), thickness = 0.75.dp)
            ProfileNavRow(
                title = "About",
                subtitle = "Version, privacy, terms",
                onClick = { showAboutSheet = true }
            )
            Divider(color = Color(0xFF1F202D), thickness = 0.75.dp)
            ProfileNavRow(
                title = "Account",
                subtitle = "Browsing data, log out, delete",
                onClick = onOpenAccount
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom "Reset goals" Link (View 21)
        Text(
            text = "Reset goals",
            color = palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable {
                    if (prefs.hardModeEnabled) {
                        showResetGauntlet = true
                    } else {
                        showResetGoalsDialog = true
                    }
                }
                .padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))
    }

    // View 22: Custom "Activate device admin app" Bottom Sheet
    if (showDeviceAdminSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeviceAdminSheet = false },
            containerColor = Color(0xFF171822),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Activate device admin app",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF232536)),
                        contentAlignment = Alignment.Center
                    ) {
                        SocialLiteRibbonIcon(size = 22.dp, tint = Color.White)
                    }
                    Text(
                        text = "Stop SocialLite being deleted",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Keeps SocialLite on this phone so blocking can't be removed by deleting the app.",
                    color = Color(0xFFD1D5DB),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "SocialLite asks for no other permissions here. This only stops the app being deleted — it cannot lock, wipe or track this device.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action Row matching View 22: Cancel | Uninstall | Activate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showDeviceAdminSheet = false }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                    Text(
                        text = "Uninstall",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                showDeviceAdminSheet = false
                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_DELETE,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                    Text(
                        text = "Activate",
                        color = Color(0xFF818CF8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                showDeviceAdminSheet = false
                                scope.launch { prefsManager.setUninstallProtection(true) }
                                try {
                                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                    val adminComponent = ComponentName(context, UninstallProtectionReceiver::class.java)
                                    if (!dpm.isAdminActive(adminComponent)) {
                                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                            putExtra(
                                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                                "Keeps SocialLite on this phone so blocking can't be removed by deleting the app."
                                            )
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
            }
        }
    }

    // Help & Support Bottom Sheet (from strings.xml lines 320–339)
    if (showHelpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHelpSheet = false },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Help & Support", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Frequently Asked Questions", color = Color(0xFF818CF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                HelpFaqCard(
                    q = "Is my login information safe?",
                    a = "Yes! When you log in to any platform, you're logging in directly on their official website through a standard WebView. SocialLite never sees, collects, or stores your passwords. Your session stays entirely on your device."
                )
                HelpFaqCard(
                    q = "Why can't I turn off Reel/Shorts blocking?",
                    a = "Core short-form video blocking (Instagram Reels, YouTube Shorts, TikTok For You, Snapchat Spotlight) is permanently enabled because that's the entire purpose of SocialLite — helping you stay off addictive infinite feeds."
                )
                HelpFaqCard(
                    q = "How do I switch between multiple Instagram accounts?",
                    a = "Open Instagram Settings inside SocialLite and tap Manage Accounts to switch or clear sessions."
                )
                HelpFaqCard(
                    q = "Why does the app feel a bit slower than the native app?",
                    a = "SocialLite loads web versions of platforms and actively strips out distractions in real time. Native apps are built by teams of thousands with custom rendering engines."
                )
            }
        }
    }

    // About Bottom Sheet
    if (showAboutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutSheet = false },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("About SocialLite", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                    Text("3.0.1 (100% On-Device)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .padding(16.dp)
                ) {
                    Text("Privacy Policy & Data Sovereignty", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "All screen-time analytics, sleep schedules, focus sessions, and WebView cookies stay 100% locally on this phone. Zero telemetry, zero cloud sync, and zero third-party ad trackers.",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // Daily Goal Picker Modal
    if (showGoalPickerDialog) {
        val goals = listOf(15, 30, 45, 60, 90, 120)
        Dialog(onDismissRequest = { showGoalPickerDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(22.dp)
            ) {
                Text("Set your daily goal", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stay under your daily social goal to fill each day's petal on your weekly flower.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                goals.chunked(3).forEach { rowGoals ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGoals.forEach { g ->
                            val isSelected = prefs.dailyGoalMinutes == g
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) EmeraldSuccess else Color(0xFF232536))
                                    .clickable {
                                        scope.launch { prefsManager.setDailyGoalMinutes(g) }
                                        showGoalPickerDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$g min",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reset Goals Confirmation Dialog
    if (showResetGoalsDialog) {
        Dialog(onDismissRequest = { showResetGoalsDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text("Reset goals?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This resets your daily goal back to 30 minutes and clears today's counters and per-app limits.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showResetGoalsDialog = false },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            scope.launch { prefsManager.resetGoals() }
                            showResetGoalsDialog = false
                            Toast.makeText(context, "Goals reset", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showResetGauntlet) {
        FrictionGauntletDialog(
            actionLabel = "your goals",
            onDismiss = { showResetGauntlet = false },
            onPassed = {
                showResetGauntlet = false
                scope.launch { prefsManager.resetGoals() }
            }
        )
    }
}

@Composable
private fun HelpFaqCard(q: String, a: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1D1E2B))
            .padding(14.dp)
    ) {
        Text(q, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(a, color = Color(0xFF9CA3AF), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun SevenPetalGoalChart(
    streakCount: Int,
    goalMinutes: Int
) {
    val textMeasurer = rememberTextMeasurer()
    // Match Views 20 & 21: "M" at top (12 o'clock), then T, W, T, F, S, S clockwise
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    Box(
        modifier = Modifier.size(204.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val petalDist = 48.dp.toPx()
            val petalWidth = 44.dp.toPx()
            val petalHeight = 72.dp.toPx()

            days.forEachIndexed { index, dayLabel ->
                val angleDeg = -90f + index * (360f / 7f)
                val angleRad = angleDeg * (PI / 180f).toFloat()
                val petalCenter = Offset(
                    center.x + petalDist * cos(angleRad),
                    center.y + petalDist * sin(angleRad)
                )

                val isAchieved = index < streakCount
                rotate(degrees = angleDeg + 90f, pivot = petalCenter) {
                    drawOval(
                        color = if (isAchieved) Color(0xFF22C55E).copy(alpha = 0.78f) else Color(0xFF1A261E),
                        topLeft = Offset(
                            petalCenter.x - petalWidth / 2f,
                            petalCenter.y - petalHeight / 2f
                        ),
                        size = Size(petalWidth, petalHeight)
                    )
                    drawOval(
                        color = if (isAchieved) Color(0xFF4ADE80) else Color(0xFF2E4234),
                        topLeft = Offset(
                            petalCenter.x - petalWidth / 2f,
                            petalCenter.y - petalHeight / 2f
                        ),
                        size = Size(petalWidth, petalHeight),
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                }

                // Day letter near outer tip of each petal
                val labelDist = 70.dp.toPx()
                val measured = textMeasurer.measure(
                    text = dayLabel,
                    style = TextStyle(
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = center.x + labelDist * cos(angleRad) - measured.size.width / 2f,
                        y = center.y + labelDist * sin(angleRad) - measured.size.height / 2f
                    )
                )
            }

            // Center Dark Circle
            drawCircle(
                color = Color(0xFF111513),
                radius = 35.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF2E4234),
                radius = 35.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$streakCount",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$goalMinutes min goal",
                color = Color(0xFF8E91A6),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProfileNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = SocialTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
