package com.socialbreak.android.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

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
    var showBedtimePicker by remember { mutableStateOf(false) }
    var showWakePicker by remember { mutableStateOf(false) }
    var repeatEveryDay by remember { mutableStateOf(true) }

    val bedTimeStr = String.format("%d:%02d pm", if (bedtimeHour > 12) bedtimeHour - 12 else (if (bedtimeHour == 0) 12 else bedtimeHour), bedtimeMinute)
    val wakeTimeStr = String.format("%d:%02d am", if (wakeHour == 0) 12 else wakeHour, wakeMinute)

    // Calculate duration in hours
    val diffHours = ((wakeHour - bedtimeHour + 24) % 24).let { if (it == 0) 24 else it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header (Screenshot 1: Sleep Mode title + Gear)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sleep Mode",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(
                onClick = onNavigateSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 24-Hour Circular Arc Dial (1:1 with Screenshot 1)
        Box(
            modifier = Modifier
                .size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(260.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 24.dp.toPx()

                // 1. Draw inner circular plate (#121624)
                drawCircle(
                    color = Color(0xFF111624),
                    radius = radius - 8.dp.toPx(),
                    center = center
                )

                // 2. Draw outer dark ring track
                drawCircle(
                    color = Color(0xFF1E2536),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 16.dp.toPx())
                )

                // 3. Draw 24-Hour numerals (0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
                val paint = Paint().apply {
                    color = android.graphics.Color.parseColor("#64748B")
                    textSize = 10.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val hourLabels = listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
                val textRadius = radius - 24.dp.toPx()
                for (h in hourLabels) {
                    val angleDeg = (h.toFloat() / 24f) * 360f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val x = center.x + textRadius * cos(angleRad).toFloat()
                    val y = center.y + textRadius * sin(angleRad).toFloat() + 4.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(h.toString(), x, y, paint)
                }

                // 4. Draw continuous active arc (11pm / 23:00 to 7am / 07:00)
                // 23:00 angle = (23/24)*360 - 90 = 255 degrees
                // 7:00 angle = (7/24)*360 - 90 = 15 degrees
                // sweepAngle = 120 degrees
                val startAngle = (bedtimeHour.toFloat() / 24f) * 360f - 90f
                val sweepAngle = (diffHours.toFloat() / 24f) * 360f

                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFFE2E8F0),
                            Color(0xFF93C5FD),
                            Color(0xFF60A5FA),
                            Color(0xFFE2E8F0)
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )

                // 5. Draw Start Knob (Moon knob at bedtime)
                val startRad = Math.toRadians(startAngle.toDouble())
                val startKnobX = center.x + radius * cos(startRad).toFloat()
                val startKnobY = center.y + radius * sin(startRad).toFloat()

                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = 16.dp.toPx(),
                    center = Offset(startKnobX, startKnobY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(startKnobX, startKnobY)
                )

                // 6. Draw End Knob (Sun knob at wake time)
                val endAngle = startAngle + sweepAngle
                val endRad = Math.toRadians(endAngle.toDouble())
                val endKnobX = center.x + radius * cos(endRad).toFloat()
                val endKnobY = center.y + radius * sin(endRad).toFloat()

                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = 16.dp.toPx(),
                    center = Offset(endKnobX, endKnobY)
                )
                drawCircle(
                    color = Color(0xFFFDE047),
                    radius = 6.dp.toPx(),
                    center = Offset(endKnobX, endKnobY)
                )
            }

            // Center Duration Text (8h)
            Text(
                text = "${diffHours}h",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bedtime and Wake Up Row (Screenshot 1: Direct on canvas with moon/sun icons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Bedtime Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { showBedtimePicker = true }
            ) {
                Text(text = "🌙", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bedTimeStr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Bedtime",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Wake Up Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { showWakePicker = true }
            ) {
                Text(text = "☀️", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = wakeTimeStr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Wake Up",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Card 1: Repeat every day (Screenshot 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111522))
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(16.dp))
                .clickable { showScheduleSheet = true }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Repeat every day",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (repeatEveryDay) "Every day" else "Weekdays",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 2: Finish protection setup (Screenshot 1 & 5)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111522))
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Finish protection setup",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = "Fix",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3B82F6),
                modifier = Modifier.clickable { /* open permission settings */ }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Split Row: Wind down | Allowed (Screenshot 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Wind down Card
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111522))
                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(16.dp))
                    .clickable { showWindDownSheet = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.RemoveCircleOutline,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Wind down",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${windDownMin} min",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Allowed Card
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111522))
                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Allowed",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "None",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CTA Pill Button (Screenshot 1: Turn on Sleep Mode -> with lavender/glow gradient)
        Button(
            onClick = {
                coroutineScope.launch { prefs.setSleepMode(!isSleepMode) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSleepMode) Color(0xFF1E2436) else Color(0xFFD8C7F8)
            )
        ) {
            Text(
                text = if (isSleepMode) "Turn off Sleep Mode →" else "Turn on Sleep Mode →",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSleepMode) Color(0xFFEF4444) else Color(0xFF1E163B)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Schedule Modal Bottom Sheet (Screenshot 24)
    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            containerColor = Color(0xFF0F131D),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF334155))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Schedule",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Repeat every day",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    Switch(
                        checked = repeatEveryDay,
                        onCheckedChange = { repeatEveryDay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF1E2538)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(36.dp))
                Button(
                    onClick = { showScheduleSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2538))
                ) {
                    Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Wind Down Modal Bottom Sheet (Screenshot 25)
    if (showWindDownSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWindDownSheet = false },
            containerColor = Color(0xFF0F131D),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF334155))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Wind Down",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bedtime winddown block
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF181E2E))
                            .clickable {
                                showWindDownSheet = false
                                showBedtimePicker = true
                            }
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(text = "Block apps", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${windDownMin}m", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "before bed time", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    // Wakeup winddown block
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF181E2E))
                            .clickable {
                                showWindDownSheet = false
                                showWakePicker = true
                            }
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(text = "Block apps", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${windDownMin}m", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "after you wake up", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showWindDownSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2538))
                ) {
                    Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Wheel Picker for Bedtime Wind Down (Screenshot 26)
    if (showBedtimePicker) {
        ModalBottomSheet(
            onDismissRequest = { showBedtimePicker = false },
            containerColor = Color(0xFF0F131D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Block apps before bed time",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(32.dp))
                // Simulated wheel picker with highlight bars
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "25 min", fontSize = 15.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(1.dp)
                            .background(Color(0xFF334155))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "30 min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(1.dp)
                            .background(Color(0xFF334155))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "35 min", fontSize = 15.sp, color = Color(0xFF64748B))
                }
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { showBedtimePicker = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8), fontSize = 15.sp)
                    }
                    TextButton(onClick = {
                        coroutineScope.launch { prefs.setWindDownMinutes(30) }
                        showBedtimePicker = false
                    }) {
                        Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Wheel Picker for Wakeup Wind Down (Screenshot 27)
    if (showWakePicker) {
        ModalBottomSheet(
            onDismissRequest = { showWakePicker = false },
            containerColor = Color(0xFF0F131D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Block apps after you wake up",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "25 min", fontSize = 15.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(1.dp)
                            .background(Color(0xFF334155))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "30 min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(1.dp)
                            .background(Color(0xFF334155))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "35 min", fontSize = 15.sp, color = Color(0xFF64748B))
                }
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { showWakePicker = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8), fontSize = 15.sp)
                    }
                    TextButton(onClick = {
                        coroutineScope.launch { prefs.setWindDownMinutes(30) }
                        showWakePicker = false
                    }) {
                        Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
