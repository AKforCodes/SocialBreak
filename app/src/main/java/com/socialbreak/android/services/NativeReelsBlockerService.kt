package com.socialbreak.android.services

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * System-wide Accessibility Service for surgical native app distraction interception.
 * Enforces user preferences outside SocialBreak when native social apps are opened:
 * - Instagram: Reels, Explore, Stories, Feed, and DMs-only enforcement.
 * - YouTube: Shorts and Search-only mode.
 * - TikTok: For You algorithmic doomscroll feed.
 * - Snapchat: Spotlight tab.
 * - Facebook: Reels and Video tabs.
 * - X (Twitter): Video tab and DMs-only mode.
 */
class NativeReelsBlockerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private lateinit var prefs: PreferencesManager

    // Cached preferences for latency-free evaluation on accessibility thread
    @Volatile
    private var cachedPrefs: SocialBreakPreferences = SocialBreakPreferences()

    private val lastActionTimestamp = AtomicLong(0L)

    companion object {
        private const val ACTION_DEBOUNCE_MS = 800L

        // Instagram View IDs for full-screen reel player containers
        private val INSTAGRAM_REELS_CONTAINER_IDS = listOf(
            "com.instagram.android:id/clips_viewer_container",
            "com.instagram.android:id/clips_viewer_video_layout",
            "com.instagram.android:id/clips_viewer_view_pager"
        )
        private val INSTAGRAM_STORIES_CONTAINER_IDS = listOf(
            "com.instagram.android:id/reel_viewer",
            "com.instagram.android:id/reel_viewer_media_container"
        )
        private val INSTAGRAM_EXPLORE_CONTAINER_IDS = listOf(
            "com.instagram.android:id/explore_popular_grid",
            "com.instagram.android:id/search_bar_grid_layout"
        )

        // YouTube View IDs for full-screen shorts containers
        private val YOUTUBE_SHORTS_CONTAINER_IDS = listOf(
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/shorts_player_container",
            "com.google.android.youtube:id/reel_recycler"
        )

        // DM Exemption Tokens for Shared Clips from Friends
        private val FRIEND_CONTENT_TOKENS = listOf(
            "Back to chat",
            "Reply privately",
            "swipe to reply",
            "shared with you",
            "sent by",
            "sent to",
            "sent you",
            "from ",
            "back to",
            "return to",
            "friends",
            "your reel",
            "your story",
            " reels von ",
            " reel di ",
            " original audio",
            "'s reels",
            "audio original",
            " reels de "
        )

        private val DM_SCREEN_TOKENS = listOf(
            "direct_inbox_action_bar",
            "row_thread_composer_edittext",
            "direct_tab",
            "direct_thread",
            "message_composer"
        )
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)

        // Continuously cache preferences in-memory for zero-delay accessibility thread checks
        serviceScope.launch {
            prefs.preferencesFlow.collectLatest { p ->
                cachedPrefs = p
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        val currentPrefs = cachedPrefs
        if (!currentPrefs.nativeReelsBlockerEnabled) return

        val rootNode = rootInActiveWindow ?: return

        when (packageName) {
            "com.instagram.android" -> {
                handleInstagram(rootNode, currentPrefs)
            }
            "com.google.android.youtube" -> {
                handleYouTube(rootNode, currentPrefs)
            }
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> {
                handleTikTok(rootNode, currentPrefs)
            }
            "com.snapchat.android" -> {
                handleSnapchat(rootNode, currentPrefs)
            }
            "com.facebook.katana" -> {
                handleFacebook(rootNode, currentPrefs)
            }
            "com.twitter.android", "com.twitter.android.lite" -> {
                handleTwitter(rootNode, currentPrefs)
            }
        }
    }

    /**
     * Checks if a view is visible to user and occupies a significant portion of the screen
     * (matching reference check A() in decompiled source: width > screenWidth / 2 && height > screenHeight / 2)
     */
    private fun isSignificantFullscreenView(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        return rect.width() > screenWidth / 2 && rect.height() > screenHeight / 2
    }

    /**
     * Instagram surgical interception:
     * - Tapping Reels tab on bottom bar redirects to DMs or Home (does NOT close app).
     * - Opening full-screen Reels viewer backs out unless it's a friend DM clip.
     * - Tapping Explore tab redirects to Home feed if native explore blocking is on.
     * - Fullscreen stories viewer backs out if native stories blocking is on.
     * - DMs-only mode redirects back into DMs if outside.
     */
    private fun handleInstagram(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        // 1. Bottom Navigation Bar Tab Interceptions (Redirects tab without exiting app)
        if (p.nativeBlockIgReels) {
            val clipsTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_tab")
            if (clipsTabs.any { it.isSelected }) {
                // Reels tab is currently selected! Redirect to Direct tab or Home feed tab
                val directTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/direct_tab")
                val targetTab = directTabs.firstOrNull { it.isVisibleToUser }
                    ?: root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab").firstOrNull { it.isVisibleToUser }

                if (targetTab != null && targetTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    lastActionTimestamp.set(now)
                    recordBlockLanded("Instagram Reels Tab Redirect")
                    return
                }
            }
        }

        if (p.nativeBlockIgExplore) {
            val searchTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/search_tab")
            if (searchTabs.any { it.isSelected }) {
                // Explore tab selected: redirect to Feed tab
                val feedTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab")
                val targetTab = feedTabs.firstOrNull { it.isVisibleToUser }
                if (targetTab != null && targetTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    lastActionTimestamp.set(now)
                    recordBlockLanded("Instagram Explore Tab Redirect")
                    return
                }
            }
        }

        // 2. DMs-only Mode Enforcement (Only if explicitly enabled)
        if (p.nativeIgDmsOnlyMode) {
            val isInsideDm = containsAnyViewId(root, DM_SCREEN_TOKENS) ||
                    containsAnyText(root, listOf("Direct", "Messages", "Chats", "Search Messenger"))
            if (!isInsideDm) {
                // Try redirecting into DMs tab first
                val directTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/direct_tab")
                val targetTab = directTabs.firstOrNull { it.isVisibleToUser }
                if (targetTab != null && targetTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    lastActionTimestamp.set(now)
                    recordBlockLanded("Instagram DMs-Only Redirect")
                    return
                } else {
                    interceptDistraction("Instagram Non-DM Surface")
                    return
                }
            }
        }

        // 3. Full-Screen Reels Player Interception
        if (p.nativeBlockIgReels) {
            var isFullscreenReels = false
            for (id in INSTAGRAM_REELS_CONTAINER_IDS) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                for (node in nodes) {
                    if (isSignificantFullscreenView(node)) {
                        isFullscreenReels = true
                        break
                    }
                }
                if (isFullscreenReels) break
            }

            if (isFullscreenReels) {
                // Check if this is a video sent directly by a friend in DMs
                val isFriendClip = containsFriendDMBreadcrumbs(root)
                val allowFriendClip = p.nativeAllowFriendReels && !p.hardModeEnabled

                if (!isFriendClip || !allowFriendClip) {
                    interceptDistraction("Instagram Reels")
                    return
                }
            }
        }

        // 4. Explore Fullscreen Content
        if (p.nativeBlockIgExplore) {
            for (id in INSTAGRAM_EXPLORE_CONTAINER_IDS) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (nodes.any { isSignificantFullscreenView(it) }) {
                    interceptDistraction("Instagram Explore")
                    return
                }
            }
        }

        // 5. Stories Fullscreen Viewer
        if (p.nativeBlockIgStories) {
            for (id in INSTAGRAM_STORIES_CONTAINER_IDS) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (nodes.any { isSignificantFullscreenView(it) }) {
                    interceptDistraction("Instagram Stories")
                    return
                }
            }
        }

        // 6. Main Feed Blocking (Only if nativeBlockIgFeed is explicitly turned on)
        if (p.nativeBlockIgFeed) {
            val feedTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab")
            if (feedTabs.any { it.isSelected }) {
                // Redirect to DMs instead of killing the app
                val directTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/direct_tab")
                val targetTab = directTabs.firstOrNull { it.isVisibleToUser }
                if (targetTab != null && targetTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    lastActionTimestamp.set(now)
                    recordBlockLanded("Instagram Feed Block")
                    return
                }
            }
        }
    }

    /**
     * YouTube surgical interception:
     * - Tapping Shorts tab on pivot bar redirects to Home.
     * - Fullscreen Shorts player backs out.
     */
    private fun handleYouTube(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        // 1. Shorts Interception
        if (p.nativeBlockYtShorts) {
            // Check if Shorts tab is currently selected in bottom bar
            val shortsTabs = root.findAccessibilityNodeInfosByText("Shorts")
            val isShortsTabSelected = shortsTabs.any { it.isSelected && it.isVisibleToUser }

            // Check if full-screen shorts video player is active
            var isFullscreenShorts = false
            for (id in YOUTUBE_SHORTS_CONTAINER_IDS) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                for (node in nodes) {
                    if (isSignificantFullscreenView(node)) {
                        isFullscreenShorts = true
                        break
                    }
                }
                if (isFullscreenShorts) break
            }

            if (isShortsTabSelected || isFullscreenShorts) {
                // Try redirecting to Home tab on pivot bar first so app doesn't exit
                val homeTabs = root.findAccessibilityNodeInfosByText("Home")
                val targetTab = homeTabs.firstOrNull { it.isClickable && !it.isSelected && it.isVisibleToUser }
                if (targetTab != null && targetTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    lastActionTimestamp.set(now)
                    recordBlockLanded("YouTube Shorts Redirect")
                    return
                } else {
                    interceptDistraction("YouTube Shorts")
                    return
                }
            }
        }

        // 2. Search-Only Mode Enforcement
        if (p.nativeYtSearchOnlyMode) {
            val isSearch = containsAnyViewId(root, listOf("search_edit_text", "search_results", "search_container"))
            val isPlayingVideo = containsAnyViewId(root, listOf("watch_player", "player_view", "movie_player"))
            if (!isSearch && !isPlayingVideo) {
                val searchButtons = root.findAccessibilityNodeInfosByText("Search")
                val targetSearch = searchButtons.firstOrNull { it.isClickable && it.isVisibleToUser }
                if (targetSearch != null && targetSearch.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    lastActionTimestamp.set(now)
                    recordBlockLanded("YouTube Search-Only Redirect")
                    return
                }
            }
        }
    }

    /**
     * TikTok surgical interception:
     * - Intercepts full-screen For You feed video player.
     * - Preserves DMs, Inbox, and Profile.
     */
    private fun handleTikTok(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        if (p.nativeBlockTtForYou) {
            // Check if user is in DMs or Profile
            val isDm = containsAnyText(root, listOf("Inbox", "Messages", "Direct message"))
            val isProfile = containsAnyText(root, listOf("Profile", "Edit profile"))
            if (isDm || isProfile) return

            // Check if main video pager is full screen
            val viewPagers = root.findAccessibilityNodeInfosByViewId("com.zhiliaoapp.musically:id/viewpager")
            if (viewPagers.any { isSignificantFullscreenView(it) }) {
                interceptDistraction("TikTok For You Feed")
            }
        }
    }

    /**
     * Snapchat surgical interception:
     * - Intercepts Spotlight tab only when selected.
     * - Redirects to Chat or Camera tab.
     */
    private fun handleSnapchat(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        if (p.nativeBlockSnapSpotlight) {
            val spotlightIcons = root.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/ngs_spotlight_icon_container")
            val isSpotlightSelected = spotlightIcons.any { it.isSelected }

            var isSpotlightFullscreen = false
            val containers = root.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/spotlight_container")
            for (node in containers) {
                if (isSignificantFullscreenView(node)) {
                    isSpotlightFullscreen = true
                    break
                }
            }

            if (isSpotlightSelected || isSpotlightFullscreen) {
                // Switch to Chat icon if present
                val chatTabs = root.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/ngs_chat_icon_container")
                val targetTab = chatTabs.firstOrNull { it.isVisibleToUser }
                if (targetTab != null && targetTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    lastActionTimestamp.set(now)
                    recordBlockLanded("Snapchat Spotlight Redirect")
                    return
                } else {
                    interceptDistraction("Snapchat Spotlight")
                }
            }
        }
    }

    /**
     * Facebook surgical interception:
     * - Intercepts Video/Watch tab only when selected.
     */
    private fun handleFacebook(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        if (p.nativeFbDmsOnlyMode) {
            val isMessages = containsAnyText(root, listOf("Messenger", "Chats", "Search Messenger"))
            if (!isMessages) {
                interceptDistraction("Facebook Non-DM Surface")
                return
            }
        }

        if (p.nativeBlockFacebookReels) {
            val videoTabs = root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_tab")
            val isVideoTabSelected = videoTabs.any { it.isSelected }

            var isFullscreenVideo = false
            val watchTabs = root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/watch_tab")
            for (node in watchTabs) {
                if (isSignificantFullscreenView(node)) {
                    isFullscreenVideo = true
                    break
                }
            }

            if (isVideoTabSelected || isFullscreenVideo) {
                interceptDistraction("Facebook Reels")
            }
        }
    }

    /**
     * X (Twitter) surgical interception:
     * - Intercepts Video tab only when selected.
     */
    private fun handleTwitter(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        if (p.nativeXDmsOnlyMode) {
            val isDms = containsAnyText(root, listOf("Messages", "Direct Messages", "Start a message"))
            if (!isDms) {
                interceptDistraction("X Non-DM Surface")
                return
            }
        }

        if (p.nativeBlockXVideo) {
            val videoTabs = root.findAccessibilityNodeInfosByText("VideoTab")
            val isVideoSelected = videoTabs.any { it.isSelected }
            if (isVideoSelected) {
                interceptDistraction("X Video Tab")
            }
        }
    }

    /**
     * Executes back navigation to surgically exit the distracting section and increments stats.
     */
    private fun interceptDistraction(featureName: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return
        lastActionTimestamp.set(now)

        performGlobalAction(GLOBAL_ACTION_BACK)
        recordBlockLanded(featureName)
    }

    private fun recordBlockLanded(featureName: String) {
        serviceScope.launch {
            prefs.incrementBlocksLanded()
        }
    }

    private fun containsAnyViewId(root: AccessibilityNodeInfo, ids: List<String>): Boolean {
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.any { it.isVisibleToUser }) return true
        }
        return false
    }

    private fun containsAnyText(root: AccessibilityNodeInfo, texts: List<String>): Boolean {
        for (text in texts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (nodes.any { it.isVisibleToUser }) return true
        }
        return false
    }

    private fun containsFriendDMBreadcrumbs(node: AccessibilityNodeInfo): Boolean {
        for (token in FRIEND_CONTENT_TOKENS) {
            val matches = node.findAccessibilityNodeInfosByText(token)
            if (matches.any { it.isVisibleToUser }) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {}
}
