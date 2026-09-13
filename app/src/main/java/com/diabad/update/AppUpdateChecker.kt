package com.diabad.update

import android.content.Context
import android.util.Log
import com.diabad.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateCheckResult {
    data object UpToDate : UpdateCheckResult()
    data class Available(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val notes: String?,
    ) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

@Singleton
class AppUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val feed = httpGetText(RELEASES_ATOM_URL)
                ?: return@withContext UpdateCheckResult.Error("Не удалось прочитать список релизов")
            val entry = GithubReleaseAtom.newestRelease(feed)
                ?: return@withContext UpdateCheckResult.Error("В ленте релизов нет тега версии")
            val remoteCode = GithubReleaseAtom.parseVersionCode(entry.tag)
                ?: return@withContext UpdateCheckResult.Error("В релизе нет versionCode (тег вида v19)")
            if (remoteCode <= BuildConfig.VERSION_CODE) {
                return@withContext UpdateCheckResult.UpToDate
            }
            val apkUrl = resolveApkUrl(entry.tag, entry.title)
                ?: return@withContext UpdateCheckResult.Error("В релизе нет DiaBAD.apk")
            val name = entry.title.takeIf { it.isNotBlank() } ?: entry.tag
            UpdateCheckResult.Available(remoteCode, name, apkUrl, entry.notes)
        } catch (t: Throwable) {
            Log.w(TAG, "Update check failed", t)
            UpdateCheckResult.Error(t.message ?: t.javaClass.simpleName)
        }
    }

    suspend fun downloadApk(
        apkUrl: String,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(dir, "diabad-update.apk")
        if (outFile.exists()) outFile.delete()

        val connection = openGet(apkUrl)
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/octet-stream")
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                error("Скачивание не удалось ($code)")
            }
            val total = connection.contentLengthLong.coerceAtLeast(0L)
            connection.inputStream.buffered().use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(16_384)
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        readTotal += read
                        if (total > 0L) {
                            onProgress((readTotal.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
        } finally {
            connection.disconnect()
        }
        outFile
    }

    private fun resolveApkUrl(tag: String, versionName: String): String? {
        val candidates = listOf(
            "$DOWNLOAD_BASE/$tag/$APK_ASSET_NAME",
            "$DOWNLOAD_BASE/$tag/DiaBAD-$versionName.apk",
            "$LATEST_DOWNLOAD_BASE/$APK_ASSET_NAME",
            "$LATEST_DOWNLOAD_BASE/DiaBAD-$versionName.apk",
        )
        return candidates.firstOrNull { urlExists(it) }
    }

    private fun urlExists(url: String): Boolean {
        val connection = openGet(url)
        connection.instanceFollowRedirects = false
        connection.requestMethod = "HEAD"
        return try {
            connection.connect()
            connection.responseCode in 200..399
        } catch (_: Throwable) {
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun httpGetText(url: String): String? {
        val connection = openGet(url)
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/atom+xml, application/xml, text/xml, */*")
        try {
            connection.connect()
            val code = connection.responseCode
            if (code == 404) return null
            if (code !in 200..299) error("GitHub ответил $code")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openGet(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", USER_AGENT)
        return connection
    }

    companion object {
        const val APK_ASSET_NAME = "DiaBAD.apk"
        const val GITHUB_OWNER = "blev001"
        const val GITHUB_REPO = "diabad"
        private const val DOWNLOAD_BASE = "https://github.com/blev001/diabad/releases/download"
        private const val LATEST_DOWNLOAD_BASE = "https://github.com/blev001/diabad/releases/latest/download"
        private const val RELEASES_ATOM_URL = "https://github.com/blev001/diabad/releases.atom"
        private const val TAG = "AppUpdateChecker"
        private const val USER_AGENT = "DiaBAD-Updater/1.0 (+https://github.com/blev001/diabad)"
    }
}
