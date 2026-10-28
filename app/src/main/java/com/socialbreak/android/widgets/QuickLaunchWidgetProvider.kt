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

class QuickLaunchWidgetProvider : AppWidgetProvider() {

    data class Slot(val viewId: Int, val platformKey: String, val label: String)

    private val slots = listOf(
        Slot(R.id.slot_1, "instagram", "IG"),
        Slot(R.id.slot_2, "youtube", "YT"),
        Slot(R.id.slot_3, "tiktok", "TT"),
        Slot(R.id.slot_4, "snapchat", "SC")
    )

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_launch)

            slots.forEachIndexed { index, slot ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("socialbreak://open?platform=${slot.platformKey}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10 + index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                views.setOnClickPendingIntent(slot.viewId, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
