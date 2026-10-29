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
        private const val ACTION_DEBOUNCE_MS = 600L

        // Instagram View IDs
        private val INSTAGRAM_REELS_VIEW_IDS = listOf(
            "com.instagram.android:id/clips_viewer_container",
            "com.instagram.android:id/clips_viewer_video_layout",
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_tab"
        )
        private val INSTAGRAM_STORIES_VIEW_IDS = listOf(
            "com.instagram.android:id/reel_viewer",
            "com.instagram.android:id/reel_viewer_media_container",
            "com.instagram.android:id/feed_story_tray"
        )
        private val INSTAGRAM_EXPLORE_VIEW_IDS = listOf(
            "com.instagram.android:id/search_tab",
            "com.instagram.android:id/explore_popular_grid",
            "com.instagram.android:id/search_bar_grid_layout"
        )

        // YouTube View IDs
        private val YOUTUBE_SHORTS_VIEW_IDS = listOf(
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/shorts_player_container",
            "com.google.android.youtube:id/reel_recycler"
        )

        // Snapchat View IDs
        private val SNAPCHAT_SPOTLIGHT_VIEW_IDS = listOf(
            "com.snapchat.android:id/ngs_spotlight_icon_container",
            "com.snapchat.android:id/spotlight_container"
        )

        // Facebook View IDs
        private val FACEBOOK_REELS_VIEW_IDS = listOf(
            "com.facebook.katana:id/video_tab",
            "com.facebook.katana:id/watch_tab"
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
            "direct_tab",
            "direct_thread",
            "message_composer",
            "DirectMessage",
            "chat_thread",
            "inbox"
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
     * Instagram native interception: Reels, Explore, Stories, Feed, and DMs-only mode.
     */
    private fun handleInstagram(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        // 1. DMs-only Mode Enforcement
        if (p.nativeIgDmsOnlyMode || p.igDmsOnly) {
            val isInsideDm = containsAnyViewId(root, DM_SCREEN_TOKENS) || containsAnyText(root, listOf("Direct", "Messages", "Chats"))
            if (!isInsideDm) {
                // If user is outside DMs, check if they are in feed, reels, or profile and redirect
                interceptDistraction("Instagram Non-DM Surface")
                return
            }
        }

        // 2. Reels Interception
        if (p.nativeBlockIgReels || p.igBlockReels) {
            var isReelsActive = containsAnyViewId(root, INSTAGRAM_REELS_VIEW_IDS)
            if (!isReelsActive) {
                val reelNodes = root.findAccessibilityNodeInfosByText("Reels")
                if (reelNodes.isNotEmpty()) {
                    for (node in reelNodes) {
                        if (node.isSelected || node.isClickable) {
                            isReelsActive = true
                            break
                        }
                    }
                }
            }

            if (isReelsActive) {
                // Check if this is a video sent directly by a friend in DMs
                val isFriendClip = containsFriendDMBreadcrumbs(root)
                val allowFriendClip = p.nativeAllowFriendReels && !p.hardModeEnabled

                if (!isFriendClip || !allowFriendClip) {
                    interceptDistraction("Instagram Reels")
                    return
                }
            }
        }

        // 3. Explore / Search Interception
        if (p.nativeBlockIgExplore || p.igBlockExplore) {
            val isExplore = containsAnyViewId(root, INSTAGRAM_EXPLORE_VIEW_IDS)
            if (isExplore) {
                interceptDistraction("Instagram Explore")
                return
            }
        }

        // 4. Stories Interception
        if (p.nativeBlockIgStories || p.igBlockStories) {
            val isStories = containsAnyViewId(root, INSTAGRAM_STORIES_VIEW_IDS)
            if (isStories) {
                interceptDistraction("Instagram Stories")
                return
            }
        }

        // 5. Main Feed Interception
        if (p.nativeBlockIgFeed || p.igBlockFeed) {
            val isFeedTab = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab").any { it.isSelected }
            if (isFeedTab) {
                interceptDistraction("Instagram Feed")
                return
            }
        }
    }

    /**
     * YouTube native interception: Shorts and Search-only mode.
     */
    private fun handleYouTube(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        // 1. Shorts Interception
        if (p.nativeBlockYtShorts || p.ytBlockShorts) {
            var isShorts = containsAnyViewId(root, YOUTUBE_SHORTS_VIEW_IDS)
            if (!isShorts) {
                val shortsNodes = root.findAccessibilityNodeInfosByText("Shorts")
                if (shortsNodes.isNotEmpty()) {
                    for (node in shortsNodes) {
                        if (node.isSelected || node.isClickable) {
                            isShorts = true
                            break
                        }
                    }
                }
            }

            if (isShorts) {
                interceptDistraction("YouTube Shorts")
                return
            }
        }

        // 2. Search-Only Mode Enforcement
        if (p.nativeYtSearchOnlyMode || p.ytSearchOnlyMode) {
            val isSearch = containsAnyViewId(root, listOf("search_edit_text", "search_results", "search_container"))
            val isPlayingVideo = containsAnyViewId(root, listOf("watch_player", "player_view", "movie_player"))
            if (!isSearch && !isPlayingVideo) {
                // If on home recommendations, redirect to search
                val searchButton = root.findAccessibilityNodeInfosByText("Search")
                if (searchButton.isNotEmpty() && searchButton[0].isClickable) {
                    searchButton[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    recordBlockLanded("YouTube Search-Only Redirect")
                } else {
                    interceptDistraction("YouTube Recommendation Feed")
                }
            }
        }
    }

    /**
     * TikTok native interception: For You Feed doomscrolling.
     */
    private fun handleTikTok(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        if (p.nativeBlockTtForYou) {
            // Check if user is in DMs or Profile
            val isDm = containsAnyText(root, listOf("Inbox", "Messages", "Direct message"))
            val isProfile = containsAnyText(root, listOf("Profile", "Edit profile"))

            if (!isDm && !isProfile) {
                // User is in algorithmic main feed
                interceptDistraction("TikTok For You Feed")
            }
        }
    }

    /**
     * Snapchat native interception: Spotlight tab.
     */
    private fun handleSnapchat(root: AccessibilityNodeInfo, p: SocialBreakPreferences) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTimestamp.get() < ACTION_DEBOUNCE_MS) return

        if (p.nativeBlockSnapSpotlight) {
            val isSpotlight = containsAnyViewId(root, SNAPCHAT_SPOTLIGHT_VIEW_IDS) ||
                    root.findAccessibilityNodeInfosByText("Spotlight").any { it.isSelected }
            if (isSpotlight) {
                interceptDistraction("Snapchat Spotlight")
            }
        }
    }

    /**
     * Facebook native interception: Reels & Watch tab.
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
            val isReelsOrVideo = containsAnyViewId(root, FACEBOOK_REELS_VIEW_IDS) ||
                    root.findAccessibilityNodeInfosByText("Reels").any { it.isSelected } ||
                    root.findAccessibilityNodeInfosByText("Watch").any { it.isSelected }
            if (isReelsOrVideo) {
                interceptDistraction("Facebook Reels")
            }
        }
    }

    /**
     * X (Twitter) native interception: Video tab and DMs-only mode.
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
            val isVideoTab = root.findAccessibilityNodeInfosByText("VideoTab").isNotEmpty() ||
                    root.findAccessibilityNodeInfosByText("Video").any { it.isSelected }
            if (isVideoTab) {
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
            if (nodes.isNotEmpty()) return true
        }
        return false
    }

    private fun containsAnyText(root: AccessibilityNodeInfo, texts: List<String>): Boolean {
        for (text in texts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (nodes.isNotEmpty()) return true
        }
        return false
    }

    private fun containsFriendDMBreadcrumbs(node: AccessibilityNodeInfo): Boolean {
        for (token in FRIEND_CONTENT_TOKENS) {
            val matches = node.findAccessibilityNodeInfosByText(token)
            if (matches.isNotEmpty()) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {}
}
