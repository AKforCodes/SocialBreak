package com.socialbreak.android.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.socialbreak.android.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NativeReelsBlockerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private lateinit var prefs: PreferencesManager

    companion object {
        // Reverse-engineered view IDs from classes.dex
        private val INSTAGRAM_VIEW_IDS = listOf(
            "com.instagram.android:id/clips_viewer_container",
            "com.instagram.android:id/clips_viewer_video_layout",
            "com.instagram.android:id/clips_viewer_view_pager"
        )

        private val YOUTUBE_VIEW_IDS = listOf(
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/shorts_player_container",
            "com.google.android.youtube:id/reel_recycler"
        )

        // Reverse-engineered multi-lingual friend-DM breadcrumb tokens
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
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        when (packageName) {
            "com.instagram.android", "com.google.android.youtube",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> {
                val rootNode = rootInActiveWindow ?: return
                serviceScope.launch {
                    val hardMode = prefs.hardModeEnabled.first()
                    inspectAndInterceptReels(rootNode, packageName, hardMode)
                }
            }
        }
    }

    private fun inspectAndInterceptReels(root: AccessibilityNodeInfo, packageName: String, hardMode: Boolean) {
        var isShortFormContainer = false

        when (packageName) {
            "com.instagram.android" -> {
                for (viewId in INSTAGRAM_VIEW_IDS) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
                    if (nodes.isNotEmpty()) {
                        isShortFormContainer = true
                        break
                    }
                }
                if (!isShortFormContainer) {
                    val reelMatches = root.findAccessibilityNodeInfosByText("Reels")
                    val clipsMatches = root.findAccessibilityNodeInfosByText("clips_viewer")
                    if (reelMatches.isNotEmpty() || clipsMatches.isNotEmpty()) {
                        isShortFormContainer = true
                    }
                }
            }
            "com.google.android.youtube" -> {
                for (viewId in YOUTUBE_VIEW_IDS) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
                    if (nodes.isNotEmpty()) {
                        isShortFormContainer = true
                        break
                    }
                }
                if (!isShortFormContainer) {
                    val shortsMatches = root.findAccessibilityNodeInfosByText("Shorts")
                    if (shortsMatches.isNotEmpty()) {
                        isShortFormContainer = true
                    }
                }
            }
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> {
                // TikTok is entirely short-form
                isShortFormContainer = true
            }
        }

        if (isShortFormContainer) {
            // Check if this is a Friend DM clip exception
            val isFriendClip = containsFriendDMBreadcrumbs(root)

            if (!isFriendClip || hardMode) {
                // Algorithmic doomscroll trap detected: strike with Back navigation
                performGlobalAction(GLOBAL_ACTION_BACK)
                serviceScope.launch {
                    prefs.incrementStrippedCount()
                }
            }
        }
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
