package com.socialbreak.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.services.WebBlockerVpnService
import com.socialbreak.android.workers.AppBlockerHealthWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            // 1. Ensure periodic health check worker is scheduled
            try {
                val healthWork = PeriodicWorkRequestBuilder<AppBlockerHealthWorker>(15, TimeUnit.MINUTES).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "socialbreak_health_worker",
                    ExistingPeriodicWorkPolicy.KEEP,
                    healthWork
                )
            } catch (_: Exception) {}

            // 2. Restore local DNS Web Blocking VPN if enabled and VPN permission is already prepared
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = PreferencesManager(context.applicationContext).preferencesFlow.first()
                    val anyWebBlockActive = prefs.webBlockSocialApps ||
                        prefs.webBlockAdultContent ||
                        prefs.webBlockOnlineCasinos ||
                        prefs.webBlockCasualGaming ||
                        prefs.webBlockSportsBetting ||
                        prefs.customBlockedDomains.isNotEmpty()

                    if (anyWebBlockActive && VpnService.prepare(context) == null) {
                        context.startService(Intent(context, WebBlockerVpnService::class.java))
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
