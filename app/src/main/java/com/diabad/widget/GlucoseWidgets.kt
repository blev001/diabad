package com.diabad.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.glance.appwidget.updateAll
import com.diabad.R
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

    /** Samsung lock-screen editor so DiaBAD can sit under the clock. */
    fun openLockScreenEditor(context: Context) {
        val candidates = listOf(
            Intent("com.samsung.settings.LOCKSCREEN_SETTINGS"),
            Intent("com.samsung.settings.LOCK_SCREEN_SETTINGS"),
            Intent("com.samsung.settings.FaceWidgetSettings"),
            Intent(Settings.ACTION_DISPLAY_SETTINGS),
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_lockscreen_toast),
                        Toast.LENGTH_LONG,
                    ).show()
                    return
                } catch (_: Exception) {
                    continue
                }
            }
        }
        Toast.makeText(
            context,
            context.getString(R.string.settings_lockscreen_toast),
            Toast.LENGTH_LONG,
        ).show()
    }
}
