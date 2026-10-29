package com.socialbreak.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.dataStore by preferencesDataStore(name = "socialbreak_settings")

data class SocialBreakPreferences(
    // Sleep Mode
    val sleepModeEnabled: Boolean = false,
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val windDownMinutes: Int = 30,
    val beforeBedWindDownEnabled: Boolean = false,
    val beforeBedWindDownMinutes: Int = 30,
    val afterWakeWindDownEnabled: Boolean = false,
    val afterWakeWindDownMinutes: Int = 30,
    val sleepRepeatEveryDay: Boolean = true,
    val sleepScheduleDays: Set<String> = setOf("0", "1", "2", "3", "4", "5", "6"),
    val sleepAllowedApps: Set<String> = emptySet(),

    // Full App Blocking & Focus Sprints
    val fullBlockActive: Boolean = false,
    val fullBlockEndTimeMillis: Long = 0L,
    val untilStoppedMode: Boolean = false,
    val selectedBlockDurationMinutes: Int = 30,
    val blockedSocialApps: Set<String> = DomainFilterRepository.SOCIAL_APPS.map { it.id }.toSet(),
    val blockedOtherApps: Set<String> = emptySet(),

    // Hard Mode & Uninstall Protection
    val hardModeEnabled: Boolean = false,
    val hardModePreventUninstall: Boolean = true,
    val hardModeActivatedTimestamp: Long = 0L,
    val uninstallProtectionEnabled: Boolean = false,

    // Global Appearance & Navigation Settings
    val navigationStyle: String = "bar", // "bubble", "bar"
    val themeMode: String = "dark", // "light", "dark", "system"
    val globalGrayscale: Boolean = false,
    val iconTheme: String = "painted", // "classic", "modern", "painted"
    val interceptNotifications: Boolean = true,
    val autoLaunchEnabled: Boolean = false,
    val autoLaunchPlatform: String = "instagram",

    // Selected WebApp in Inside SocialLite
    val selectedWebAppPlatform: String = "instagram",

    // Instagram WebApp Settings
    val igBlockReels: Boolean = true,
    val igBlockExplore: Boolean = true,
    val igBlockStories: Boolean = false,
    val igBlockSavedPosts: Boolean = false,
    val igBlockFeed: Boolean = false,
    val igDmsOnly: Boolean = false,
    val igGrayscale: Boolean = false,
    val igBlockAds: Boolean = true,
    val igBlockSuggestedPosts: Boolean = true,

    // YouTube WebApp Settings
    val ytBlockShorts: Boolean = true,
    val ytAllowChannelShorts: Boolean = false,
    val ytStopAutoplay: Boolean = false,
    val ytSearchOnlyMode: Boolean = false,
    val ytHideRecommendations: Boolean = false,
    val ytHideComments: Boolean = false,
    val ytBlockAds: Boolean = false,
    val ytCaptionsFullscreen: Boolean = false,
    val ytKeepAudioPlaying: Boolean = true,
    val ytGrayscale: Boolean = false,

    // Native App Surface Interception
    val nativeReelsBlockerEnabled: Boolean = true,
    val nativeBlockIgReels: Boolean = true,
    val nativeBlockYtShorts: Boolean = true,
    val nativeBlockTtForYou: Boolean = true,
    val nativeBlockSnapSpotlight: Boolean = true,

    // Per-App Daily Short-Form Allowance (minutes, 0 = No limit)
    val appDailyLimitsRaw: String = "",
    val hiddenHomeApps: Set<String> = emptySet(),

    // Web / DNS Domain Blocking
    val webBlockSocialApps: Boolean = false,
    val webBlockAdultContent: Boolean = false,
    val webBlockOnlineCasinos: Boolean = false,
    val webBlockCasualGaming: Boolean = false,
    val webBlockSportsBetting: Boolean = false,
    val customBlockedDomains: Set<String> = emptySet(),
    val webWhitelistedDomains: Set<String> = emptySet(),

    // Profile, Account, & Daily Goal Stats
    val accountEmail: String = "akinphil2005@gmail.com",
    val dailyGoalMinutes: Int = 30,
    val currentStreakDays: Int = 0,
    val blocksLandedToday: Int = 0,
    val inSocialLiteMinutesToday: Int = 0,
    val sessionsToday: Int = 0,
    val limitHitsToday: Int = 0,
    val pushedThroughToday: Int = 0
) {
    fun isCurrentlyBlocked(): Boolean {
        val now = System.currentTimeMillis()
        if (fullBlockActive) {
            if (untilStoppedMode || fullBlockEndTimeMillis > now) return true
        }
        if (sleepModeEnabled && isWithinSleepWindow()) return true
        return false
    }

    fun getAppDailyLimitMinutes(appId: String): Int {
        if (appDailyLimitsRaw.isBlank()) return 0
        return appDailyLimitsRaw.split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2 && parts[0] == appId) parts[1].toIntOrNull() else null
            }
            .firstOrNull() ?: 0
    }

    fun isWithinSleepWindow(): Boolean {
        val cal = Calendar.getInstance()
        // Check day of week (0 = Sun .. 6 = Sat)
        val dayIndex = (cal.get(Calendar.DAY_OF_WEEK) - 1).toString()
        if (!sleepRepeatEveryDay && !sleepScheduleDays.contains(dayIndex)) {
            return false
        }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val rawBedMinutes = bedtimeHour * 60 + bedtimeMinute
        val rawWakeMinutes = wakeHour * 60 + wakeMinute

        val preBuffer = if (beforeBedWindDownEnabled) beforeBedWindDownMinutes else 0
        val postBuffer = if (afterWakeWindDownEnabled) afterWakeWindDownMinutes else 0

        val startMinutes = ((rawBedMinutes - preBuffer) % 1440 + 1440) % 1440
        val endMinutes = (rawWakeMinutes + postBuffer) % 1440

        return if (startMinutes > endMinutes) {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            currentMinutes in startMinutes until endMinutes
        }
    }
}

