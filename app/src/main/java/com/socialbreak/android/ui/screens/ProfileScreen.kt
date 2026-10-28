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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.receivers.UninstallProtectionReceiver
import com.socialbreak.android.ui.theme.*
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "This week",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextHighlight
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Petal Floral Radial Achievement Visualizer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = BgSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Petal Canvas
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(150.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val petalRadius = 20.dp.toPx()
                        val orbitRadius = 45.dp.toPx()
                        val days = listOf("S", "M", "T", "W", "T", "F", "S")

                        for (i in 0 until 7) {
                            val angle = Math.toRadians((i * (360.0 / 7) - 90)).toFloat()
                            val cx = center.x + orbitRadius * cos(angle)
                            val cy = center.y + orbitRadius * sin(angle)

                            drawCircle(
                                color = BgTertiary,
                                radius = petalRadius,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = BorderSubtle,
                                radius = petalRadius,
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }

                        // Center Circle
                        drawCircle(
                            color = BgPrimary,
                            radius = 28.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = BorderSubtle,
                            radius = 28.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHighlight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "0 days within your limit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighlight
                )
                Text(
                    text = "First full goal day starts tomorrow",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Give your friends a week of SocialBreak Pro
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BgSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Give your friends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "a week of SocialBreak Pro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHighlight
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Menu Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BgSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column {
                // Device Admin Row
                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Stop SocialBreak being deleted",
                    sub = if (isDeviceAdminActive) "Active - uninstall protection enabled" else "Off - anyone can delete the app",
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

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                // Help
                ProfileMenuItem(
                    icon = Icons.Default.Help,
                    title = "Help",
                    sub = "FAQ, feedback and support",
                    onClick = {}
                )

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                // Account
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = "Account",
                    sub = "Browsing data, log out, delete",
                    onClick = onNavigateAccount
                )

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                // About
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    sub = "Version 1.0.0, privacy, terms",
                    onClick = onNavigateAbout
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reset goals
        TextButton(
            onClick = {},
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset goals", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextHighlight)
                Text(text = sub, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
