package com.diabad.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object GlucoseWidgets {

    fun refresh(context: Context, scope: CoroutineScope) {
        scope.launch {
            val app = context.applicationContext
            val snapshot = GlucoseWidgetSnapshot.load(app)
            GlucoseLockSnapshotStore.save(app, snapshot)
            GlucoseGlanceWidget().updateAll(app)
            GlucoseLockWidgetProvider.push(app, snapshot)
            FaceWidgetPublisher.publish(app, snapshot)
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
}