class PreferencesManager(private val context: Context) {

    companion object {
        val SLEEP_MODE_ENABLED = booleanPreferencesKey("sleep_mode_enabled")
        val BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
        val WAKE_HOUR = intPreferencesKey("wake_hour")
        val WAKE_MINUTE = intPreferencesKey("wake_minute")
        val WIND_DOWN_MINUTES = intPreferencesKey("wind_down_minutes")
        val BEFORE_BED_WINDDOWN_ENABLED = booleanPreferencesKey("before_bed_winddown_enabled")
        val BEFORE_BED_WINDDOWN_MINUTES = intPreferencesKey("before_bed_winddown_minutes")
        val AFTER_WAKE_WINDDOWN_ENABLED = booleanPreferencesKey("after_wake_winddown_enabled")
        val AFTER_WAKE_WINDDOWN_MINUTES = intPreferencesKey("after_wake_winddown_minutes")
        val SLEEP_REPEAT_EVERY_DAY = booleanPreferencesKey("sleep_repeat_every_day")
        val SLEEP_SCHEDULE_DAYS = stringSetPreferencesKey("sleep_schedule_days")
        val SLEEP_ALLOWED_APPS = stringSetPreferencesKey("sleep_allowed_apps")

        val FULL_BLOCK_ACTIVE = booleanPreferencesKey("full_block_active")
        val FULL_BLOCK_END_TIME = longPreferencesKey("full_block_end_time")
        val UNTIL_STOPPED_MODE = booleanPreferencesKey("until_stopped_mode")
        val SELECTED_BLOCK_DURATION = intPreferencesKey("selected_block_duration")
        val BLOCKED_SOCIAL_APPS = stringSetPreferencesKey("blocked_social_apps")
        val BLOCKED_OTHER_APPS = stringSetPreferencesKey("blocked_other_apps")

        val HARD_MODE_ENABLED = booleanPreferencesKey("hard_mode_enabled")
        val HARD_MODE_PREVENT_UNINSTALL = booleanPreferencesKey("hard_mode_prevent_uninstall")
        val HARD_MODE_ACTIVATED_TS = longPreferencesKey("hard_mode_activated_ts")
        val UNINSTALL_PROTECTION_ENABLED = booleanPreferencesKey("uninstall_protection_enabled")

        val NAVIGATION_STYLE = stringPreferencesKey("navigation_style")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val GLOBAL_GRAYSCALE = booleanPreferencesKey("global_grayscale")
        val ICON_THEME = stringPreferencesKey("icon_theme")
        val INTERCEPT_NOTIFICATIONS = booleanPreferencesKey("intercept_notifications")
        val AUTO_LAUNCH_ENABLED = booleanPreferencesKey("auto_launch_enabled")
        val AUTO_LAUNCH_PLATFORM = stringPreferencesKey("auto_launch_platform")

        val SELECTED_WEBAPP_PLATFORM = stringPreferencesKey("selected_webapp_platform")

        val IG_BLOCK_REELS = booleanPreferencesKey("ig_block_reels")
        val IG_BLOCK_EXPLORE = booleanPreferencesKey("ig_block_explore")
        val IG_BLOCK_STORIES = booleanPreferencesKey("ig_block_stories")
        val IG_BLOCK_SAVED_POSTS = booleanPreferencesKey("ig_block_saved_posts")
        val IG_BLOCK_FEED = booleanPreferencesKey("ig_block_feed")
        val IG_DMS_ONLY = booleanPreferencesKey("ig_dms_only")
        val IG_GRAYSCALE = booleanPreferencesKey("ig_grayscale")
        val IG_BLOCK_ADS = booleanPreferencesKey("ig_block_ads")
        val IG_BLOCK_SUGGESTED_POSTS = booleanPreferencesKey("ig_block_suggested_posts")

        val YT_BLOCK_SHORTS = booleanPreferencesKey("yt_block_shorts")
        val YT_ALLOW_CHANNEL_SHORTS = booleanPreferencesKey("yt_allow_channel_shorts")
        val YT_STOP_AUTOPLAY = booleanPreferencesKey("yt_stop_autoplay")
        val YT_SEARCH_ONLY = booleanPreferencesKey("yt_search_only")
        val YT_HIDE_RECOMMENDATIONS = booleanPreferencesKey("yt_hide_recommendations")
        val YT_HIDE_COMMENTS = booleanPreferencesKey("yt_hide_comments")
        val YT_BLOCK_ADS = booleanPreferencesKey("yt_block_ads")
        val YT_CAPTIONS_FULLSCREEN = booleanPreferencesKey("yt_captions_fullscreen")
        val YT_KEEP_AUDIO_PLAYING = booleanPreferencesKey("yt_keep_audio_playing")
        val YT_GRAYSCALE = booleanPreferencesKey("yt_grayscale")

        val NATIVE_REELS_BLOCKER_ENABLED = booleanPreferencesKey("native_reels_blocker_enabled")
        val APP_DAILY_LIMITS_RAW = stringPreferencesKey("app_daily_limits_raw")
        val HIDDEN_HOME_APPS = stringSetPreferencesKey("hidden_home_apps")

        val WEB_BLOCK_SOCIAL = booleanPreferencesKey("web_block_social")
        val WEB_BLOCK_ADULT = booleanPreferencesKey("web_block_adult")
        val WEB_BLOCK_CASINOS = booleanPreferencesKey("web_block_casinos")
        val WEB_BLOCK_GAMING = booleanPreferencesKey("web_block_gaming")
        val WEB_BLOCK_SPORTS = booleanPreferencesKey("web_block_sports")
        val CUSTOM_BLOCKED_DOMAINS = stringSetPreferencesKey("custom_blocked_domains")
        val WEB_WHITELISTED_DOMAINS = stringSetPreferencesKey("web_whitelisted_domains")

        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val CURRENT_STREAK_DAYS = intPreferencesKey("current_streak_days")
        val BLOCKS_LANDED_TODAY = intPreferencesKey("blocks_landed_today")
        val IN_SOCIALLITE_MINUTES_TODAY = intPreferencesKey("in_sociallite_minutes_today")
        val SESSIONS_TODAY = intPreferencesKey("sessions_today")
        val LIMIT_HITS_TODAY = intPreferencesKey("limit_hits_today")
        val PUSHED_THROUGH_TODAY = intPreferencesKey("pushed_through_today")
    }

