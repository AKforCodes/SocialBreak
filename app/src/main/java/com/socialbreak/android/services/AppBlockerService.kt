package com.socialbreak.android.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.socialbreak.android.MainActivity
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
    private val DEBOUNCE_MS = 200L

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
            val now = System.currentTimeMillis()

            val isBlocked = (now < focusBlockUntil || sleepMode) && blockedSet.contains(packageName)

            if (isBlocked) {
                // Intercept and navigate home
                performGlobalAction(GLOBAL_ACTION_HOME)
                prefs.incrementBlocksLanded()

                // Launch SocialBreak blocker alert screen
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("BLOCKED_APP", packageName)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}
