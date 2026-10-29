package com.socialbreak.android.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.socialbreak.android.MainActivity
import com.socialbreak.android.R
import com.socialbreak.android.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationBlockerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private lateinit var prefs: PreferencesManager

    companion object {
        private const val CHANNEL_REDIRECT_ID = "socialbreak_clean_redirect"
        private const val CHANNEL_REDIRECT_NAME = "Clean Message Alerts"

        private val MONITORED_PACKAGES = mapOf(
            "com.instagram.android" to Pair("instagram", "https://www.instagram.com/direct/inbox/"),
            "com.google.android.youtube" to Pair("youtube", "https://m.youtube.com"),
            "com.zhiliaoapp.musically" to Pair("tiktok", "https://www.tiktok.com/messages"),
            "com.ss.android.ugc.trill" to Pair("tiktok", "https://www.tiktok.com/messages"),
            "com.snapchat.android" to Pair("snapchat", "https://web.snapchat.com"),
            "com.twitter.android" to Pair("twitter", "https://x.com/messages/"),
            "com.twitter.android.lite" to Pair("twitter", "https://x.com/messages/"),
            "com.facebook.katana" to Pair("facebook", "https://m.facebook.com/messages/"),
            "com.facebook.lite" to Pair("facebook", "https://m.facebook.com/messages/"),
            "com.facebook.orca" to Pair("facebook", "https://m.facebook.com/messages/"),
            "com.linkedin.android" to Pair("linkedin", "https://www.linkedin.com/messaging/")
        )
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REDIRECT_ID,
                CHANNEL_REDIRECT_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Clean message notifications without algorithmic feeds"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName

        val redirectTarget = MONITORED_PACKAGES[packageName] ?: return

        serviceScope.launch {
            val currentPrefs = prefs.preferencesFlow.first()
            if (!currentPrefs.interceptNotifications) return@launch
            val extras = sbn.notification?.extras ?: return@launch
            val title = extras.getString("android.title") ?: extras.getString("android.conversationTitle") ?: "New Message"
            val text = extras.getCharSequence("android.text")?.toString() ?: extras.getCharSequence("android.bigText")?.toString() ?: "Tap to open in clean chat"

            // Cancel the native distracting notification
            try {
                cancelNotification(sbn.key)
            } catch (e: Exception) {
                // Silently handle if OEM restricts cancelling
            }

            // Post replacement clean notification
            val (platform, cleanUrl) = redirectTarget
            postCleanReplacement(platform, cleanUrl, title, text, sbn.id)
        }
    }

    private fun postCleanReplacement(platform: String, cleanUrl: String, title: String, text: String, notifId: Int) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("socialbreak://open?platform=$platform&url=${Uri.encode(cleanUrl)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_REDIRECT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("$title (SocialBreak Clean)")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId + 100000, notification)
    }
}
