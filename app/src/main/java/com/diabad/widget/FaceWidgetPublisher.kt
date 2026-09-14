package com.diabad.widget

import android.content.Context
import android.content.Intent
import android.util.Log

object FaceWidgetPublisher {

    fun publish(
        context: Context,
        snapshot: GlucoseWidgetSnapshot,
        pageId: String = FaceWidgetCatalog.PAGE_ID,
    ) {
        val app = context.applicationContext
        val origin = GlucoseLockRemoteViews.lockScreen(app, snapshot)
        val aod = GlucoseLockRemoteViews.aod(app, snapshot)
        val intent = Intent(FaceWidgetCatalog.RESPONSE_ACTION).apply {
            setPackage(FaceWidgetCatalog.SYSTEM_UI_PACKAGE)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra("package", app.packageName)
            putExtra("pageId", pageId.ifBlank { FaceWidgetCatalog.PAGE_ID })
            putExtra("show", true)
            putExtra("origin", origin)
            putExtra("aod", aod)
        }
        try {
            app.sendBroadcast(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "Face widget broadcast failed", t)
        }
    }

    private const val TAG = "FaceWidgetPublisher"
}
