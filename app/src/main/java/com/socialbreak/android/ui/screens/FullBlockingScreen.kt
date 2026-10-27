package com.socialbreak.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

data class FocusPresetItem(val time: String, val label: String, val minutes: Int, val gradient: List<Color>)

val FOCUS_PRESETS = listOf(
    FocusPresetItem("25 min", "focus sprint", 25, listOf(Color(0xFF2C1E14), Color(0xFF161B26))),
    FocusPresetItem("1 hr", "study session", 60, listOf(Color(0xFF1E2838), Color(0xFF121824))),
    FocusPresetItem("2 hr", "deep work", 120, listOf(Color(0xFF241E38), Color(0xFF121824))),
    FocusPresetItem("4 hr", "half day", 240, listOf(Color(0xFF38231E), Color(0xFF121824))),
    FocusPresetItem("8 hr", "work day", 480, listOf(Color(0xFF1A3828), Color(0xFF121824)))
)

val APPS_LIST = listOf(
    "Instagram" to "com.instagram.android",
    "YouTube" to "com.google.android.youtube",
    "TikTok" to "com.zhiliaoapp.musically",
    "Snapchat" to "com.snapchat.android",
    "X" to "com.twitter.android",
    "Facebook" to "com.facebook.katana",
    "LinkedIn" to "com.linkedin.android"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullBlockingScreen(prefs: PreferencesManager) {
    val coroutineScope = rememberCoroutineScope()
    var selectedMinutes by remember { mutableIntStateOf(30) }
    val focusBlockUntil by prefs.focusBlockUntil.collectAsState(initial = 0L)
    val blockedPackages by prefs.blockedPackages.collectAsState(initial = emptySet())

    var showExplainerSheet by remember { mutableStateOf(false) }
    var showAppPickerSheet by remember { mutableStateOf(false) }

    val isCurrentlyBlocked = System.currentTimeMillis() < focusBlockUntil

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Full App Blocking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextHighlight
            )
            Row {
                IconButton(onClick = { showExplainerSheet = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showAppPickerSheet = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Customize", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Cards (Screenshot 3 & 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgSecondary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .clickable { showAppPickerSheet = true }
                    .padding(20.dp)
            ) {
                Column {
                    Text(text = "Social", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${blockedPackages.size} of 7 on", style = MaterialTheme.typography.labelSmall, color = AccentEmerald)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgSecondary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(text = "Other apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "0 apps", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Focus Sprint Preset Cards (Screenshot 4)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(FOCUS_PRESETS) { preset ->
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(preset.gradient))
                        .border(1.dp, if (selectedMinutes == preset.minutes) AccentBlue else BorderSubtle, RoundedCornerShape(20.dp))
                        .clickable { selectedMinutes = preset.minutes }
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Text(text = preset.time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Text(text = preset.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Center Countdown / Duration Display (Screenshot 29)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCurrentlyBlocked) "ACTIVE" else "${selectedMinutes}:00",
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentlyBlocked) AccentCoral else TextHighlight,
                letterSpacing = (-0.02).sp
            )
        }

        // Stepper: - 30 min +
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (selectedMinutes > 5) selectedMinutes -= 5 },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(BgSecondary)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
            }

            Text(
                text = "$selectedMinutes min",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextHighlight,
                modifier = Modifier.padding(horizontal = 28.dp)
            )

            IconButton(
                onClick = { selectedMinutes += 5 },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(BgSecondary)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Start / Stop Block Button (Screenshot 4, 29)
        Button(
            onClick = {
                coroutineScope.launch {
                    if (isCurrentlyBlocked) {
                        prefs.setFocusBlockUntil(0L)
                    } else {
                        val until = System.currentTimeMillis() + (selectedMinutes * 60 * 1000L)
                        prefs.setFocusBlockUntil(until)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCurrentlyBlocked) AccentCoral else BgTertiary
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextHighlight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCurrentlyBlocked) "Stop Block" else "Start $selectedMinutes min block",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHighlight
                )
            }
        }
    }

    // Explainer Bottom Sheet (Screenshot 3)
    if (showExplainerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExplainerSheet = false },
            containerColor = BgSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Full App Blocking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This blocks whole apps outright. A blocked app will not open at all - not the feed, not messages, nothing. The WebApps tab is the softer version: the app opens, but reels and algorithmic short-form feeds are stripped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "With Hard Mode on, switching this off - or switching Hard Mode itself off - takes a four-stage process: type a commitment, answer five maths questions, wait out a 60-second cooldown, then confirm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = { showExplainerSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BgTertiary)
                ) {
                    Text("Got it", color = TextHighlight, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // App Picker Bottom Sheet (Screenshot 30)
    if (showAppPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppPickerSheet = false },
            containerColor = BgSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(text = "Social Apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                Text(text = "Tap an app to include or exclude it from blocking.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(APPS_LIST) { (name, pkg) ->
                        val isBlocked = blockedPackages.contains(pkg)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isBlocked) AccentBlue.copy(alpha = 0.2f) else BgTertiary)
                                .border(1.dp, if (isBlocked) AccentBlue else BorderSubtle, RoundedCornerShape(16.dp))
                                .clickable {
                                    coroutineScope.launch { prefs.toggleBlockedPackage(pkg) }
                                }
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = name.take(2), fontWeight = FontWeight.Bold, color = if (isBlocked) AccentBlue else TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = name, style = MaterialTheme.typography.labelSmall, color = TextHighlight)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showAppPickerSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BgTertiary)
                ) {
                    Text("Done", color = TextHighlight, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
