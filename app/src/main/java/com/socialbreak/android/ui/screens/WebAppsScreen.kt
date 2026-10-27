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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAppsScreen(prefs: PreferencesManager) {
    val coroutineScope = rememberCoroutineScope()
    var activeUrl by remember { mutableStateOf<String?>(null) }
    var selectedApp by remember { mutableStateOf("instagram") } // "instagram" or "youtube"
    var instagramExpanded by remember { mutableStateOf(false) }
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
        // Active Clean WebView
        Column(modifier = Modifier.fillMaxSize().background(BgPrimary)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgSecondary)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeUrl = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHighlight)
                }
                Text(
                    text = if (activeUrl!!.contains("instagram")) "Clean Instagram" else "Clean YouTube",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighlight
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
        // Main Inside SocialBreak Screen (Screenshots 8, 9, 10, 11, 12, 14)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inside SocialBreak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighlight
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add to Home Screen Banner (Screenshot 8, 10)
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
                Text(text = "Add to Home Screen", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large App Selectors (Instagram & YouTube) (Screenshot 8, 10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instagram Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (selectedApp == "instagram") Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)))
                            else Brush.linearGradient(listOf(BgSecondary, BgSecondary))
                        )
                        .border(1.dp, if (selectedApp == "instagram") AccentBlue else BorderSubtle, RoundedCornerShape(24.dp))
                        .clickable {
                            selectedApp = "instagram"
                            activeUrl = "https://www.instagram.com"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "IG", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Text(text = "Instagram", style = MaterialTheme.typography.labelSmall, color = TextHighlight)
                    }
                }

                // YouTube Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (selectedApp == "youtube") Brush.linearGradient(listOf(Color(0xFFFF0000), Color(0xFFCC0000)))
                            else Brush.linearGradient(listOf(BgSecondary, BgSecondary))
                        )
                        .border(1.dp, if (selectedApp == "youtube") AccentCoral else BorderSubtle, RoundedCornerShape(24.dp))
                        .clickable {
                            selectedApp = "youtube"
                            activeUrl = "https://m.youtube.com"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "YT", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Text(text = "YouTube", style = MaterialTheme.typography.labelSmall, color = TextHighlight)
                    }
                }
            }

            Text(
                text = "Tap the selected logo to open.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp).align(Alignment.CenterHorizontally)
            )

            // Instagram Settings Accordion (Screenshots 8, 10, 11)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { instagramExpanded = !instagramExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Instagram settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Icon(if (instagramExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Block Ads & Block Suggested Posts Gradient Cards (Screenshot 8, 10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFE1306C))))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(text = "Block Ads", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                            }
                            Icon(Icons.Default.Check, contentDescription = null, tint = TextHighlight, modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFE1306C), Color(0xFFFD1D1D))))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(text = "Block Suggested", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHighlight)
                            }
                            Icon(Icons.Default.Check, contentDescription = null, tint = TextHighlight, modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
                        }
                    }

                    AnimatedVisibility(visible = instagramExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(text = "These settings only affect apps inside SocialBreak.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(12.dp))
                            AlwaysOnRow("Block Reels")
                            AlwaysOnRow("Block Explore")
                            ToggleRow("Block Stories", igBlockStories) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_IG_BLOCK_STORIES, it) } }
                            ToggleRow("Block Saved Posts", igBlockSaved) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_IG_BLOCK_SAVED, it) } }
                            ToggleRow("Block Feed", igBlockFeed) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_IG_BLOCK_FEED, it) } }
                            ToggleRow("DMs Only", igDmsOnly) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_IG_DMS_ONLY, it) } }
                            ToggleRow("Grayscale", igGrayscale) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_IG_GRAYSCALE, it) } }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // YouTube Settings Accordion (Screenshots 9, 12, 14)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { youtubeExpanded = !youtubeExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "YouTube settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighlight)
                        Icon(if (youtubeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary)
                    }

                    AnimatedVisibility(visible = youtubeExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(text = "Content", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            ToggleRow("Block Shorts", ytBlockShorts) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_BLOCK_SHORTS, it) } }
                            ToggleRow("Allow Shorts on channel pages", ytAllowChannelShorts) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_ALLOW_CHANNEL_SHORTS, it) } }
                            Text(text = "Watch a Short you choose on a channel. Swiping to another returns you to the channel.", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp))
                            ToggleRow("Stop feed autoplay", ytStopAutoplay) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_STOP_AUTOPLAY, it) } }
                            ToggleRow("Search Only Mode", ytSearchOnly) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_SEARCH_ONLY, it) } }
                            ToggleRow("Hide recommendations below videos", ytHideRecommendations) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_HIDE_RECOMMENDATIONS, it) } }
                            ToggleRow("Hide comments", ytHideComments) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_HIDE_COMMENTS, it) } }
                            ToggleRow("Block ads", ytBlockAds) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_BLOCK_ADS, it) } }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(text = "Playback", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            ToggleRow("Captions in fullscreen", ytCaptionsFullscreen) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_CAPTIONS_FULLSCREEN, it) } }
                            ToggleRow("When leaving YouTube: Keep audio playing", ytKeepAudioPlaying) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_KEEP_AUDIO_PLAYING, it) } }
                            ToggleRow("Grayscale", ytGrayscale) { coroutineScope.launch { prefs.setPlatformToggle(PreferencesManager.KEY_YT_GRAYSCALE, it) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlwaysOnRow(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Always On", style = MaterialTheme.typography.labelSmall, color = AccentEmerald)
        }
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = BgTertiary)
        )
    }
}
