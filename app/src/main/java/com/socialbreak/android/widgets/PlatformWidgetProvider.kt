package com.socialbreak.android.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.socialbreak.android.MainActivity
import com.socialbreak.android.R

open class PlatformWidgetProvider(
    private val platformKey: String,
    private val platformLabel: String,
    private val iconResId: Int
) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_platform)
            views.setTextViewText(R.id.widget_label, platformLabel)
            views.setImageViewResource(R.id.widget_icon, iconResId)

            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("socialbreak://open?platform=$platformKey")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class InstagramWidgetProvider : PlatformWidgetProvider("instagram", "Instagram", R.drawable.ic_instagram_painted)
class YouTubeWidgetProvider : PlatformWidgetProvider("youtube", "YouTube", R.drawable.ic_youtube_painted)
class TikTokWidgetProvider : PlatformWidgetProvider("tiktok", "TikTok", R.drawable.ic_tiktok_painted)
class TwitterWidgetProvider : PlatformWidgetProvider("twitter", "X", R.drawable.ic_twitter_painted)
class SnapchatWidgetProvider : PlatformWidgetProvider("snapchat", "Snapchat", R.drawable.ic_snapchat_painted)
class FacebookWidgetProvider : PlatformWidgetProvider("facebook", "Facebook", R.drawable.ic_facebook_painted)
class LinkedInWidgetProvider : PlatformWidgetProvider("linkedin", "LinkedIn", R.drawable.ic_linkedin_painted)
