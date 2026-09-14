package com.diabad.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.diabad.MainActivity
import com.diabad.R

object GlucoseLockRemoteViews {

    fun lockScreen(context: Context, snapshot: GlucoseWidgetSnapshot): RemoteViews {
        return bind(context, R.layout.glucose_lock_widget, snapshot)
    }

    fun aod(context: Context, snapshot: GlucoseWidgetSnapshot): RemoteViews {
        return bind(context, R.layout.glucose_lock_widget_aod, snapshot)
    }

    private fun bind(
        context: Context,
        layoutId: Int,
        snapshot: GlucoseWidgetSnapshot,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutId)
        val color = GlucoseLockPalette.contentColor(snapshot.zone, snapshot.waiting)
        views.setTextViewText(R.id.glucose_lock_line, snapshot.line)
        views.setTextColor(R.id.glucose_lock_line, color)
        views.setInt(R.id.glucose_lock_icon, "setColorFilter", color)
        views.setOnClickPendingIntent(R.id.glucose_lock_root, clickIntent(context))
        return views
    }

    private fun clickIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
