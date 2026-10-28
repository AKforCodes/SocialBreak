package com.socialbreak.android.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.receivers.UninstallProtectionReceiver
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(
    prefs: PreferencesManager,
    onNavigateAccount: () -> Unit = {},
    onNavigateAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    var isDeviceAdminActive by remember {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, UninstallProtectionReceiver::class.java)
        mutableStateOf(dpm.isAdminActive(adminComponent))
    }

    val weeklyCompliance = remember { com.socialbreak.android.data.AppUsageTracker.getWeeklyCompliance(context) }
    val daysWithinLimit = remember(weeklyCompliance) { weeklyCompliance.count { it } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header (Screenshot 20: This week)
        Text(
            text = "This week",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        // Top Horizontal Split Card: 7-Petal Floral Chart + Days within limit (Screenshot 20)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Half: 7-Petal Radial Floral Chart
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(125.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val petalRadius = 13.dp.toPx()
                        val orbitRadius = 36.dp.toPx()

                        val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")

                        val textPaint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 8.dp.toPx()
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }

                        // Draw 7 radiating petals
                        for (i in 0 until 7) {
                            val angleRad = Math.toRadians((i * (360.0 / 7) - 90)).toFloat()
                            val px = center.x + orbitRadius * cos(angleRad)
                            val py = center.y + orbitRadius * sin(angleRad)
                            val isAchieved = if (i < weeklyCompliance.size) weeklyCompliance[i] else false

                            drawCircle(
                                color = if (isAchieved) Color(0xFF10B981) else Color(0xFF1E2638),
                                radius = petalRadius,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = if (isAchieved) Color(0xFF34D399) else Color(0xFF334155),
                                radius = petalRadius,
                                center = Offset(px, py),
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // Letter inside petal
                            drawContext.canvas.nativeCanvas.drawText(
                                dayLetters[i],
                                px,
                                py + 3.dp.toPx(),
                                textPaint
                            )
                        }

                        // Center Circle
                        drawCircle(
                            color = Color(0xFF0C0F17),
                            radius = 18.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFF1E2538),
                            radius = 18.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Center Number
                    Text(
                        text = daysWithinLimit.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Half: Days Within Limit Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$daysWithinLimit days within your limit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (daysWithinLimit == 0) "First full goal day starts tomorrow" else "Maintaining great digital wellness this week",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Purple Gift Card (Screenshot 20: Give your friends a week of SocialBreak Pro)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2E244E), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A152E)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Give your friends",
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "a week of SocialBreak Pro",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎁", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "▼", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Settings Menu Card Group (Screenshot 20)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column {
                // Row 1: Stop SocialBreak being deleted
                ExactProfileRow(
                    icon = Icons.Outlined.Lock,
                    iconBgColor = if (isDeviceAdminActive) Color(0xFF064E3B) else Color(0xFF1E2638),
                    iconTintColor = if (isDeviceAdminActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                    title = "Stop SocialBreak being deleted",
                    subtitle = if (isDeviceAdminActive) "Active - uninstall protection enabled" else "Off - anyone can delete the app",
                    onClick = {
                        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                        val adminComponent = ComponentName(context, UninstallProtectionReceiver::class.java)
                        if (!dpm.isAdminActive(adminComponent)) {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "SocialBreak asks for no other permissions here. This only stops the app being deleted — it cannot lock, wipe or track this device."
                                )
                            }
                            context.startActivity(intent)
                        }
                    }
                )

                HorizontalDivider(color = Color(0xFF1E2538), thickness = 0.5.dp)

                // Row 2: Help
                ExactProfileRow(
                    icon = Icons.Outlined.HelpOutline,
                    iconBgColor = Color(0xFF1A2234),
                    iconTintColor = Color(0xFF60A5FA),
                    title = "Help",
                    subtitle = "FAQ, feedback and support",
                    onClick = {}
                )

                HorizontalDivider(color = Color(0xFF1E2538), thickness = 0.5.dp)

                // Row 3: Account
                ExactProfileRow(
                    icon = Icons.Outlined.Person,
                    iconBgColor = Color(0xFF1A2234),
                    iconTintColor = Color(0xFFF59E0B),
                    title = "Account",
                    subtitle = "Browsing data, log out, delete",
                    onClick = onNavigateAccount
                )

                HorizontalDivider(color = Color(0xFF1E2538), thickness = 0.5.dp)

                // Row 4: About
                ExactProfileRow(
                    icon = Icons.Outlined.Info,
                    iconBgColor = Color(0xFF1A2234),
                    iconTintColor = Color(0xFF94A3B8),
                    title = "About",
                    subtitle = "Version, privacy, terms",
                    onClick = onNavigateAbout
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Reset goals link (Screenshot 20)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* reset goals */ },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Reset goals",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ExactProfileRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )
    }
}
