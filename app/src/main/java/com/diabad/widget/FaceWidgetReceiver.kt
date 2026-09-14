package com.diabad.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class FaceWidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != FaceWidgetCatalog.REQUEST_ACTION) return
        val pageId = intent.getStringExtra("pageId")
            ?.takeIf { it.isNotBlank() }
            ?: FaceWidgetCatalog.PAGE_ID
        Log.i(TAG, "SystemUI requested lock-screen RemoteViews pageId=$pageId")
        val snapshot = GlucoseLockSnapshotStore.load(context)
        FaceWidgetPublisher.publish(context, snapshot, pageId)
    }

    private companion object {
        const val TAG = "FaceWidgetReceiver"
    }
}
