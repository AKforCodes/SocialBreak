package com.socialbreak.android

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.socialbreak.android.data.AppShortcutManager
import com.socialbreak.android.workers.AppBlockerHealthWorker
import java.util.concurrent.TimeUnit

class SocialBreakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppShortcutManager.initDynamicShortcuts(this)

        try {
            val healthWork = PeriodicWorkRequestBuilder<AppBlockerHealthWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "socialbreak_health_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                healthWork
            )
        } catch (_: Exception) {}
    }
}
