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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.components.FrictionGauntletDialog
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeDashboardScreen(
    prefs: PreferencesManager,
    onNavigateAnalytics: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isHardMode by prefs.hardModeEnabled.collectAsState(initial = false)
    val strippedCount by prefs.todayStrippedCount.collectAsState(initial = 0)

    var showGauntlet by remember { mutableStateOf(false) }
    var showHardModePrompt by remember { mutableStateOf(false) }
    var preventEasyUninstall by remember { mutableStateOf(true) }

    val todayDateStr = remember {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
    }

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

    // Hard Mode Activation Dialog (1:1 with Screenshot 6)
    if (showHardModePrompt) {
        Dialog(onDismissRequest = { showHardModePrompt = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF000000))
                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(24.dp))
                    .padding(28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Centered Amber Padlock
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Turn on Hard Mode?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Checkbox card: Prevent easy uninstall
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF111522))
                            .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(14.dp))
                            .clickable { preventEasyUninstall = !preventEasyUninstall }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = preventEasyUninstall,
                            onCheckedChange = { preventEasyUninstall = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFF59E0B),
                                uncheckedColor = Color(0xFF64748B)
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Prevent easy uninstall",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Big Orange Pill Button: Continue
                    Button(
                        onClick = {
                            showHardModePrompt = false
                            coroutineScope.launch { prefs.setHardMode(true) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "Continue",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Not yet",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { showHardModePrompt = false }
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "SocialBreak",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header (Screenshot 5: SocialBreak logo + Customize apps)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SocialBreak",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { /* open customize apps sheet */ }
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Customize apps",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Protection Alerts Banner (Screenshot 5: Finish protection setup, Protection alerts, Fix)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111522))
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2538)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Finish protection setup",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "Protection alerts",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            Text(
                text = "Fix",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3B82F6),
                modifier = Modifier.clickable { /* trigger system settings fix */ }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Central Connected Node Graph Cluster (1:1 with Screenshot 5 & 7)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Line 1: Center to Top (YouTube)
                drawLine(
                    color = Color(0xFF1E2538),
                    start = center,
                    end = Offset(center.x, center.y - 85.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )

                // Line 2: Center to Bottom-Left (Instagram)
                drawLine(
                    color = Color(0xFF1E2538),
                    start = center,
                    end = Offset(center.x - 90.dp.toPx(), center.y + 40.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )

                // Line 3: Center to Bottom-Right (TikTok)
                drawLine(
                    color = Color(0xFF1E2538),
                    start = center,
                    end = Offset(center.x + 90.dp.toPx(), center.y + 40.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Top Node: YouTube (Offset: center x, top y)
            Box(
                modifier = Modifier
                    .offset(y = (-85).dp)
                    .align(Alignment.Center)
            ) {
                ConnectedAppNode(
                    gradient = Brush.linearGradient(listOf(Color(0xFFFF0000), Color(0xFFC00000))),
                    iconEmoji = "▶",
                    timerText = "0:00"
                )
            }

            // Bottom-Left Node: Instagram
            Box(
                modifier = Modifier
                    .offset(x = (-90).dp, y = 40.dp)
                    .align(Alignment.Center)
            ) {
                ConnectedAppNode(
                    gradient = Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))),
                    iconEmoji = "📸",
                    timerText = "0:00"
                )
            }

            // Bottom-Right Node: TikTok
            Box(
                modifier = Modifier
                    .offset(x = 90.dp, y = 40.dp)
                    .align(Alignment.Center)
            ) {
                ConnectedAppNode(
                    gradient = Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF111111))),
                    iconEmoji = "🎵",
                    timerText = "0:00"
                )
            }

            // Center Hub Node (Double-curve S logo)
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0C0F17))
                    .border(2.dp, Color(0xFF1F283C), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "§",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Floating App Dock Card (Screenshot 5 & 7)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Secondary apps row: Snapchat, Facebook, X with "No limit"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockAppItem(
                        name = "Snapchat",
                        bgBrush = Brush.linearGradient(listOf(Color(0xFFFFFC00), Color(0xFFEAB308))),
                        emoji = "👻",
                        limit = "No limit"
                    )
                    DockAppItem(
                        name = "Facebook",
                        bgBrush = Brush.linearGradient(listOf(Color(0xFF1877F2), Color(0xFF0C63D4))),
                        emoji = "f",
                        limit = "No limit"
                    )
                    DockAppItem(
                        name = "X",
                        bgBrush = Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF161616))),
                        emoji = "𝕏",
                        limit = "No limit"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // The Signature Orange Pill Button: Hard (Screenshot 5 & 7)
                Button(
                    onClick = {
                        if (isHardMode) showGauntlet = true else showHardModePrompt = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(42.dp),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHardMode) Color(0xFFEA580C) else Color(0xFFF59E0B)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHardMode) "Hard Mode" else "Hard",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Today Card (Screenshot 7: Tue, Oct 27)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateAnalytics() }
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = todayDateStr,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (strippedCount == 0)
                        "Nothing stripped yet today. This fills in as the blocker catches reels and shorts."
                    else
                        "Successfully shielded you from $strippedCount short-form dopamine traps today.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ConnectedAppNode(
    gradient: Brush,
    iconEmoji: String,
    timerText: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(gradient)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconEmoji, fontSize = 24.sp)
            // Small gear badge in top-right
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111522))
                    .align(Alignment.TopEnd)
                    .offset(x = (-3).dp, y = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = timerText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun DockAppItem(
    name: String,
    bgBrush: Brush,
    emoji: String,
    limit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgBrush)
                .border(1.dp, Color(0xFF262F44), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (name == "Facebook" || name == "X") Color.White else Color.Black
            )
            // Small gear badge
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111522))
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(9.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = limit,
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
    }
}