    val preferencesFlow: Flow<SocialBreakPreferences> = context.dataStore.data.map { prefs ->
        SocialBreakPreferences(
            sleepModeEnabled = prefs[SLEEP_MODE_ENABLED] ?: false,
            bedtimeHour = prefs[BEDTIME_HOUR] ?: 23,
            bedtimeMinute = prefs[BEDTIME_MINUTE] ?: 0,
            wakeHour = prefs[WAKE_HOUR] ?: 7,
            wakeMinute = prefs[WAKE_MINUTE] ?: 0,
            windDownMinutes = prefs[WIND_DOWN_MINUTES] ?: 30,
            beforeBedWindDownEnabled = prefs[BEFORE_BED_WINDDOWN_ENABLED] ?: false,
            beforeBedWindDownMinutes = prefs[BEFORE_BED_WINDDOWN_MINUTES] ?: 30,
            afterWakeWindDownEnabled = prefs[AFTER_WAKE_WINDDOWN_ENABLED] ?: false,
            afterWakeWindDownMinutes = prefs[AFTER_WAKE_WINDDOWN_MINUTES] ?: 30,
            sleepRepeatEveryDay = prefs[SLEEP_REPEAT_EVERY_DAY] ?: true,
            sleepScheduleDays = prefs[SLEEP_SCHEDULE_DAYS] ?: setOf("0", "1", "2", "3", "4", "5", "6"),
            sleepAllowedApps = prefs[SLEEP_ALLOWED_APPS] ?: emptySet(),

            fullBlockActive = prefs[FULL_BLOCK_ACTIVE] ?: false,
            fullBlockEndTimeMillis = prefs[FULL_BLOCK_END_TIME] ?: 0L,
            untilStoppedMode = prefs[UNTIL_STOPPED_MODE] ?: false,
            selectedBlockDurationMinutes = prefs[SELECTED_BLOCK_DURATION] ?: 30,
            blockedSocialApps = prefs[BLOCKED_SOCIAL_APPS] ?: DomainFilterRepository.SOCIAL_APPS.map { it.id }.toSet(),
            blockedOtherApps = prefs[BLOCKED_OTHER_APPS] ?: emptySet(),

            hardModeEnabled = prefs[HARD_MODE_ENABLED] ?: false,
            hardModePreventUninstall = prefs[HARD_MODE_PREVENT_UNINSTALL] ?: true,
            hardModeActivatedTimestamp = prefs[HARD_MODE_ACTIVATED_TS] ?: 0L,
            uninstallProtectionEnabled = prefs[UNINSTALL_PROTECTION_ENABLED] ?: false,

            navigationStyle = prefs[NAVIGATION_STYLE] ?: "bar",
            themeMode = prefs[THEME_MODE] ?: "dark",
            globalGrayscale = prefs[GLOBAL_GRAYSCALE] ?: false,
            iconTheme = prefs[ICON_THEME] ?: "painted",
            interceptNotifications = prefs[INTERCEPT_NOTIFICATIONS] ?: true,
            autoLaunchEnabled = prefs[AUTO_LAUNCH_ENABLED] ?: false,
            autoLaunchPlatform = prefs[AUTO_LAUNCH_PLATFORM] ?: "instagram",

            selectedWebAppPlatform = prefs[SELECTED_WEBAPP_PLATFORM] ?: "instagram",

            igBlockReels = prefs[IG_BLOCK_REELS] ?: true,
            igBlockExplore = prefs[IG_BLOCK_EXPLORE] ?: true,
            igBlockStories = prefs[IG_BLOCK_STORIES] ?: false,
            igBlockSavedPosts = prefs[IG_BLOCK_SAVED_POSTS] ?: false,
            igBlockFeed = prefs[IG_BLOCK_FEED] ?: false,
            igDmsOnly = prefs[IG_DMS_ONLY] ?: false,
            igGrayscale = prefs[IG_GRAYSCALE] ?: false,
            igBlockAds = prefs[IG_BLOCK_ADS] ?: true,
            igBlockSuggestedPosts = prefs[IG_BLOCK_SUGGESTED_POSTS] ?: true,

            ytBlockShorts = prefs[YT_BLOCK_SHORTS] ?: true,
            ytAllowChannelShorts = prefs[YT_ALLOW_CHANNEL_SHORTS] ?: false,
            ytStopAutoplay = prefs[YT_STOP_AUTOPLAY] ?: false,
            ytSearchOnlyMode = prefs[YT_SEARCH_ONLY] ?: false,
            ytHideRecommendations = prefs[YT_HIDE_RECOMMENDATIONS] ?: false,
            ytHideComments = prefs[YT_HIDE_COMMENTS] ?: false,
            ytBlockAds = prefs[YT_BLOCK_ADS] ?: false,
            ytCaptionsFullscreen = prefs[YT_CAPTIONS_FULLSCREEN] ?: false,
            ytKeepAudioPlaying = prefs[YT_KEEP_AUDIO_PLAYING] ?: true,
            ytGrayscale = prefs[YT_GRAYSCALE] ?: false,

            nativeReelsBlockerEnabled = prefs[NATIVE_REELS_BLOCKER_ENABLED] ?: true,
            appDailyLimitsRaw = prefs[APP_DAILY_LIMITS_RAW] ?: "",
            hiddenHomeApps = prefs[HIDDEN_HOME_APPS] ?: emptySet(),

            webBlockSocialApps = prefs[WEB_BLOCK_SOCIAL] ?: false,
            webBlockAdultContent = prefs[WEB_BLOCK_ADULT] ?: false,
            webBlockOnlineCasinos = prefs[WEB_BLOCK_CASINOS] ?: false,
            webBlockCasualGaming = prefs[WEB_BLOCK_GAMING] ?: false,
            webBlockSportsBetting = prefs[WEB_BLOCK_SPORTS] ?: false,
            customBlockedDomains = prefs[CUSTOM_BLOCKED_DOMAINS] ?: emptySet(),
            webWhitelistedDomains = prefs[WEB_WHITELISTED_DOMAINS] ?: emptySet(),

            accountEmail = prefs[ACCOUNT_EMAIL] ?: "akinphil2005@gmail.com",
            dailyGoalMinutes = prefs[DAILY_GOAL_MINUTES] ?: 30,
            currentStreakDays = prefs[CURRENT_STREAK_DAYS] ?: 0,
            blocksLandedToday = prefs[BLOCKS_LANDED_TODAY] ?: 0,
            inSocialLiteMinutesToday = prefs[IN_SOCIALLITE_MINUTES_TODAY] ?: 0,
            sessionsToday = prefs[SESSIONS_TODAY] ?: 0,
            limitHitsToday = prefs[LIMIT_HITS_TODAY] ?: 0,
            pushedThroughToday = prefs[PUSHED_THROUGH_TODAY] ?: 0
        )
    }

