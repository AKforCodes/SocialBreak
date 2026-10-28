package com.socialbreak.android.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

object AppUsageTracker {

    data class AppUsageInfo(
        val packageName: String,
        val appName: String,
        val foregroundTimeMillis: Long
    )

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsageStats(context: Context): List<AppUsageInfo> {
        if (!hasUsageStatsPermission(context)) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            now
        ) ?: return emptyList()

        val monitoredPackages = DomainFilterRepository.SOCIAL_APPS.associate { it.nativePackage to it.displayName }

        return stats.filter { monitoredPackages.containsKey(it.packageName) && it.totalTimeInForeground > 0 }
            .map {
                AppUsageInfo(
                    packageName = it.packageName,
                    appName = monitoredPackages[it.packageName] ?: it.packageName,
                    foregroundTimeMillis = it.totalTimeInForeground
                )
            }
            .sortedByDescending { it.foregroundTimeMillis }
    }

    fun getWeeklyCompliance(context: Context, dailyLimitMillis: Long = 45 * 60 * 1000L): List<Boolean> {
        // Returns 7 booleans for Sunday through Saturday
        if (!hasUsageStatsPermission(context)) {
            return List(7) { false }
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return List(7) { false }
        val monitored = DomainFilterRepository.SOCIAL_APPS.map { it.nativePackage }.toSet()

        val results = mutableListOf<Boolean>()
        val cal = Calendar.getInstance()

        // Scan the past 7 days
        for (dayOffset in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val dayEnd = cal.timeInMillis

            val dayStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                dayStart,
                dayEnd
            )

            val totalSocialTime = dayStats?.filter { monitored.contains(it.packageName) }
                ?.sumOf { it.totalTimeInForeground } ?: 0L

            results.add(totalSocialTime in 1..dailyLimitMillis)
        }

        return results
    }
}
