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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepModeScreen(
    prefs: PreferencesManager,
    onNavigateSettings: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isSleepMode by prefs.sleepModeEnabled.collectAsState(initial = false)
    val bedtimeHour by prefs.bedtimeHour.collectAsState(initial = 23)
    val bedtimeMinute by prefs.bedtimeMinute.collectAsState(initial = 0)
    val wakeHour by prefs.wakeHour.collectAsState(initial = 7)
    val wakeMinute by prefs.wakeMinute.collectAsState(initial = 0)
    val windDownMin by prefs.windDownMinutes.collectAsState(initial = 30)

    var showScheduleSheet by remember { mutableStateOf(false) }
    var showWindDownSheet by remember { mutableStateOf(false) }
    var repeatEveryDay by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sleep Mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextHighlight
            )
            IconButton(onClick = onNavigateSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 24-Hour Circular Arc Dial Visualizer (Screenshot 1)
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(BgSecondary)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                // Outer Track
                drawArc(
                    color = BorderSubtle,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )
                // Active Sleep Arc (e.g. 11pm to 7am)
                drawArc(
                    brush = Brush.sweepGradient(listOf(AccentPurple, AccentBlue, AccentPurple)),
                    startAngle = 270f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "8h",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHighlight
                )
                Text(
                    text = "Scheduled Sleep",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bedtime and Wake Up Times Cards (Screenshot 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = String.format("%02d:%02d pm", if (bedtimeHour > 12) bedtimeHour - 12 else bedtimeHour, bedtimeMinute),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHighlight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Bedtime", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = String.format("%02d:%02d am", wakeHour, wakeMinute),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHighlight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Wake Up", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Repeat Schedule Row (Screenshot 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgSecondary)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .clickable { showScheduleSheet = true }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Repeat every day", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (repeatEveryDay) "Every day" else "Weekdays", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Finish Protection Setup Banner (Screenshot 1 & 5)
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
            Text(text = "Finish protection setup", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(
                text = "Fix",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                modifier = Modifier.clickable { /* open permission intent */ }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Wind Down & Allowed Pills (Screenshot 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgSecondary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .clickable { showWindDownSheet = true }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "Wind down", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${windDownMin} min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgSecondary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "Allowed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "None", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Master Sleep Toggle Button (Screenshot 1, 28)
        Button(
            onClick = {
                coroutineScope.launch { prefs.setSleepMode(!isSleepMode) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSleepMode) AccentCoral else AccentPurple
            )
        ) {
            Text(
                text = if (isSleepMode) "Turn off Sleep Mode ->" else "Turn on Sleep Mode ->",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextHighlight
            )
        }
    }

    // Schedule Bottom Sheet (Screenshot 24)
    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            containerColor = BgSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Schedule", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Repeat every day", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Switch(
                        checked = repeatEveryDay,
                        onCheckedChange = { repeatEveryDay = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentEmerald, checkedTrackColor = BgTertiary)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showScheduleSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BgTertiary)
                ) {
                    Text("Done", color = TextHighlight, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Wind Down Bottom Sheet (Screenshots 25, 26, 27)
    if (showWindDownSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWindDownSheet = false },
            containerColor = BgSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Wind Down", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgTertiary)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(text = "Block apps", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(text = "${windDownMin}m", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                            Text(text = "before bed time", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgTertiary)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(text = "Block apps", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(text = "${windDownMin}m", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                            Text(text = "after you wake up", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = { showWindDownSheet = false },
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
