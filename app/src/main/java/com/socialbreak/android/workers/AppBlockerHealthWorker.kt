package com.socialbreak.android.workers

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.socialbreak.android.data.PreferencesManager

class AppBlockerHealthWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)

        return try {
            val am = applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledList = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

            var appBlockerAlive = false
            var reelsBlockerAlive = false

            for (service in enabledList) {
                val id = service.id
                if (id.contains("AppBlockerService")) appBlockerAlive = true
                if (id.contains("NativeReelsBlockerService")) reelsBlockerAlive = true
            }

            val needsAttention = !appBlockerAlive || !reelsBlockerAlive
            prefs.setServiceHealthWarning(needsAttention)

            Result.success()
        } catch (e: Exception) {
            // Keep periodic worker alive even if exception thrown
            Result.success()
        }
    }
}
