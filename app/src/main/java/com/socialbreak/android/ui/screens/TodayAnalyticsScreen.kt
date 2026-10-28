package com.socialbreak.android.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager

@Composable
fun TodayAnalyticsScreen(
    prefs: PreferencesManager,
    onBack: () -> Unit = {}
) {
    val blocksLanded by prefs.todayBlocksLanded.collectAsState(initial = 0)
    val strippedCount by prefs.todayStrippedCount.collectAsState(initial = 0)
    val hardModeEnabled by prefs.hardModeEnabled.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header (Screenshot 18: <- Today)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Today",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 Arc Gauges Cluster (1:1 with Screenshot 18)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Top Arc Gauge: Blocks Landed
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(175.dp)) {
                    // Outer background arc
                    drawArc(
                        color = Color(0xFF1E2538),
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Active arc if blocks landed
                    if (blocksLanded > 0) {
                        drawArc(
                            color = Color(0xFF60A5FA),
                            startAngle = 140f,
                            sweepAngle = (blocksLanded.coerceAtMost(30) / 30f) * 260f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$blocksLanded",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Blocks landed",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Triple Bottom Arc Gauges Row (Blocked, Hard Mode, left today)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SmallArcGauge(
                    value = "$strippedCount",
                    label = "Blocked"
                )
                SmallArcGauge(
                    value = if (hardModeEnabled) "Active" else "0m",
                    label = "Hard Mode"
                )
                SmallArcGauge(
                    value = "0m",
                    label = "left today"
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Information Card: What these numbers are (Screenshot 18 & 19)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "What these numbers are",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Everything here is counted on this phone for the selected day, and compared with your own last thirty days. The tick on each bar is that average — left of it is a quieter day than usual, right of it is a heavier one. Nothing is scored against anybody else.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 8 Metric List Rows (Screenshots 18 & 19)
        AnalyticsMetricRow("Blocks landed", "$blocksLanded")
        AnalyticsMetricRow("Time in apps", "0m")
        AnalyticsMetricRow("Time left of your limit", "0m")
        AnalyticsMetricRow("Sessions", "0")
        AnalyticsMetricRow("In SocialBreak", "0m")
        AnalyticsMetricRow("Limit hits", "0")
        AnalyticsMetricRow("Pushed through", "0")
        AnalyticsMetricRow("Hard Mode", if (hardModeEnabled) "Active" else "0m")

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SmallArcGauge(
    value: String,
    label: String
) {
    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            drawArc(
                color = Color(0xFF1E2538),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun AnalyticsMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Pill badge: New
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E2538))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "New",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
