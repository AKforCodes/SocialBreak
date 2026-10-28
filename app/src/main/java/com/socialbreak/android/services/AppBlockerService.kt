package com.socialbreak.android.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.socialbreak.android.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppBlockerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private lateinit var prefs: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
    }

    private var lastEventTime = 0L
    private val DEBOUNCE_MS = 300L

    companion object {
        private val PACKAGE_TO_PLATFORM = mapOf(
            "com.instagram.android" to "instagram",
            "com.google.android.youtube" to "youtube",
            "com.zhiliaoapp.musically" to "tiktok",
            "com.ss.android.ugc.trill" to "tiktok",
            "com.snapchat.android" to "snapchat",
            "com.twitter.android" to "twitter",
            "com.twitter.android.lite" to "twitter",
            "com.facebook.katana" to "facebook",
            "com.facebook.lite" to "facebook",
            "com.linkedin.android" to "linkedin"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val now = System.currentTimeMillis()
        if (now - lastEventTime < DEBOUNCE_MS) return
        lastEventTime = now

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app package
        if (packageName == applicationContext.packageName) return

        serviceScope.launch {
            val blockedSet = prefs.blockedPackages.first()
            val focusBlockUntil = prefs.focusBlockUntil.first()
            val sleepMode = prefs.sleepModeEnabled.first()
            val currentTime = System.currentTimeMillis()

            val isFocusBlocked = currentTime < focusBlockUntil && blockedSet.contains(packageName)
            val isSleepBlocked = sleepMode && blockedSet.contains(packageName)
            val isDirectlyBlocked = blockedSet.contains(packageName)

            if (isFocusBlocked || isSleepBlocked) {
                // Determine reason
                val reason = when {
                    isSleepBlocked -> "sleep_block"
                    isFocusBlocked -> "focus_block"
                    else -> "limit_block"
                }

                val platform = PACKAGE_TO_PLATFORM[packageName] ?: packageName

                // Increment metric
                prefs.incrementBlocksLanded()

                // Launch dedicated BlockedAppActivity interceptor
                val intent = Intent(applicationContext, BlockedAppActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("platform", platform)
                    putExtra("reason", reason)
                    putExtra("package_name", packageName)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}