    suspend fun setSleepMode(enabled: Boolean) {
        context.dataStore.edit { it[SLEEP_MODE_ENABLED] = enabled }
    }

    suspend fun updateSleepSchedule(bedHour: Int, bedMin: Int, wakeHour: Int, wakeMin: Int) {
        context.dataStore.edit {
            it[BEDTIME_HOUR] = bedHour
            it[BEDTIME_MINUTE] = bedMin
            it[WAKE_HOUR] = wakeHour
            it[WAKE_MINUTE] = wakeMin
        }
    }

    suspend fun setWindDownMinutes(minutes: Int) {
        context.dataStore.edit {
            it[WIND_DOWN_MINUTES] = minutes
            it[BEFORE_BED_WINDDOWN_MINUTES] = minutes
        }
    }

    suspend fun setBeforeBedWindDown(enabled: Boolean, minutes: Int) {
        context.dataStore.edit {
            it[BEFORE_BED_WINDDOWN_ENABLED] = enabled
            it[BEFORE_BED_WINDDOWN_MINUTES] = minutes
            it[WIND_DOWN_MINUTES] = if (enabled) minutes else 0
        }
    }

    suspend fun setAfterWakeWindDown(enabled: Boolean, minutes: Int) {
        context.dataStore.edit {
            it[AFTER_WAKE_WINDDOWN_ENABLED] = enabled
            it[AFTER_WAKE_WINDDOWN_MINUTES] = minutes
        }
    }

