package com.diabad.widget

import android.content.Context
import com.diabad.R
import com.diabad.domain.model.GlucoseZone

object GlucoseLockSnapshotStore {
    private const val PREFS = "glucose_lock_snapshot"
    private const val KEY_LINE = "line"
    private const val KEY_ZONE = "zone"
    private const val KEY_WAITING = "waiting"

    fun save(context: Context, snapshot: GlucoseWidgetSnapshot) {
        prefs(context).edit()
            .putString(KEY_LINE, snapshot.line)
            .putString(KEY_ZONE, snapshot.zone.name)
            .putBoolean(KEY_WAITING, snapshot.waiting)
            .apply()
    }

    fun load(context: Context): GlucoseWidgetSnapshot {
        val stored = prefs(context)
        val line = stored.getString(KEY_LINE, null)
        if (line.isNullOrBlank()) {
            return waiting(context)
        }
        val zone = runCatching {
            GlucoseZone.valueOf(stored.getString(KEY_ZONE, GlucoseZone.UNKNOWN.name)!!)
        }.getOrDefault(GlucoseZone.UNKNOWN)
        return GlucoseWidgetSnapshot(
            line = line,
            zone = zone,
            waiting = stored.getBoolean(KEY_WAITING, true),
        )
    }

    fun waiting(context: Context): GlucoseWidgetSnapshot {
        return GlucoseWidgetSnapshot(
            line = context.getString(R.string.widget_waiting),
            zone = GlucoseZone.UNKNOWN,
            waiting = true,
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
