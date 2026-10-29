package com.socialbreak.android.data

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

object UsageStatsHelper {

    data class AppUsageEntry(
        val id: String,
        val name: String,
        val minutesToday: Int
    )

    private val HOME_APPS = listOf(
        Triple("instagram", "Instagram", "com.instagram.android"),
        Triple("youtube", "YouTube", "com.google.android.youtube"),
        Triple("tiktok", "TikTok", "com.zhiliaoapp.musically"),
        Triple("snapchat", "Snapchat", "com.snapchat.android"),
        Triple("facebook", "Facebook", "com.facebook.katana"),
        Triple("x", "X", "com.twitter.android")
    )

    fun getTodaySocialUsage(context: Context): List<AppUsageEntry> {
        val rawStats = AppUsageTracker.getTodayUsageStats(context)
        val byPkg = rawStats.associateBy { it.packageName }
        return HOME_APPS.map { (id, name, pkg) ->
            val millis = byPkg[pkg]?.foregroundTimeMillis ?: 0L
            AppUsageEntry(
                id = id,
                name = name,
                minutesToday = (millis / 60_000L).toInt()
            )
        }
    }

    fun getTodayHourlyBuckets(context: Context): List<Int> {
        if (!AppUsageTracker.hasUsageStatsPermission(context)) {
            return List(24) { 0 }
        }
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return List(24) { 0 }
        val monitoredPkgs = HOME_APPS.map { it.third }.toSet()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startOfDay,
            now
        ) ?: return List(24) { 0 }

        val buckets = IntArray(24)
        for (stat in stats) {
            if (stat.packageName in monitoredPkgs && stat.totalTimeInForeground > 0) {
                val calStat = Calendar.getInstance().apply { timeInMillis = stat.lastTimeUsed.coerceAtLeast(startOfDay) }
                val hr = calStat.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                buckets[hr] += (stat.totalTimeInForeground / 60_000L).toInt()
            }
        }
        return buckets.toList()
    }
}

