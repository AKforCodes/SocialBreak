package com.socialbreak.android.data

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import com.socialbreak.android.MainActivity

object AppShortcutManager {

    const val ACTION_START_FOCUS_25 = "com.socialbreak.android.action.START_FOCUS_25"
    const val ACTION_OPEN_ANALYTICS = "com.socialbreak.android.action.OPEN_ANALYTICS"

    fun initDynamicShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        val focusShortcut = ShortcutInfo.Builder(context, "shortcut_focus_25")
            .setShortLabel("25m Focus")
            .setLongLabel("Start 25m Focus Sprint")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_lock_idle_lock))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_START_FOCUS_25
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            .build()

        val instagramShortcut = ShortcutInfo.Builder(context, "shortcut_clean_ig")
            .setShortLabel("Clean Instagram")
            .setLongLabel("Open Clean Instagram DMs")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_dialog_email))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("socialbreak://open?platform=instagram")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            .build()

        val youtubeShortcut = ShortcutInfo.Builder(context, "shortcut_clean_yt")
            .setShortLabel("Clean YouTube")
            .setLongLabel("Open Clean YouTube Search")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_media_play))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("socialbreak://open?platform=youtube")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            .build()

        val statsShortcut = ShortcutInfo.Builder(context, "shortcut_stats")
            .setShortLabel("Today Stats")
            .setLongLabel("View Today's Shield Metrics")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_sort_by_size))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_OPEN_ANALYTICS
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            .build()

        try {
            shortcutManager.dynamicShortcuts = listOf(focusShortcut, instagramShortcut, youtubeShortcut, statsShortcut)
        } catch (e: Exception) {
            // Ignore if launcher limits shortcuts
        }
    }
}
