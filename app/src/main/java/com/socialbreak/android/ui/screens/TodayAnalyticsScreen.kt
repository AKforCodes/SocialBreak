package com.socialbreak.android.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.socialbreak.android.data.AppUsageTracker
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.ui.components.ProtectionPermissionHelper
import com.socialbreak.android.ui.theme.*

@Composable
fun TodayAnalyticsScreen(
    prefs: SocialBreakPreferences,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val palette = SocialTheme.colors

    val usageGranted = remember { ProtectionPermissionHelper.isUsageAccessGranted(context) }
    val todayUsageList = remember(usageGranted) {
        if (usageGranted) AppUsageTracker.getTodayUsageStats(context) else emptyList()
    }
    val nativeSocialMinutes = remember(todayUsageList) {
        (todayUsageList.sumOf { it.foregroundTimeMillis } / 60_000L).toInt()
    }
    val timeInAppsMinutes = nativeSocialMinutes + prefs.inSocialLiteMinutesToday
    val timeLeftMinutes = (prefs.dailyGoalMinutes - timeInAppsMinutes).coerceAtLeast(0)

    val hardModeMinutes = remember(prefs.hardModeEnabled, prefs.hardModeActivatedTimestamp) {
        if (prefs.hardModeEnabled && prefs.hardModeActivatedTimestamp > 0L) {
            ((System.currentTimeMillis() - prefs.hardModeActivatedTimestamp) / 60_000L).toInt().coerceAtLeast(1)
        } else {
            0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Header Row: Back Arrow + "Today"
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
                text = "Today",
                color = palette.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top Summary Card: "0 blocks landed" | "0m on social today · 30m left today"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${prefs.blocksLandedToday}",
                    color = palette.textPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "blocks landed",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${timeInAppsMinutes}m on social today · ${timeLeftMinutes}m left today",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Usage Access Explanation Banner Card (View 18)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = "See your real screen time",
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android shares time spent in other apps only if you approve Usage Access. SocialLite reads your social apps only, and it stays on this phone.",
                color = palette.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (usageGranted) "Usage Access Approved ✓" else "Approve Usage Access",
                color = if (usageGranted) EmeraldSuccess else Color(0xFF818CF8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (_: Exception) {}
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 8 Metric Rows Card with 30-day average tick bars (Views 18 & 19)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            TodayMetricRow(
                label = "Blocks landed",
                value = "${prefs.blocksLandedToday}",
                fillFraction = (prefs.blocksLandedToday / 20f).coerceIn(0f, 1f)
            )
            MetricDivider()
            TodayMetricRow(
                label = "Time in apps",
                value = "${timeInAppsMinutes}m",
                fillFraction = (timeInAppsMinutes / 60f).coerceIn(0f, 1f)
            )
            MetricDivider()
            TodayMetricRow(
                label = "Time left of your limit",
                value = "${timeLeftMinutes}m",
                fillFraction = (timeLeftMinutes.toFloat() / prefs.dailyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
            )
            MetricDivider()
            TodayMetricRow(
                label = "Sessions",
                value = "${prefs.sessionsToday}",
                fillFraction = (prefs.sessionsToday / 10f).coerceIn(0f, 1f)
            )
            MetricDivider()
            TodayMetricRow(
                label = "In SocialLite",
                value = "${prefs.inSocialLiteMinutesToday}m",
                fillFraction = (prefs.inSocialLiteMinutesToday / 60f).coerceIn(0f, 1f)
            )
            MetricDivider()
            TodayMetricRow(
                label = "Limit hits",
                value = "${prefs.limitHitsToday}",
                fillFraction = (prefs.limitHitsToday / 10f).coerceIn(0f, 1f)
            )
            MetricDivider()
            TodayMetricRow(
                label = "Pushed through",
                value = "${prefs.pushedThroughToday}",
                fillFraction = (prefs.pushedThroughToday / 5f).coerceIn(0f, 1f)
            )
            MetricDivider()
            TodayMetricRow(
                label = "Hard Mode",
                value = "${hardModeMinutes}m",
                fillFraction = (hardModeMinutes / 120f).coerceIn(0f, 1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Card: "What these numbers are" (View 19)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = "What these numbers are",
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Everything here is counted on this phone for the selected day, and compared with your own last thirty days. The tick on each bar is that average — left of it is a quieter day than usual, right of it is a heavier one. Nothing is scored against anybody else.",
                color = palette.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TodayMetricRow(
    label: String,
    value: String,
    fillFraction: Float = 0f
) {
    val palette = SocialTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (fillFraction > 0f) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF222330))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF6366F1))
                )
                // 30-day average center tick mark
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF9CA3AF))
                )
            }
        }
    }
}

@Composable
private fun MetricDivider() {
    Divider(color = Color(0xFF1F202D), thickness = 0.75.dp)
}
