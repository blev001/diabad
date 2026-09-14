package com.diabad.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

class GlucoseLockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        push(context, GlucoseLockSnapshotStore.load(context), appWidgetIds)
    }

    companion object {
        fun push(
            context: Context,
            snapshot: GlucoseWidgetSnapshot,
            appWidgetIds: IntArray? = null,
        ) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = appWidgetIds ?: manager.getAppWidgetIds(
                ComponentName(app, GlucoseLockWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            val views = GlucoseLockRemoteViews.lockScreen(app, snapshot)
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
