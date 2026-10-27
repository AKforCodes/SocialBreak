package com.socialbreak.android.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.socialbreak.android.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationBlockerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private lateinit var prefs: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName

        serviceScope.launch {
            val blockedSet = prefs.blockedPackages.first()
            val focusBlockUntil = prefs.focusBlockUntil.first()
            val sleepMode = prefs.sleepModeEnabled.first()
            val now = System.currentTimeMillis()

            if ((now < focusBlockUntil || sleepMode) && blockedSet.contains(packageName)) {
                // Cancel distracting notification from blocked app
                cancelNotification(sbn.key)
            }
        }
    }
}
