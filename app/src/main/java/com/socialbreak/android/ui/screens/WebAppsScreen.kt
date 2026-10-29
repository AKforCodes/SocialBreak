package com.socialbreak.android.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.socialbreak.android.MainActivity
import com.socialbreak.android.R
import com.socialbreak.android.data.DomainFilterRepository
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.services.YouTubePlaybackService
import com.socialbreak.android.ui.components.SocialAppIcon
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAppsScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    initialPlatformId: String? = null,
    onClearInitialPlatform: () -> Unit = {},
    onNavigateSettings: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = SocialTheme.colors

    var activeWebViewPlatform by remember { mutableStateOf<String?>(initialPlatformId) }
    var igExpanded by remember { mutableStateOf(false) }
    var ytExpanded by remember { mutableStateOf(true) }
    var showAddHomeShortcutSheet by remember { mutableStateOf(false) }
    var showManageAccountsSheetFor by remember { mutableStateOf<String?>(null) }
    var stayStrongLockedFeature by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialPlatformId) {
        if (initialPlatformId != null) {
            activeWebViewPlatform = initialPlatformId
        }
    }

    // Track time spent inside SocialLite WebApps
    LaunchedEffect(activeWebViewPlatform) {
        while (activeWebViewPlatform != null) {
            delay(60_000L)
            prefsManager.incrementInSocialLiteMinutes(1)
        }
    }

    // Active Clean WebView Mode
    if (activeWebViewPlatform != null) {
        val platformId = activeWebViewPlatform!!
        val appMeta = DomainFilterRepository.SOCIAL_APPS.find { it.id == platformId }
            ?: DomainFilterRepository.SOCIAL_APPS.first()

        val initialUrl = when (platformId) {
            "instagram" -> if (prefs.igDmsOnly) "https://www.instagram.com/direct/inbox/" else "https://www.instagram.com/"
            "youtube" -> if (prefs.ytSearchOnlyMode) "https://m.youtube.com/results?search_query=" else "https://m.youtube.com/"
            else -> appMeta.cleanUrl
        }

        val isPlatformGrayscale = prefs.globalGrayscale ||
            (platformId == "instagram" && prefs.igGrayscale) ||
            (platformId == "youtube" && prefs.ytGrayscale)

        var webViewRef by remember { mutableStateOf<WebView?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        DisposableEffect(platformId, prefs.ytKeepAudioPlaying) {
            if (platformId == "youtube" && prefs.ytKeepAudioPlaying) {
                try {
                    context.startService(Intent(context, YouTubePlaybackService::class.java))
                } catch (_: Exception) {}
            }
            onDispose {
                if (platformId == "youtube" && !prefs.ytKeepAudioPlaying) {
                    try {
                        context.stopService(Intent(context, YouTubePlaybackService::class.java))
                    } catch (_: Exception) {}
                }
            }
        }

        BackHandler {
            val wv = webViewRef
            if (wv != null && wv.canGoBack()) {
                wv.goBack()
            } else {
                activeWebViewPlatform = null
                onClearInitialPlatform()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack)
                .grayscaleLayer(isPlatformGrayscale)
        ) {
            // Minimalist Clean WebView Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101016))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            activeWebViewPlatform = null
                            onClearInitialPlatform()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back to apps",
                            tint = Color.White
                        )
                    }
                    SocialAppIcon(
                        appId = appMeta.id,
                        iconTheme = prefs.iconTheme,
                        size = 26.dp,
                        cornerRadius = 7.dp
                    )
                    Column {
                        Text(
                            text = appMeta.displayName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Protected inside SocialLite",
                            color = EmeraldSuccess,
                            fontSize = 11.sp
                        )
                    }
                }
                Row {
                    IconButton(onClick = { showManageAccountsSheetFor = appMeta.displayName }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Account Settings",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reload",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF6366F1),
                    trackColor = Color(0xFF14141C)
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = (platformId == "youtube" && prefs.ytStopAutoplay)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url?.toString()?.lowercase() ?: return false
                                if (platformId == "instagram") {
                                    if (reqUrl.contains("/reels") || reqUrl.contains("/reel/")) {
                                        scope.launch { prefsManager.incrementBlocksLanded() }
                                        return true
                                    }
                                    if (prefs.igBlockExplore && reqUrl.contains("/explore")) {
                                        scope.launch { prefsManager.incrementBlocksLanded() }
                                        return true
                                    }
                                }
                                if (platformId == "youtube" && prefs.ytBlockShorts && reqUrl.contains("/shorts/")) {
                                    scope.launch { prefsManager.incrementBlocksLanded() }
                                    return true
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                view ?: return

                                // Inject live user configuration + distraction-stripping CSS/JS
                                val liveConfigJs = """
                                    (function() {
                                        window.__SOCIALLITE_CONFIG__ = {
                                            platform: "$platformId",
                                            igBlockReels: true,
                                            igBlockExplore: true,
                                            igBlockStories: ${prefs.igBlockStories},
                                            igBlockSavedPosts: ${prefs.igBlockSavedPosts},
                                            igBlockFeed: ${prefs.igBlockFeed},
                                            igDmsOnly: ${prefs.igDmsOnly},
                                            igBlockAds: ${prefs.igBlockAds},
                                            igBlockSuggestedPosts: ${prefs.igBlockSuggestedPosts},
                                            ytBlockShorts: ${prefs.ytBlockShorts},
                                            ytAllowChannelShorts: ${prefs.ytAllowChannelShorts},
                                            ytStopAutoplay: ${prefs.ytStopAutoplay},
                                            ytSearchOnlyMode: ${prefs.ytSearchOnlyMode},
                                            ytHideRecommendations: ${prefs.ytHideRecommendations},
                                            ytHideComments: ${prefs.ytHideComments},
                                            ytBlockAds: ${prefs.ytBlockAds}
                                        };
                                        var s = document.getElementById('sociallite-live-rules');
                                        if (!s) {
                                            s = document.createElement('style');
                                            s.id = 'sociallite-live-rules';
                                            document.head.appendChild(s);
                                        }
                                        var css = '';
                                        if ("$platformId" === "instagram") {
                                            css += 'a[href*="/reels"], a[href*="/explore"] { display: none !important; } ';
                                            if (${prefs.igBlockStories}) css += 'div[role="menu"], section main > div > div > div > ul { display: none !important; } ';
                                            if (${prefs.igBlockFeed} || ${prefs.igDmsOnly}) css += 'article { display: none !important; } ';
                                        }
                                        if ("$platformId" === "youtube") {
                                            if (${prefs.ytBlockShorts}) css += 'ytm-reel-shelf-renderer, a[href*="/shorts/"], ytm-pivot-bar-item-renderer:nth-child(2) { display: none !important; } ';
                                            if (${prefs.ytHideRecommendations}) css += 'ytm-item-section-renderer[section-identifier="related-items"] { display: none !important; } ';
                                            if (${prefs.ytHideComments}) css += 'ytm-comment-section-renderer { display: none !important; } ';
                                        }
                                        s.textContent = css;
                                    })();
                                """.trimIndent()
                                view.evaluateJavascript(liveConfigJs, null)

                                injectAssetScript(ctx, view, "kid_web_controls.js")
                                when (platformId) {
                                    "instagram" -> {
                                        injectAssetScript(ctx, view, "kid_instagram_polish.js")
                                        injectAssetScript(ctx, view, "instagram_comments_open.js")
                                        injectAssetScript(ctx, view, "instagram_comments_panel.js")
                                        injectAssetScript(ctx, view, "instagram_feed_play_icon.js")
                                        injectAssetScript(ctx, view, "instagram_page_motion.js")
                                        injectAssetScript(ctx, view, "instagram_post_zoom.js")
                                    }
                                    "youtube" -> injectAssetScript(ctx, view, "youtube/personal.js")
                                    "facebook" -> injectAssetScript(ctx, view, "kid_facebook_controls.js")
                                    "snapchat" -> injectAssetScript(ctx, view, "kid_snapchat_polish.js")
                                    "reddit" -> injectAssetScript(ctx, view, "kid_reddit_controls.js")
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(initialUrl)
                        webViewRef = this
                    }
                }
            )
        }
        return
    }

    val isInstagramSelected = prefs.selectedWebAppPlatform != "youtube"

    // Views 8, 9, 10, 11, 12, 14: "Inside SocialLite" Screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Header: "Inside SocialLite" + Gear Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inside SocialLite",
                color = palette.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = palette.textSecondary,
                modifier = Modifier
                    .size(21.dp)
                    .clickable { onNavigateSettings() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // "Add to Home Screen ->" Banner Card (Views 8 & 10)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardBackground)
                .clickable { showAddHomeShortcutSheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF222330)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Add to Home Screen",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quick access to your clean apps",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Dual Hero App Selector Logos (Instagram | YouTube) matching Views 8 & 10
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Instagram Hero Card
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .alpha(if (isInstagramSelected) 1f else 0.42f)
                    .clip(RoundedCornerShape(30.dp))
                    .clickable {
                        if (isInstagramSelected) {
                            activeWebViewPlatform = "instagram"
                        } else {
                            scope.launch { prefsManager.setSelectedWebAppPlatform("instagram") }
                        }
                    }
            ) {
                SocialAppIcon(
                    appId = "instagram",
                    iconTheme = prefs.iconTheme,
                    size = 126.dp,
                    cornerRadius = 30.dp
                )
            }

            // Right: YouTube Hero Card
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .alpha(if (!isInstagramSelected) 1f else 0.42f)
                    .clip(RoundedCornerShape(30.dp))
                    .clickable {
                        if (!isInstagramSelected) {
                            activeWebViewPlatform = "youtube"
                        } else {
                            scope.launch { prefsManager.setSelectedWebAppPlatform("youtube") }
                        }
                    }
            ) {
                SocialAppIcon(
                    appId = "youtube",
                    iconTheme = prefs.iconTheme,
                    size = 126.dp,
                    cornerRadius = 30.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Tap the selected logo to open",
            color = palette.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (isInstagramSelected) {
            // Views 8, 10, 11: Instagram Settings Accordion + 2 Painted Artwork Tiles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { igExpanded = !igExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Instagram settings",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (igExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = palette.textSecondary
                    )
                }

                if (igExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Info Banner (View 11)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1D1E2B))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFF8E91A6),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "These settings only affect apps inside SocialLite.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Block Reels (Always On)
                    AlwaysOnSettingRow(
                        label = "Block Reels",
                        onClick = { stayStrongLockedFeature = "Block Reels" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Block Explore (Always On)
                    AlwaysOnSettingRow(
                        label = "Block Explore",
                        onClick = { stayStrongLockedFeature = "Block Explore" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WebAppToggleRow(
                        label = "Block Stories",
                        checked = prefs.igBlockStories,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.IG_BLOCK_STORIES, it) }
                        }
                    )
                    WebAppToggleRow(
                        label = "Block Saved Posts",
                        checked = prefs.igBlockSavedPosts,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.IG_BLOCK_SAVED_POSTS, it) }
                        }
                    )
                    WebAppToggleRow(
                        label = "Block Feed",
                        checked = prefs.igBlockFeed,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.IG_BLOCK_FEED, it) }
                        }
                    )
                    WebAppToggleRow(
                        label = "DMs Only",
                        checked = prefs.igDmsOnly,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.IG_DMS_ONLY, it) }
                        }
                    )
                    WebAppToggleRow(
                        label = "Grayscale",
                        checked = prefs.igGrayscale,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.IG_GRAYSCALE, it) }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManageAccountsSheetFor = "Instagram" }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Manage Accounts", color = palette.textPrimary, fontSize = 14.sp)
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2 Square Painted Artwork Cards: Block Ads | Block Suggested Posts (Views 8, 10, 11)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaintedFilterTileCard(
                    title = "Block Ads",
                    drawableRes = R.drawable.tile_bg_block_ads,
                    icon = Icons.Outlined.Block,
                    checked = prefs.igBlockAds,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            prefsManager.setBooleanPreference(PreferencesManager.IG_BLOCK_ADS, !prefs.igBlockAds)
                        }
                    }
                )
                PaintedFilterTileCard(
                    title = "Block Suggested Posts",
                    drawableRes = R.drawable.tile_bg_block_suggested,
                    icon = Icons.Outlined.VisibilityOff,
                    checked = prefs.igBlockSuggestedPosts,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            prefsManager.setBooleanPreference(
                                PreferencesManager.IG_BLOCK_SUGGESTED_POSTS,
                                !prefs.igBlockSuggestedPosts
                            )
                        }
                    }
                )
            }
        } else {
            // Views 9, 12, 14: YouTube Settings Accordion
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ytExpanded = !ytExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YouTube settings",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (ytExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = palette.textSecondary
                    )
                }

                if (ytExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Content",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    WebAppToggleRow(
                        label = "Block Shorts",
                        checked = prefs.ytBlockShorts,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.YT_BLOCK_SHORTS, it) }
                        }
                    )
                    WebAppToggleRow(
                        label = "Allow Shorts on channel pages",
                        subtitle = "Still blocked in Home, Search and the Shorts tab",
                        checked = prefs.ytAllowChannelShorts,
                        onCheckedChange = {
                            scope.launch {
                                prefsManager.setBooleanPreference(PreferencesManager.YT_ALLOW_CHANNEL_SHORTS, it)
                            }
                        }
                    )
                    WebAppToggleRow(
                        label = "Stop feed autoplay",
                        checked = prefs.ytStopAutoplay,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.YT_STOP_AUTOPLAY, it) }
                        }
                    )
                    WebAppToggleRow(
                        label = "Search Only Mode",
                        checked = prefs.ytSearchOnlyMode,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.YT_SEARCH_ONLY, it) }
                        }
                    )
                    WebAppToggleRow(
                        label = "Hide recommendations below videos",
                        checked = prefs.ytHideRecommendations,
                        onCheckedChange = {
                            scope.launch {
                                prefsManager.setBooleanPreference(PreferencesManager.YT_HIDE_RECOMMENDATIONS, it)
                            }
                        }
                    )
                    WebAppToggleRow(
                        label = "Hide comments",
                        checked = prefs.ytHideComments,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.YT_HIDE_COMMENTS, it) }
                        }
                    )
                    WebAppToggleRow(
                        label = "Block ads",
                        checked = prefs.ytBlockAds,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.YT_BLOCK_ADS, it) }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Playback",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    WebAppToggleRow(
                        label = "Captions in fullscreen",
                        checked = prefs.ytCaptionsFullscreen,
                        onCheckedChange = {
                            scope.launch {
                                prefsManager.setBooleanPreference(PreferencesManager.YT_CAPTIONS_FULLSCREEN, it)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    prefsManager.setBooleanPreference(
                                        PreferencesManager.YT_KEEP_AUDIO_PLAYING,
                                        !prefs.ytKeepAudioPlaying
                                    )
                                }
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("When leaving YouTube", color = palette.textPrimary, fontSize = 14.sp)
                        Text(
                            text = if (prefs.ytKeepAudioPlaying) "Keep audio playing" else "Pause audio",
                            color = Color(0xFF60A5FA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    WebAppToggleRow(
                        label = "Grayscale",
                        checked = prefs.ytGrayscale,
                        onCheckedChange = {
                            scope.launch { prefsManager.setBooleanPreference(PreferencesManager.YT_GRAYSCALE, it) }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManageAccountsSheetFor = "YouTube" }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Manage Accounts", color = palette.textPrimary, fontSize = 14.sp)
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Add to Home Screen Shortcut Picker Sheet
    if (showAddHomeShortcutSheet) {
        val shortcutApps = listOf(
            "instagram" to "Instagram",
            "youtube" to "YouTube",
            "tiktok" to "TikTok",
            "snapchat" to "Snapchat",
            "facebook" to "Facebook",
            "x" to "X"
        )
        ModalBottomSheet(
            onDismissRequest = { showAddHomeShortcutSheet = false },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 30.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add SocialLite Apps to Home Screen",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Select an app to pin a clean shortcut directly to your home screen.",
                            color = Color(0xFF8E91A6),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Done",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showAddHomeShortcutSheet = false }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                shortcutApps.forEach { (appId, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1D1E2B))
                            .clickable {
                                pinCleanAppShortcut(context, appId, label)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SocialAppIcon(
                                appId = appId,
                                iconTheme = prefs.iconTheme,
                                size = 36.dp,
                                cornerRadius = 10.dp
                            )
                            Column {
                                Text(
                                    text = "Clean $label",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Open $label in SocialLite",
                                    color = Color(0xFF8E91A6),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = "Add",
                            color = Color(0xFF818CF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Manage Accounts Bottom Sheet
    showManageAccountsSheetFor?.let { platformLabel ->
        ModalBottomSheet(
            onDismissRequest = { showManageAccountsSheetFor = null },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 30.dp)
            ) {
                Text(
                    text = "$platformLabel Accounts",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your $platformLabel session cookie stays strictly on this phone and is never shared.",
                    color = Color(0xFF8E91A6),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Primary Session", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(prefs.accountEmail, color = Color(0xFF8E91A6), fontSize = 12.sp)
                    }
                    Text("Active", color = EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF252738))
                        .clickable {
                            val targetId = platformLabel.lowercase()
                            showManageAccountsSheetFor = null
                            activeWebViewPlatform = if (targetId.contains("youtube")) "youtube" else "instagram"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add Another Account / Switch", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2A181C))
                        .clickable {
                            try {
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                                WebStorage.getInstance().deleteAllData()
                            } catch (_: Exception) {}
                            Toast.makeText(context, "Cleared $platformLabel session cookies", Toast.LENGTH_SHORT).show()
                            showManageAccountsSheetFor = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log out of $platformLabel",
                        color = CrimsonDanger,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // "Stay Strong" Locked Feature Dialog
    stayStrongLockedFeature?.let { featureName ->
        Dialog(onDismissRequest = { stayStrongLockedFeature = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Stay Strong",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$featureName is always blocked to help you stay focused. This can't be turned off.",
                    color = Color(0xFFA0A3B5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF6366F1))
                        .clickable { stayStrongLockedFeature = null },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AlwaysOnSettingRow(
    label: String,
    onClick: () -> Unit
) {
    val palette = SocialTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.textPrimary, fontSize = 14.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = EmeraldSuccess,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Always On",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun WebAppToggleRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = SocialTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = label,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6366F1),
                uncheckedThumbColor = Color(0xFF8E91A6),
                uncheckedTrackColor = palette.switchTrackOff
            )
        )
    }
}

@Composable
private fun PaintedFilterTileCard(
    title: String,
    drawableRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        // Top-right white checkmark circle
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(22.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(if (checked) Color.White else Color.Black.copy(alpha = 0.45f))
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF111118),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Bottom-left icon + title
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )
        }
    }
}

private fun pinCleanAppShortcut(context: android.content.Context, appId: String, label: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            val iconRes = when (appId) {
                "instagram" -> R.drawable.ic_instagram_painted
                "youtube" -> R.drawable.ic_youtube_painted
                "tiktok" -> R.drawable.ic_tiktok_painted
                "snapchat" -> R.drawable.ic_snapchat_painted
                "facebook" -> R.drawable.ic_facebook_painted
                else -> R.drawable.ic_twitter_painted
            }
            val shortcutInfo = ShortcutInfo.Builder(context, "pinned_clean_$appId")
                .setShortLabel(label)
                .setLongLabel("Open $label in SocialLite")
                .setIcon(Icon.createWithResource(context, iconRes))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse("socialbreak://open?platform=$appId")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                .build()
            shortcutManager.requestPinShortcut(shortcutInfo, null)
            Toast.makeText(context, "Adding Clean $label to Home Screen…", Toast.LENGTH_SHORT).show()
            return
        }
    }
    Toast.makeText(context, "Shortcut registered in app launcher menu!", Toast.LENGTH_SHORT).show()
}

private fun injectAssetScript(context: android.content.Context, webView: WebView, assetPath: String) {
    try {
        val script = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        webView.evaluateJavascript(script, null)
    } catch (_: Exception) {
    }
}