    suspend fun setSleepRepeatEveryDay(repeatEveryDay: Boolean, days: Set<String>) {
        context.dataStore.edit {
            it[SLEEP_REPEAT_EVERY_DAY] = repeatEveryDay
            it[SLEEP_SCHEDULE_DAYS] = days
        }
    }

    suspend fun setSleepAllowedApps(apps: Set<String>) {
        context.dataStore.edit { it[SLEEP_ALLOWED_APPS] = apps }
    }

    suspend fun startTimedBlock(durationMinutes: Int, untilStopped: Boolean = false) {
        val endTime = if (untilStopped) Long.MAX_VALUE else System.currentTimeMillis() + (durationMinutes * 60_000L)
        context.dataStore.edit {
            it[FULL_BLOCK_ACTIVE] = true
            it[UNTIL_STOPPED_MODE] = untilStopped
            it[FULL_BLOCK_END_TIME] = endTime
            it[SELECTED_BLOCK_DURATION] = durationMinutes
            val currentSessions = it[SESSIONS_TODAY] ?: 0
            it[SESSIONS_TODAY] = currentSessions + 1
        }
    }

    suspend fun setUntilStoppedMode(enabled: Boolean) {
        context.dataStore.edit { it[UNTIL_STOPPED_MODE] = enabled }
    }

