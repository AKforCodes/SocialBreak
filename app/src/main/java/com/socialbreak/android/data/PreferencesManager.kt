package com.socialbreak.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "socialbreak_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_SLEEP_MODE_ENABLED = booleanPreferencesKey("sleep_mode_enabled")
        val KEY_BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val KEY_BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
        val KEY_WAKE_HOUR = intPreferencesKey("wake_hour")
        val KEY_WAKE_MINUTE = intPreferencesKey("wake_minute")
        val KEY_WIND_DOWN_MINUTES = intPreferencesKey("wind_down_minutes")

        val KEY_HARD_MODE_ENABLED = booleanPreferencesKey("hard_mode_enabled")
        val KEY_FOCUS_BLOCK_UNTIL = longPreferencesKey("focus_block_until")
        val KEY_BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")

        val KEY_WEB_BLOCKING_ADULT = booleanPreferencesKey("web_blocking_adult")
        val KEY_WEB_BLOCKING_CASINOS = booleanPreferencesKey("web_blocking_casinos")
        val KEY_WEB_BLOCKING_GAMING = booleanPreferencesKey("web_blocking_gaming")
        val KEY_WEB_BLOCKING_SOCIAL = booleanPreferencesKey("web_blocking_social")

        val KEY_TODAY_BLOCKS_LANDED = intPreferencesKey("today_blocks_landed")
        val KEY_TODAY_STRIPPED_COUNT = intPreferencesKey("today_stripped_count")

        // Instagram granular switches
        val KEY_IG_BLOCK_STORIES = booleanPreferencesKey("ig_block_stories")
        val KEY_IG_BLOCK_SAVED = booleanPreferencesKey("ig_block_saved")
        val KEY_IG_BLOCK_FEED = booleanPreferencesKey("ig_block_feed")
        val KEY_IG_DMS_ONLY = booleanPreferencesKey("ig_dms_only")
        val KEY_IG_GRAYSCALE = booleanPreferencesKey("ig_grayscale")
        val KEY_IG_BLOCK_ADS = booleanPreferencesKey("ig_block_ads")
        val KEY_IG_BLOCK_SUGGESTED = booleanPreferencesKey("ig_block_suggested")

        // YouTube granular switches
        val KEY_YT_BLOCK_SHORTS = booleanPreferencesKey("yt_block_shorts")
        val KEY_YT_ALLOW_CHANNEL_SHORTS = booleanPreferencesKey("yt_allow_channel_shorts")
        val KEY_YT_STOP_AUTOPLAY = booleanPreferencesKey("yt_stop_autoplay")
        val KEY_YT_SEARCH_ONLY = booleanPreferencesKey("yt_search_only")
        val KEY_YT_HIDE_RECOMMENDATIONS = booleanPreferencesKey("yt_hide_recommendations")
        val KEY_YT_HIDE_COMMENTS = booleanPreferencesKey("yt_hide_comments")
        val KEY_YT_BLOCK_ADS = booleanPreferencesKey("yt_block_ads")
        val KEY_YT_CAPTIONS_FULLSCREEN = booleanPreferencesKey("yt_captions_fullscreen")
        val KEY_YT_KEEP_AUDIO_PLAYING = booleanPreferencesKey("yt_keep_audio_playing")
        val KEY_YT_GRAYSCALE = booleanPreferencesKey("yt_grayscale")

        // Appearance
        val KEY_NAV_STYLE = stringPreferencesKey("nav_style")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ICON_THEME = stringPreferencesKey("icon_theme")
        val KEY_GLOBAL_GRAYSCALE = booleanPreferencesKey("global_grayscale")
    }

    // Instagram flows
    val igBlockStories: Flow<Boolean> = context.dataStore.data.map { it[KEY_IG_BLOCK_STORIES] ?: false }
    val igBlockSaved: Flow<Boolean> = context.dataStore.data.map { it[KEY_IG_BLOCK_SAVED] ?: false }
    val igBlockFeed: Flow<Boolean> = context.dataStore.data.map { it[KEY_IG_BLOCK_FEED] ?: false }
    val igDmsOnly: Flow<Boolean> = context.dataStore.data.map { it[KEY_IG_DMS_ONLY] ?: false }
    val igGrayscale: Flow<Boolean> = context.dataStore.data.map { it[KEY_IG_GRAYSCALE] ?: false }
    val igBlockAds: Flow<Boolean> = context.dataStore.data.map { it[KEY_IG_BLOCK_ADS] ?: true }
    val igBlockSuggested: Flow<Boolean> = context.dataStore.data.map { it[KEY_IG_BLOCK_SUGGESTED] ?: true }

    // YouTube flows
    val ytBlockShorts: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_BLOCK_SHORTS] ?: true }
    val ytAllowChannelShorts: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_ALLOW_CHANNEL_SHORTS] ?: false }
    val ytStopAutoplay: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_STOP_AUTOPLAY] ?: true }
    val ytSearchOnly: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_SEARCH_ONLY] ?: false }
    val ytHideRecommendations: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_HIDE_RECOMMENDATIONS] ?: false }
    val ytHideComments: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_HIDE_COMMENTS] ?: false }
    val ytBlockAds: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_BLOCK_ADS] ?: true }
    val ytCaptionsFullscreen: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_CAPTIONS_FULLSCREEN] ?: true }
    val ytKeepAudioPlaying: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_KEEP_AUDIO_PLAYING] ?: true }
    val ytGrayscale: Flow<Boolean> = context.dataStore.data.map { it[KEY_YT_GRAYSCALE] ?: false }

    // Appearance flows
    val navStyle: Flow<String> = context.dataStore.data.map { it[KEY_NAV_STYLE] ?: "Bar" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: "Dark" }
    val iconTheme: Flow<String> = context.dataStore.data.map { it[KEY_ICON_THEME] ?: "Painted" }
    val globalGrayscale: Flow<Boolean> = context.dataStore.data.map { it[KEY_GLOBAL_GRAYSCALE] ?: false }

    suspend fun setPlatformToggle(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setAppearance(nav: String? = null, theme: String? = null, icon: String? = null, grayscale: Boolean? = null) {
        context.dataStore.edit {
            nav?.let { v -> it[KEY_NAV_STYLE] = v }
            theme?.let { v -> it[KEY_THEME_MODE] = v }
            icon?.let { v -> it[KEY_ICON_THEME] = v }
            grayscale?.let { v -> it[KEY_GLOBAL_GRAYSCALE] = v }
        }
    }

    val sleepModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SLEEP_MODE_ENABLED] ?: false }
    val bedtimeHour: Flow<Int> = context.dataStore.data.map { it[KEY_BEDTIME_HOUR] ?: 23 }
    val bedtimeMinute: Flow<Int> = context.dataStore.data.map { it[KEY_BEDTIME_MINUTE] ?: 0 }
    val wakeHour: Flow<Int> = context.dataStore.data.map { it[KEY_WAKE_HOUR] ?: 7 }
    val wakeMinute: Flow<Int> = context.dataStore.data.map { it[KEY_WAKE_MINUTE] ?: 0 }
    val windDownMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_WIND_DOWN_MINUTES] ?: 30 }

    val hardModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_HARD_MODE_ENABLED] ?: false }
    val focusBlockUntil: Flow<Long> = context.dataStore.data.map { it[KEY_FOCUS_BLOCK_UNTIL] ?: 0L }
    val blockedPackages: Flow<Set<String>> = context.dataStore.data.map {
        it[KEY_BLOCKED_PACKAGES] ?: setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.snapchat.android",
            "com.twitter.android",
            "com.facebook.katana"
        )
    }

    val webBlockingAdult: Flow<Boolean> = context.dataStore.data.map { it[KEY_WEB_BLOCKING_ADULT] ?: false }
    val webBlockingCasinos: Flow<Boolean> = context.dataStore.data.map { it[KEY_WEB_BLOCKING_CASINOS] ?: false }
    val webBlockingGaming: Flow<Boolean> = context.dataStore.data.map { it[KEY_WEB_BLOCKING_GAMING] ?: false }
    val webBlockingSocial: Flow<Boolean> = context.dataStore.data.map { it[KEY_WEB_BLOCKING_SOCIAL] ?: false }

    val todayBlocksLanded: Flow<Int> = context.dataStore.data.map { it[KEY_TODAY_BLOCKS_LANDED] ?: 0 }
    val todayStrippedCount: Flow<Int> = context.dataStore.data.map { it[KEY_TODAY_STRIPPED_COUNT] ?: 0 }

    suspend fun setSleepMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SLEEP_MODE_ENABLED] = enabled }
    }

    suspend fun setSleepSchedule(bHour: Int, bMin: Int, wHour: Int, wMin: Int) {
        context.dataStore.edit {
            it[KEY_BEDTIME_HOUR] = bHour
            it[KEY_BEDTIME_MINUTE] = bMin
            it[KEY_WAKE_HOUR] = wHour
            it[KEY_WAKE_MINUTE] = wMin
        }
    }

    suspend fun setHardMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HARD_MODE_ENABLED] = enabled }
    }

    suspend fun setFocusBlockUntil(timestamp: Long) {
        context.dataStore.edit { it[KEY_FOCUS_BLOCK_UNTIL] = timestamp }
    }

    suspend fun toggleBlockedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLOCKED_PACKAGES]?.toMutableSet() ?: mutableSetOf(
                "com.instagram.android",
                "com.google.android.youtube",
                "com.zhiliaoapp.musically",
                "com.snapchat.android",
                "com.twitter.android",
                "com.facebook.katana"
            )
            if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
            prefs[KEY_BLOCKED_PACKAGES] = current
        }
    }

    suspend fun setWebFilter(category: String, enabled: Boolean) {
        context.dataStore.edit {
            when (category) {
                "adult" -> it[KEY_WEB_BLOCKING_ADULT] = enabled
                "casinos" -> it[KEY_WEB_BLOCKING_CASINOS] = enabled
                "gaming" -> it[KEY_WEB_BLOCKING_GAMING] = enabled
                "social" -> it[KEY_WEB_BLOCKING_SOCIAL] = enabled
            }
        }
    }

    suspend fun incrementBlocksLanded() {
        context.dataStore.edit {
            val count = it[KEY_TODAY_BLOCKS_LANDED] ?: 0
            it[KEY_TODAY_BLOCKS_LANDED] = count + 1
        }
    }

    suspend fun incrementStrippedCount() {
        context.dataStore.edit {
            val count = it[KEY_TODAY_STRIPPED_COUNT] ?: 0
            it[KEY_TODAY_STRIPPED_COUNT] = count + 1
        }
    }
}
