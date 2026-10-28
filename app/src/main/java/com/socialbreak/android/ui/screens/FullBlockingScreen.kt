package com.socialbreak.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.outlined.Apps
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

data class FocusSprintCard(
    val duration: String,
    val title: String,
    val minutes: Int,
    val gradient: List<Color>
)

val SPRINT_CARDS = listOf(
    FocusSprintCard("25 min", "focus sprint", 25, listOf(Color(0xFF382318), Color(0xFF1E1714), Color(0xFF12141A))),
    FocusSprintCard("1 hr", "study session", 60, listOf(Color(0xFF282838), Color(0xFF181C26), Color(0xFF10141D))),
    FocusSprintCard("2 hr", "deep work", 120, listOf(Color(0xFF1E2838), Color(0xFF141E28), Color(0xFF0F141E))),
    FocusSprintCard("4 hr", "half day", 240, listOf(Color(0xFF382A1C), Color(0xFF241C16), Color(0xFF12151C))),
    FocusSprintCard("8 hr", "work day", 480, listOf(Color(0xFF203224), Color(0xFF14241A), Color(0xFF0E1814)))
)

data class SelectableApp(
    val name: String,
    val packageName: String,
    val iconColor: Color,
    val iconEmoji: String
)

val SOCIAL_APPS = listOf(
    SelectableApp("Instagram", "com.instagram.android", Color(0xFFE1306C), "📸"),
    SelectableApp("YouTube", "com.google.android.youtube", Color(0xFFFF0000), "▶"),
    SelectableApp("TikTok", "com.zhiliaoapp.musically", Color(0xFF000000), "🎵"),
    SelectableApp("Snapchat", "com.snapchat.android", Color(0xFFFFFC00), "👻"),
    SelectableApp("X", "com.twitter.android", Color(0xFF000000), "𝕏"),
    SelectableApp("Facebook", "com.facebook.katana", Color(0xFF1877F2), "f"),
    SelectableApp("LinkedIn", "com.linkedin.android", Color(0xFF0A66C2), "in")
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
    var showSessionsMode by remember { mutableStateOf(false) }

    val isCurrentlyBlocked = System.currentTimeMillis() < focusBlockUntil

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Top Header (Screenshot 4 & 29: Full App Blocking + Info + Gear)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Full App Blocking",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showExplainerSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { showAppPickerSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Customize",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Two Top Square Cards (Screenshot 4 & 29: Social & Other apps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Social Card (Clickable -> Opens Social selection sheet Screenshot 30)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF111522))
                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(18.dp))
                    .clickable { showAppPickerSheet = true }
                    .padding(16.dp)
            ) {
                Column {
                    Icon(
                        Icons.Outlined.Apps,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Social",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "${blockedPackages.size} of 7 on",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Other apps Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF111522))
                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Icon(
                        Icons.Outlined.Apps,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Other apps",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "0 apps",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle between Timer View (Screenshot 29) and Focus Sprints View (Screenshot 4)
        if (!showSessionsMode) {
            // -------------------------------------------------------------
            // View A: Giant Timer View (Screenshot 29)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                // Giant 30:00 Timer Typography
                Text(
                    text = if (isCurrentlyBlocked) "ACTIVE" else "${selectedMinutes}:00",
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.02).sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Stepper Row: [ ∞ | - | 30 min | + ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "∞",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier
                            .clickable { selectedMinutes = 120 }
                            .padding(end = 24.dp)
                    )

                    // Minus button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111522))
                            .border(1.dp, Color(0xFF1E2538), CircleShape)
                            .clickable { if (selectedMinutes > 5) selectedMinutes -= 5 },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Minus", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "$selectedMinutes min",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    // Plus button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111522))
                            .border(1.dp, Color(0xFF1E2538), CircleShape)
                            .clickable { selectedMinutes += 5 },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Swipe indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showSessionsMode = true }
                ) {
                    Text(
                        text = "swipe for sessions",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF475569)))
                }
            }
        } else {
            // -------------------------------------------------------------
            // View B: Focus Sprint Photo Cards View (Screenshot 4)
            // -------------------------------------------------------------
            Column(modifier = Modifier.fillMaxWidth()) {
                // Card 1: 25 min focus sprint (Full width hero card)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(SPRINT_CARDS[0].gradient))
                        .border(1.dp, if (selectedMinutes == 25) Color(0xFF60A5FA) else Color(0xFF2A221C), RoundedCornerShape(18.dp))
                        .clickable { selectedMinutes = 25 }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(text = "25 min", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "focus sprint", fontSize = 12.sp, color = Color(0xFFCBD5E1), modifier = Modifier.padding(bottom = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: 1 hr study session | 2 hr deep work
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(SPRINT_CARDS[1].gradient))
                            .border(1.dp, if (selectedMinutes == 60) Color(0xFF60A5FA) else Color(0xFF1E2538), RoundedCornerShape(16.dp))
                            .clickable { selectedMinutes = 60 }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(text = "1 hr", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "study session", fontSize = 11.sp, color = Color(0xFFCBD5E1), modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(SPRINT_CARDS[2].gradient))
                            .border(1.dp, if (selectedMinutes == 120) Color(0xFF60A5FA) else Color(0xFF1E2538), RoundedCornerShape(16.dp))
                            .clickable { selectedMinutes = 120 }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(text = "2 hr", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "deep work", fontSize = 11.sp, color = Color(0xFFCBD5E1), modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 3: 4 hr half day | 8 hr work day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(SPRINT_CARDS[3].gradient))
                            .border(1.dp, if (selectedMinutes == 240) Color(0xFF60A5FA) else Color(0xFF1E2538), RoundedCornerShape(16.dp))
                            .clickable { selectedMinutes = 240 }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(text = "4 hr", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "half day", fontSize = 11.sp, color = Color(0xFFCBD5E1), modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(SPRINT_CARDS[4].gradient))
                            .border(1.dp, if (selectedMinutes == 480) Color(0xFF60A5FA) else Color(0xFF1E2538), RoundedCornerShape(16.dp))
                            .clickable { selectedMinutes = 480 }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(text = "8 hr", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "work day", fontSize = 11.sp, color = Color(0xFFCBD5E1), modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dots indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSessionsMode = false }
                ) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF475569)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Start 30 min block Button (Screenshot 4 & 29)
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
                .height(54.dp),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCurrentlyBlocked) Color(0xFFDC2626) else Color(0xFF161B29)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCurrentlyBlocked) "Stop Block" else "Start $selectedMinutes min block",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Info Explainer Bottom Sheet (Screenshot 3)
    if (showExplainerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExplainerSheet = false },
            containerColor = Color(0xFF0F131D),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Full App Blocking",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "This blocks whole apps outright. A blocked app will not open at all - not the feed, not messages, nothing. The Block Box is the softer version: the app opens, only the short-form inside it is stripped.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "With Hard Mode on, switching this off - or switching Hard Mode itself off - takes a four-stage process: type a commitment, answer five maths questions, wait out a 60-second cooldown, then confirm. There is no timer here; off stays off until you turn it back on.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Got it",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clickable { showExplainerSheet = false }
                            .padding(12.dp)
                    )
                }
            }
        }
    }

    // Social Apps Selection Bottom Sheet (Screenshot 30)
    if (showAppPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppPickerSheet = false },
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
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Social",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap an app to include it in blocking.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3-Column App Icon Grid (Screenshot 30)
                val rows = listOf(
                    SOCIAL_APPS.subList(0, 3),
                    SOCIAL_APPS.subList(3, 6),
                    SOCIAL_APPS.subList(6, 7)
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (app in row) {
                            val isBlocked = blockedPackages.contains(app.packageName)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            val current = blockedPackages.toMutableSet()
                                            if (isBlocked) current.remove(app.packageName)
                                            else current.add(app.packageName)
                                            prefs.setBlockedPackages(current)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isBlocked) Color(0xFF1E2538) else Color(0xFF151926))
                                        .border(
                                            2.dp,
                                            if (isBlocked) Color(0xFF3B82F6) else Color(0xFF262F44),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = app.iconEmoji, fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = app.name,
                                    fontSize = 12.sp,
                                    color = if (isBlocked) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                        if (row.size < 3) {
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.width(60.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
