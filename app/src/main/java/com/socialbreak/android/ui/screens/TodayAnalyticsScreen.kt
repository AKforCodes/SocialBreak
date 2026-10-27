package com.socialbreak.android.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*

@Composable
fun TodayAnalyticsScreen(prefs: PreferencesManager) {
    val blocksLanded by prefs.todayBlocksLanded.collectAsState(initial = 0)
    val strippedCount by prefs.todayStrippedCount.collectAsState(initial = 0)
    val blockedPackages by prefs.blockedPackages.collectAsState(initial = emptySet())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Today Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextHighlight,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Large Central Dial (Blocks Landed)
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(BgSecondary)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(170.dp)) {
                drawArc(
                    color = BorderSubtle,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = AccentBlue,
                    startAngle = 135f,
                    sweepAngle = ((blocksLanded.coerceAtMost(30) / 30f) * 270f),
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$blocksLanded",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHighlight
                )
                Text(
                    text = "Blocks landed",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Triple Sub-Dials
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SubDial(value = "${blockedPackages.size}", label = "Shielded")
            SubDial(value = "${strippedCount}", label = "Shorts Trapped")
            SubDial(value = "0m", label = "Over Limit")
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Metric Counters Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Daily Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    "Total Blocks Landed" to "$blocksLanded",
                    "Addictive Feeds Stripped" to "$strippedCount",
                    "Shielded Social Apps" to "${blockedPackages.size}",
                    "Focus Sessions Started" to "0",
                    "Impulse Attempts Intercepted" to "$blocksLanded"
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                    }
                }
            }
        }
    }
}

@Composable
fun SubDial(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(BgSecondary)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
