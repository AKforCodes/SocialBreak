package com.socialbreak.android.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.viewinterop.AndroidView
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAppsScreen(
    prefs: PreferencesManager,
    onNavigateWebBlocking: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var activeUrl by remember { mutableStateOf<String?>(null) }
    var selectedApp by remember { mutableStateOf("instagram") } // "instagram" or "youtube"
    var instagramExpanded by remember { mutableStateOf(true) }
    var youtubeExpanded by remember { mutableStateOf(false) }

    // Instagram prefs
    val igBlockStories by prefs.igBlockStories.collectAsState(initial = false)
    val igBlockSaved by prefs.igBlockSaved.collectAsState(initial = false)
    val igBlockFeed by prefs.igBlockFeed.collectAsState(initial = false)
    val igDmsOnly by prefs.igDmsOnly.collectAsState(initial = false)
    val igGrayscale by prefs.igGrayscale.collectAsState(initial = false)
    val igBlockAds by prefs.igBlockAds.collectAsState(initial = true)
    val igBlockSuggested by prefs.igBlockSuggested.collectAsState(initial = true)

    // YouTube prefs
    val ytBlockShorts by prefs.ytBlockShorts.collectAsState(initial = true)
    val ytAllowChannelShorts by prefs.ytAllowChannelShorts.collectAsState(initial = false)
    val ytStopAutoplay by prefs.ytStopAutoplay.collectAsState(initial = true)
    val ytSearchOnly by prefs.ytSearchOnly.collectAsState(initial = false)
    val ytHideRecommendations by prefs.ytHideRecommendations.collectAsState(initial = false)
    val ytHideComments by prefs.ytHideComments.collectAsState(initial = false)
    val ytBlockAds by prefs.ytBlockAds.collectAsState(initial = true)
    val ytCaptionsFullscreen by prefs.ytCaptionsFullscreen.collectAsState(initial = true)
    val ytKeepAudioPlaying by prefs.ytKeepAudioPlaying.collectAsState(initial = true)
    val ytGrayscale by prefs.ytGrayscale.collectAsState(initial = false)

    if (activeUrl != null) {
        // Active Clean In-App Web Client
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111522))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeUrl = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (activeUrl!!.contains("instagram")) "Clean Instagram" else "Clean YouTube",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                try {
                                    val script = ctx.assets.open("kid_web_controls.js").bufferedReader().use { it.readText() }
                                    view?.evaluateJavascript(script, null)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        loadUrl(activeUrl!!)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Main Screen (1:1 with Screenshots 8, 9, 10, 11, 12, 14)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header (Screenshot 8: Inside SocialBreak + Gear)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inside SocialBreak",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(
                    onClick = { /* settings */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add to Home Screen Banner (Screenshot 8, 10: green icon + arrow)
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
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF064E3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add to Home Screen",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large App Selectors (Instagram & YouTube) (Screenshot 8 & 10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instagram Big Card
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF833AB4),
                                        Color(0xFFFD1D1D),
                                        Color(0xFFFCB045)
                                    )
                                )
                            )
                            .clickable {
                                selectedApp = "instagram"
                                activeUrl = "https://www.instagram.com"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📸", fontSize = 44.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Instagram",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selectedApp == "instagram") {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                }

                // YouTube Big Card
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF262C3A))
                            .border(1.dp, Color(0xFF333D52), RoundedCornerShape(26.dp))
                            .clickable {
                                selectedApp = "youtube"
                                activeUrl = "https://m.youtube.com"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "▶", fontSize = 38.sp, color = Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "YouTube",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selectedApp == "youtube") {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the selected logo to open.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
            )

            // Instagram Settings Accordion (Screenshot 8, 10, 11)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { instagramExpanded = !instagramExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Instagram settings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Icon(
                            if (instagramExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    // Two Gradient Cards (Screenshot 8 & 10: Block Ads & Block Suggested Posts)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Block Ads (Coral to Purple)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFE11D48), Color(0xFF9333EA))
                                    )
                                )
                                .clickable {
                                    coroutineScope.launch { prefs.setIgBlockAds(!igBlockAds) }
                                }
                                .padding(12.dp)
                        ) {
                            if (igBlockAds) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                }
                            }
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(text = "⃠", fontSize = 16.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Block Ads", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Card 2: Block Suggested Posts (Purple to Orange)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF7C3AED), Color(0xFFEA580C))
                                    )
                                )
                                .clickable {
                                    coroutineScope.launch { prefs.setIgBlockSuggested(!igBlockSuggested) }
                                }
                                .padding(12.dp)
                        ) {
                            if (igBlockSuggested) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                }
                            }
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Block Suggested Posts", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Expandable Toggle Rows (Screenshot 11)
                    AnimatedVisibility(visible = instagramExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            // Sub-notice bubble
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF181F30))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Language, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "These settings only affect apps inside SocialBreak.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Locked Rows: Block Reels & Block Explore (Always On)
                            LockedAlwaysOnRow("Block Reels")
                            LockedAlwaysOnRow("Block Explore")

                            // Toggles
                            SettingsToggleRow("Block Stories", igBlockStories) { coroutineScope.launch { prefs.setIgBlockStories(it) } }
                            SettingsToggleRow("Block Saved Posts", igBlockSaved) { coroutineScope.launch { prefs.setIgBlockSaved(it) } }
                            SettingsToggleRow("Block Feed", igBlockFeed) { coroutineScope.launch { prefs.setIgBlockFeed(it) } }
                            SettingsToggleRow("DMs Only", igDmsOnly) { coroutineScope.launch { prefs.setIgDmsOnly(it) } }
                            SettingsToggleRow("Grayscale", igGrayscale) { coroutineScope.launch { prefs.setIgGrayscale(it) } }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* manage accounts */ }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = "Manage Accounts", fontSize = 14.sp, color = Color.White)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // YouTube Settings Accordion (Screenshot 9, 12, 14)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { youtubeExpanded = !youtubeExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "YouTube settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(if (youtubeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color(0xFF94A3B8))
                    }

                    AnimatedVisibility(visible = youtubeExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF181F30))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Language, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "These settings only affect apps inside SocialBreak.", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Content", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))

                            SettingsToggleRow("Block Shorts", ytBlockShorts) { coroutineScope.launch { prefs.setYtBlockShorts(it) } }
                            SettingsToggleRow(
                                title = "Allow Shorts on channel pages",
                                checked = ytAllowChannelShorts,
                                subtitle = "Watch a Short you choose on a channel. Swiping to another returns you to the channel."
                            ) { coroutineScope.launch { prefs.setYtAllowChannelShorts(it) } }
                            SettingsToggleRow("Stop feed autoplay", ytStopAutoplay) { coroutineScope.launch { prefs.setYtStopAutoplay(it) } }
                            SettingsToggleRow("Search Only Mode", ytSearchOnly) { coroutineScope.launch { prefs.setYtSearchOnly(it) } }
                            SettingsToggleRow("Hide recommendations below videos", ytHideRecommendations) { coroutineScope.launch { prefs.setYtHideRecommendations(it) } }
                            SettingsToggleRow("Hide comments", ytHideComments) { coroutineScope.launch { prefs.setYtHideComments(it) } }
                            SettingsToggleRow("Block ads", ytBlockAds) { coroutineScope.launch { prefs.setYtBlockAds(it) } }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Playback", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))

                            SettingsToggleRow("Captions in fullscreen", ytCaptionsFullscreen) { coroutineScope.launch { prefs.setYtCaptionsFullscreen(it) } }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "When leaving YouTube", fontSize = 14.sp, color = Color.White)
                                Text(text = "Keep audio playing", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3B82F6))
                            }

                            SettingsToggleRow("Grayscale", ytGrayscale) { coroutineScope.launch { prefs.setYtGrayscale(it) } }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* manage accounts */ }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = "Manage Accounts", fontSize = 14.sp, color = Color.White)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // More controls Section (Screenshot 8 & 10)
            Text(text = "More controls", fontSize = 12.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(10.dp))

            // Web Blocking Card (Screenshot 10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111522))
                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(16.dp))
                    .clickable { onNavigateWebBlocking() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E3A8A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌐", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Web Blocking", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Block sites system-wide", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LockedAlwaysOnRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF064E3B)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔒", fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 14.sp, color = Color.White)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Always On", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, fontSize = 14.sp, color = Color.White)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFF64748B))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3B82F6),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E2538)
            )
        )
    }
}
