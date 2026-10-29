package com.socialbreak.android.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.socialbreak.android.data.DomainFilterRepository
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
        if (packageName == applicationContext.packageName) return

        serviceScope.launch {
            val currentPrefs = prefs.preferencesFlow.first()
            val socialMeta = DomainFilterRepository.SOCIAL_APPS.find { it.nativePackage == packageName }
            val appId = socialMeta?.id ?: PACKAGE_TO_PLATFORM[packageName]
            val displayName = socialMeta?.displayName ?: packageName

            val isSelectedSocial = appId != null && currentPrefs.blockedSocialApps.contains(appId)
            val isSelectedOther = currentPrefs.blockedOtherApps.any {
                packageName.contains(it, ignoreCase = true) || displayName.equals(it, ignoreCase = true)
            }
            val isTargetBlockedApp = isSelectedSocial || isSelectedOther

            if (!isTargetBlockedApp) return@launch

            val isFocusBlocked = currentPrefs.fullBlockActive &&
                (currentPrefs.untilStoppedMode || now < currentPrefs.fullBlockEndTimeMillis)
            val isSleepBlocked = currentPrefs.sleepModeEnabled &&
                currentPrefs.isWithinSleepWindow() &&
                !currentPrefs.sleepAllowedApps.contains(displayName)

            if (isFocusBlocked || isSleepBlocked) {
                val reason = if (isSleepBlocked) "sleep_block" else "focus_block"
                val platform = appId ?: packageName

                prefs.incrementBlocksLanded()

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
