package com.diabad.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun canInstallPackages(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(): Intent =
        Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun kingInstallerInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(KING_PACKAGE, 0)
        true
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
        false
    }

    fun openKingInstallerDownload() {
        context.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/fcaronte/KingInstaller/releases/latest"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    suspend fun sendInstalledApkToKingInstaller() {
        val copy = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            File(dir, "DiaBAD-KingInstaller.apk").also {
                File(context.applicationInfo.sourceDir).copyTo(it, overwrite = true)
            }
        }
        install(copy, useKingInstaller = true)
    }

    fun install(apkFile: File, useKingInstaller: Boolean = false) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (useKingInstaller) {
            check(kingInstallerInstalled()) { "Сначала установите KingInstaller из раздела Android Auto" }
            intent.setClassName(KING_PACKAGE, "$KING_PACKAGE.ApkHandler")
            intent.clipData = android.content.ClipData.newRawUri("DiaBAD APK", uri)
        }
        context.startActivity(intent)
    }

    companion object {
        private const val KING_PACKAGE = "com.example.kinginstaller"
    }
}
