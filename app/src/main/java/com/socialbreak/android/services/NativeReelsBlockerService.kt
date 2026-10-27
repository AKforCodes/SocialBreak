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

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName == "com.instagram.android" || packageName == "com.google.android.youtube") {
            val rootNode = rootInActiveWindow ?: return
            serviceScope.launch {
                val hardMode = prefs.hardModeEnabled.first()
                if (hardMode) {
                    checkAndExitShortForm(rootNode, packageName)
                }
            }
        }
    }

    private fun checkAndExitShortForm(root: AccessibilityNodeInfo, packageName: String) {
        val triggers = when (packageName) {
            "com.instagram.android" -> listOf("Reels", "reels_viewer", "clips_viewer")
            "com.google.android.youtube" -> listOf("Shorts", "reel_watch_fragment", "shorts_container")
            else -> emptyList()
        }

        for (trigger in triggers) {
            val matches = root.findAccessibilityNodeInfosByText(trigger)
            if (matches.isNotEmpty()) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                serviceScope.launch {
                    prefs.incrementStrippedCount()
                }
                return
            }
        }
    }

    override fun onInterrupt() {}
}