    suspend fun setSelectedBlockDuration(minutes: Int) {
        context.dataStore.edit {
            it[SELECTED_BLOCK_DURATION] = minutes.coerceIn(5, 720)
            it[UNTIL_STOPPED_MODE] = false
        }
    }

    suspend fun stopBlock() {
        context.dataStore.edit {
            it[FULL_BLOCK_ACTIVE] = false
            it[UNTIL_STOPPED_MODE] = false
            it[FULL_BLOCK_END_TIME] = 0L
        }
    }

    suspend fun setBlockedSocialApps(apps: Set<String>) {
        context.dataStore.edit { it[BLOCKED_SOCIAL_APPS] = apps }
    }

    suspend fun setBlockedOtherApps(apps: Set<String>) {
        context.dataStore.edit { it[BLOCKED_OTHER_APPS] = apps }
    }

    suspend fun setHardMode(enabled: Boolean, preventUninstall: Boolean = true) {
        context.dataStore.edit {
            it[HARD_MODE_ENABLED] = enabled
            it[HARD_MODE_PREVENT_UNINSTALL] = preventUninstall
            it[HARD_MODE_ACTIVATED_TS] = if (enabled) System.currentTimeMillis() else 0L
        }
    }

    suspend fun setUninstallProtection(enabled: Boolean) {
        context.dataStore.edit { it[UNINSTALL_PROTECTION_ENABLED] = enabled }
    }

