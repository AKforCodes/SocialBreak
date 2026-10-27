package com.socialbreak.android.ui.screens

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.components.FrictionGauntletDialog
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeDashboardScreen(prefs: PreferencesManager) {
    val coroutineScope = rememberCoroutineScope()
    val isHardMode by prefs.hardModeEnabled.collectAsState(initial = false)
    val strippedCount by prefs.todayStrippedCount.collectAsState(initial = 0)

    var showGauntlet by remember { mutableStateOf(false) }
    var showHardModePrompt by remember { mutableStateOf(false) }
    var preventEasyUninstall by remember { mutableStateOf(true) }

    // Hard Mode Off: 4-stage friction gauntlet
    if (showGauntlet) {
        FrictionGauntletDialog(
            onDismiss = { showGauntlet = false },
            onSuccess = {
                showGauntlet = false
                coroutineScope.launch { prefs.setHardMode(false) }
            }
        )
    }

    // Hard Mode On: Activation Screen (Screenshot 6)
    if (showHardModePrompt) {
        Dialog(onDismissRequest = { showHardModePrompt = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BgPrimary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Turn on Hard Mode?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgSecondary)
                            .clickable { preventEasyUninstall = !preventEasyUninstall }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = preventEasyUninstall,
                            onCheckedChange = { preventEasyUninstall = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentAmber, uncheckedColor = TextMuted)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Prevent easy uninstall", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showHardModePrompt = false
                            coroutineScope.launch { prefs.setHardMode(true) }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                    ) {
                        Text("Continue", color = BgPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showHardModePrompt = false }) {
                        Text("Not yet", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "SocialBreak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header (Screenshot 5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SocialBreak",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextHighlight
            )
            Text(
                text = "Customize apps",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Protection Alerts Banner (Screenshot 5)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgSecondary)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Protection alerts", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            Text(text = "Fix", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AccentBlue)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Central Connected Node Graph Cluster (Screenshot 5, 7)
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(BgSecondary)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val angles = listOf(210.0, 270.0, 330.0) // Triangle configuration for YouTube, Instagram, TikTok
                angles.forEach { angleDeg ->
                    val rad = Math.toRadians(angleDeg)
                    val x = center.x + (size.width * 0.35f * Math.cos(rad)).toFloat()
                    val y = center.y + (size.height * 0.35f * Math.sin(rad)).toFloat()
                    drawLine(color = BorderSubtle, start = center, end = Offset(x, y), strokeWidth = 2.dp.toPx())
                }
            }

            // Center S Logo Node
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(BgTertiary)
                    .border(2.dp, AccentBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(30.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom App Dock (Snapchat, Facebook, X, Hard Mode Pill) (Screenshot 5, 7)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockAppItem("Snapchat", "No limit")
                    DockAppItem("Facebook", "No limit")
                    DockAppItem("X", "No limit")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hard Mode Pill Toggle (Screenshot 5, 7)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isHardMode) AccentAmber.copy(alpha = 0.2f) else BgTertiary)
                        .border(1.dp, if (isHardMode) AccentAmber else BorderSubtle, RoundedCornerShape(9999.dp))
                        .clickable {
                            if (isHardMode) showGauntlet = true else showHardModePrompt = true
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = if (isHardMode) AccentAmber else TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHardMode) "Hard Mode Active" else "Hard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isHardMode) AccentAmber else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Today Dynamic Stat Card (Screenshot 7)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                    Text(text = "Tue, Oct 27", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (strippedCount == 0) "Nothing stripped yet today. This fills in as the blocker catches reels and shorts."
                           else "Successfully shielded you from $strippedCount short-form dopamine traps today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun DockAppItem(name: String, limit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgTertiary)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = name.take(2), fontWeight = FontWeight.Bold, color = TextHighlight)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = limit, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
