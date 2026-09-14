package com.diabad.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.diabad.R

object LockStarLauncher {

    const val LOCKSTAR_PACKAGE = "com.samsung.systemui.lockstar"
    const val LOCKSTAR_PACKAGE_LEGACY = "com.samsung.android.app.lockstar"
    const val GOODLOCK_PACKAGE = "com.samsung.android.goodlock"

    val installedPackages = listOf(
        LOCKSTAR_PACKAGE,
        LOCKSTAR_PACKAGE_LEGACY,
        GOODLOCK_PACKAGE,
    )

    fun isInstalled(context: Context): Boolean {
        return installedPackages.any { isPackageInstalled(context.packageManager, it) }
    }

    fun open(context: Context) {
        val pm = context.packageManager
        for (packageName in installedPackages) {
            val launch = pm.getLaunchIntentForPackage(packageName) ?: continue
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launch)
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_lockscreen_toast_lockstar),
                    Toast.LENGTH_LONG,
                ).show()
                return
            } catch (_: Exception) {
                continue
            }
        }
        for (intent in storeIntents()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(pm) == null) continue
            try {
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_lockscreen_toast_store),
                    Toast.LENGTH_LONG,
                ).show()
                return
            } catch (_: Exception) {
                continue
            }
        }
        Toast.makeText(
            context,
            context.getString(R.string.settings_lockscreen_toast_store),
            Toast.LENGTH_LONG,
        ).show()
    }

    fun storeIntents(): List<Intent> {
        return storeUris().map { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
    }

    fun storeUris(): List<String> = listOf(
        "samsungapps://ProductDetail/$LOCKSTAR_PACKAGE",
        "https://galaxystore.samsung.com/detail/$LOCKSTAR_PACKAGE",
        "market://details?id=$LOCKSTAR_PACKAGE",
        "https://play.google.com/store/apps/details?id=$LOCKSTAR_PACKAGE",
    )

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