    suspend fun setNavigationStyle(style: String) {
        context.dataStore.edit { it[NAVIGATION_STYLE] = style }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setGlobalGrayscale(enabled: Boolean) {
        context.dataStore.edit { it[GLOBAL_GRAYSCALE] = enabled }
    }

    suspend fun setIconTheme(theme: String) {
        context.dataStore.edit { it[ICON_THEME] = theme }
    }

    suspend fun setSelectedWebAppPlatform(platform: String) {
        context.dataStore.edit { it[SELECTED_WEBAPP_PLATFORM] = platform }
    }

    suspend fun setAppDailyLimitMinutes(appId: String, minutes: Int) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[APP_DAILY_LIMITS_RAW] ?: ""
            val map = currentRaw.split(",")
                .mapNotNull {
                    val p = it.split(":")
                    if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 0) else null
                }
                .toMap()
                .toMutableMap()
            if (minutes <= 0) {
                map.remove(appId)
            } else {
                map[appId] = minutes
            }
            prefs[APP_DAILY_LIMITS_RAW] = map.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun setHiddenHomeApps(hidden: Set<String>) {
        context.dataStore.edit { it[HIDDEN_HOME_APPS] = hidden }
    }

    suspend fun setBooleanPreference(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setAutoLaunch(enabled: Boolean, platform: String) {
        context.dataStore.edit {
            it[AUTO_LAUNCH_ENABLED] = enabled
            it[AUTO_LAUNCH_PLATFORM] = platform
        }
    }

    suspend fun setAccountEmail(email: String) {
        context.dataStore.edit { it[ACCOUNT_EMAIL] = email }
    }

    suspend fun addCustomBlockedDomain(domain: String) {
        val clean = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
        if (clean.isNotEmpty() && clean.contains(".")) {
            context.dataStore.edit {
                val current = it[CUSTOM_BLOCKED_DOMAINS] ?: emptySet()
                it[CUSTOM_BLOCKED_DOMAINS] = current + clean
            }
        }
    }

    suspend fun removeCustomBlockedDomain(domain: String) {
        context.dataStore.edit {
            val current = it[CUSTOM_BLOCKED_DOMAINS] ?: emptySet()
            it[CUSTOM_BLOCKED_DOMAINS] = current - domain
        }
    }

    suspend fun toggleWhitelistedDomain(domain: String) {
        context.dataStore.edit {
            val current = it[WEB_WHITELISTED_DOMAINS] ?: emptySet()
            it[WEB_WHITELISTED_DOMAINS] = if (current.contains(domain)) current - domain else current + domain
        }
    }

    suspend fun incrementBlocksLanded() {
        context.dataStore.edit {
            val current = it[BLOCKS_LANDED_TODAY] ?: 0
            it[BLOCKS_LANDED_TODAY] = current + 1
        }
    }

    suspend fun incrementInSocialLiteMinutes(delta: Int = 1) {
        context.dataStore.edit {
            val current = it[IN_SOCIALLITE_MINUTES_TODAY] ?: 0
            it[IN_SOCIALLITE_MINUTES_TODAY] = current + delta
        }
    }

    suspend fun incrementPushedThrough() {
        context.dataStore.edit {
            val current = it[PUSHED_THROUGH_TODAY] ?: 0
            it[PUSHED_THROUGH_TODAY] = current + 1
        }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { it[DAILY_GOAL_MINUTES] = minutes.coerceIn(5, 240) }
    }

    suspend fun setServiceHealthWarning(needsAttention: Boolean) {
        context.dataStore.edit { it[NATIVE_REELS_BLOCKER_ENABLED] = !needsAttention }
    }

    suspend fun resetGoals() {
        context.dataStore.edit {
            it[DAILY_GOAL_MINUTES] = 30
            it[CURRENT_STREAK_DAYS] = 0
            it[BLOCKS_LANDED_TODAY] = 0
            it[IN_SOCIALLITE_MINUTES_TODAY] = 0
            it[SESSIONS_TODAY] = 0
            it[LIMIT_HITS_TODAY] = 0
            it[PUSHED_THROUGH_TODAY] = 0
            it[APP_DAILY_LIMITS_RAW] = ""
        }
    }

    suspend fun clearAllLocalData() {
        context.dataStore.edit { it.clear() }
    }
}
