package com.diabad.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object GlucoseWidgets {

    fun refresh(context: Context, scope: CoroutineScope) {
        scope.launch {
            GlucoseGlanceWidget().updateAll(context.applicationContext)
        }
    }

    fun canPin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return manager.isRequestPinAppWidgetSupported
    }

    fun requestPin(context: Context): Boolean {
        return try {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, GlucoseWidgetReceiver::class.java)
            manager.requestPinAppWidget(provider, null, null)
        } catch (_: Exception) {
            false
        }
    }

    fun liveUpdatesSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < 36) return null
        return Intent(ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    private const val ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS =
        "android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS"
}
