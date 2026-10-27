package com.socialbreak.android.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.receivers.UninstallProtectionReceiver
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

val SOCIAL_SITES = listOf(
    "Instagram", "TikTok", "Facebook", "YouTube", "Snapchat", "Twitter",
    "X", "Reddit", "Threads", "Pinterest", "LinkedIn", "Discord",
    "Messenger", "BeReal", "VSCO", "Tumblr"
)

val GAMING_SITES = listOf(
    "1001games.com", "addictinggames.com", "agame.com", "armorgames.com", "chess.com",
    "coolmathgames.com", "crazygames.com", "friv.com", "gamefaqs.com", "gamehouse.com",
    "kick.com", "kizi.com", "kongregate.com", "lichess.org", "miniclip.com",
    "poki.com", "roblox.com", "silvergames.com", "twitch.tv", "y8.com"
)

@Composable
fun SettingsScreen(prefs: PreferencesManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val webAdult by prefs.webBlockingAdult.collectAsState(initial = false)
    val webCasinos by prefs.webBlockingCasinos.collectAsState(initial = false)
    val webGaming by prefs.webBlockingGaming.collectAsState(initial = false)
    val webSocial by prefs.webBlockingSocial.collectAsState(initial = false)

    val navStyle by prefs.navStyle.collectAsState(initial = "Bar")
    val themeMode by prefs.themeMode.collectAsState(initial = "Dark")
    val iconTheme by prefs.iconTheme.collectAsState(initial = "Painted")
    val grayscale by prefs.globalGrayscale.collectAsState(initial = false)

    var socialSitesExpanded by remember { mutableStateOf(false) }
    var gamingSitesExpanded by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(false) }

    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, UninstallProtectionReceiver::class.java)
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // "This Week" 7-Petal Flower Goal Tracker (Screenshot 20, 21, 22)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "This week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 7-Petal Ring
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(BgTertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val days = listOf("S", "M", "T", "W", "T", "F", "S")
                            val center = Offset(size.width / 2, size.height / 2)
                            days.forEachIndexed { index, _ ->
                                val angle = Math.toRadians((index * (360.0 / 7.0)) - 90.0)
                                val x = center.x + (size.width * 0.32f * Math.cos(angle)).toFloat()
                                val y = center.y + (size.height * 0.32f * Math.sin(angle)).toFloat()
                                drawCircle(color = BorderSubtle, radius = 9.dp.toPx(), center = Offset(x, y))
                            }
                        }
                        Text(text = "0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextHighlight)
                    }

                    Spacer(modifier = Modifier.width(18.dp))
                    Column {
                        Text(text = "0 days within your limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Text(text = "First full goal day starts tomorrow", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Give your friends SocialBreak Pro Banner (Screenshot 20)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Give your friends", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(text = "a week of SocialBreak Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                }
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Admin Anti-Uninstall Protection (Screenshots 20, 22)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
                    .clickable {
                        if (!isAdminActive) {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "SocialBreak uses device administrator privileges solely to prevent uninstallation during Hard Mode.")
                            }
                            context.startActivity(intent)
                        } else {
                            dpm.removeActiveAdmin(adminComponent)
                            isAdminActive = false
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = if (isAdminActive) AccentEmerald else TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Stop SocialBreak being deleted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Text(
                            text = if (isAdminActive) "Protected - admin enabled" else "Off - anyone can delete the app",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAdminActive) AccentEmerald else TextMuted
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Appearance & Theme Accordion (Screenshot 2)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { appearanceExpanded = !appearanceExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Appearance & Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                    }
                    Icon(if (appearanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary)
                }

                AnimatedVisibility(visible = appearanceExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(text = "Navigation", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Bubble", "Bar").forEach { style ->
                                FilterChip(
                                    selected = navStyle == style,
                                    onClick = { coroutineScope.launch { prefs.setAppearance(nav = style) } },
                                    label = { Text(style) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "Theme", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Light", "Dark", "System").forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { coroutineScope.launch { prefs.setAppearance(theme = mode) } },
                                    label = { Text(mode) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Grayscale", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Switch(
                                checked = grayscale,
                                onCheckedChange = { coroutineScope.launch { prefs.setAppearance(grayscale = it) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = BgTertiary)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "Icon Theme", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Classic", "Modern", "Painted").forEach { iconT ->
                                FilterChip(
                                    selected = iconTheme == iconT,
                                    onClick = { coroutineScope.launch { prefs.setAppearance(icon = iconT) } },
                                    label = { Text(iconT) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System-Wide Web Blocking (Screenshots 15, 16, 17)
        Text(text = "Web Blocking Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Social Apps Pack (16 sites) - Expandable (Screenshot 16)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).clickable { socialSitesExpanded = !socialSitesExpanded }) {
                        Text(text = "Social apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextHighlight)
                        Text(text = "16 sites ${if (socialSitesExpanded) "▲" else "▼"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Switch(
                        checked = webSocial,
                        onCheckedChange = { coroutineScope.launch { prefs.setWebFilter("social", it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = BgTertiary)
                    )
                }

                AnimatedVisibility(visible = socialSitesExpanded) {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        Text(text = "Keep these reachable", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Text(text = "Chips OFF = blocked. Chips ON = still works.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SOCIAL_SITES) { site ->
                                SuggestionChip(
                                    onClick = { /* toggle exception */ },
                                    label = { Text(site, style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = BgTertiary)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                // Casual Gaming Portals Pack (21 sites) - Expandable (Screenshot 17)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).clickable { gamingSitesExpanded = !gamingSitesExpanded }) {
                        Text(text = "Casual gaming portals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextHighlight)
                        Text(text = "21 sites ${if (gamingSitesExpanded) "▲" else "▼"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Switch(
                        checked = webGaming,
                        onCheckedChange = { coroutineScope.launch { prefs.setWebFilter("gaming", it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = BgTertiary)
                    )
                }

                AnimatedVisibility(visible = gamingSitesExpanded) {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        Text(text = "Sites in this pack", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(GAMING_SITES) { site ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(site, style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = BgTertiary)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                WebBlockRow(
                    title = "Adult content",
                    subtitle = "25 sites",
                    checked = webAdult,
                    onCheckedChange = { coroutineScope.launch { prefs.setWebFilter("adult", it) } }
                )

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                WebBlockRow(
                    title = "Online casinos",
                    subtitle = "24 sites",
                    checked = webCasinos,
                    onCheckedChange = { coroutineScope.launch { prefs.setWebFilter("casinos", it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Menu Items (Help, Account, About) (Screenshot 20)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                MenuItemRow("Help", "FAQ, feedback and support")
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                MenuItemRow("Account", "Browsing data, log out, delete")
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                MenuItemRow("About", "Version 1.0.0, privacy, terms")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reset goals text button (Screenshot 20)
        TextButton(
            onClick = { /* reset */ },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Reset goals", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MenuItemRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun WebBlockRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextHighlight)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = BgTertiary)
        )
    }
}

