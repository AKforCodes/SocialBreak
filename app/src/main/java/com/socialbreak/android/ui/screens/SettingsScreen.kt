package com.socialbreak.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.GrayscaleController
import com.socialbreak.android.data.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    prefs: PreferencesManager,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val navStyle by prefs.navStyle.collectAsState(initial = "Bar")
    val themeMode by prefs.themeMode.collectAsState(initial = "System")
    val iconTheme by prefs.iconTheme.collectAsState(initial = "Painted")
    val grayscale by prefs.globalGrayscale.collectAsState(initial = false)

    var appearanceExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header (Screenshot 2: <- Settings)
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
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Group 1: Appearance Card (Screenshot 2)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Appearance Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { appearanceExpanded = !appearanceExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Appearance",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Icon(
                        if (appearanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = appearanceExpanded) {
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        // 1. Navigation (Screenshot 2)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(text = "Navigation", fontSize = 13.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "i", fontSize = 9.sp, color = Color.White)
                            }
                        }
                        // Segmented Control: [ Bubble | Bar ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Color(0xFF181F30))
                                .border(1.dp, Color(0xFF242E44), RoundedCornerShape(9999.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SegmentedButton(
                                text = "Bubble",
                                isSelected = navStyle == "Bubble",
                                modifier = Modifier.weight(1f)
                            ) {
                                coroutineScope.launch { prefs.setNavStyle("Bubble") }
                            }
                            SegmentedButton(
                                text = "Bar",
                                isSelected = navStyle == "Bar",
                                modifier = Modifier.weight(1f)
                            ) {
                                coroutineScope.launch { prefs.setNavStyle("Bar") }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2. Theme (Screenshot 2)
                        Text(text = "Theme", fontSize = 13.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Segmented Control: [ Light | Dark | System ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Color(0xFF181F30))
                                .border(1.dp, Color(0xFF242E44), RoundedCornerShape(9999.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SegmentedButton(
                                text = "Light",
                                isSelected = themeMode == "Light",
                                modifier = Modifier.weight(1f)
                            ) {
                                coroutineScope.launch { prefs.setThemeMode("Light") }
                            }
                            SegmentedButton(
                                text = "Dark",
                                isSelected = themeMode == "Dark",
                                modifier = Modifier.weight(1f)
                            ) {
                                coroutineScope.launch { prefs.setThemeMode("Dark") }
                            }
                            SegmentedButton(
                                text = "System",
                                isSelected = themeMode == "System",
                                modifier = Modifier.weight(1f)
                            ) {
                                coroutineScope.launch { prefs.setThemeMode("System") }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Grayscale (Screenshot 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Grayscale", fontSize = 14.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "i", fontSize = 9.sp, color = Color.White)
                                }
                            }

                            Switch(
                                checked = grayscale,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        prefs.setGlobalGrayscale(it)
                                        GrayscaleController.setHardwareGrayscale(context, it)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3B82F6),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF1E2538)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. Icon Theme (Screenshot 2)
                        Text(text = "Icon Theme", fontSize = 13.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Color(0xFF181F30))
                                .border(1.dp, Color(0xFF242E44), RoundedCornerShape(9999.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SegmentedButton(
                                text = "Classic",
                                isSelected = iconTheme == "Classic",
                                modifier = Modifier.weight(1f)
                            ) {
                                coroutineScope.launch { prefs.setIconTheme("Classic") }
                            }
                            SegmentedButton(
                                text = "Modern",
                                isSelected = iconTheme == "Modern",
                                modifier = Modifier.weight(1f)
                            ) {
                                coroutineScope.launch { prefs.setIconTheme("Modern") }
                            }
                            SegmentedButton(
                                text = "Painted",
                                isSelected = iconTheme == "Painted",
                                modifier = Modifier.weight(1f)
                            ) {
                                coroutineScope.launch { prefs.setIconTheme("Painted") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Group 2: App Launch Card (Screenshot 2)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(18.dp))
                .clickable { /* app launch sub-settings */ },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "App Launch",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Group 3: Notifications Card (Screenshot 2)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(18.dp))
                .clickable { /* notifications sub-settings */ },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Notifications",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SegmentedButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(9999.dp))
            .background(if (isSelected) Color(0xFF26334D) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) Color(0xFF3B82F6) else Color.Transparent,
                RoundedCornerShape(9999.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}
