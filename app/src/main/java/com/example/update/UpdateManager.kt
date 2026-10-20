package com.example.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Единая константа версии приложения.
 * Отображается внизу главного экрана («Версия 1.0.0») и на экране «О приложении».
 */
const val APP_VERSION = "1.0.0"

/**
 * Единая константа репозитория GitHub для автоматической проверки релизов.
 * Поменяйте на свой логин и репозиторий, например: "ivanov/autoservice-app"
 */
const val GITHUB_REPO = "ВАШ_ЛОГИН/ВАШ_РЕПОЗИТОРИЙ"

data class ReleaseInfo(
    val version: String,
    val tagName: String,
    val body: String,
    val apkDownloadUrl: String?,
    val apkName: String?,
    val apkSizeBytes: Long,
    val htmlUrl: String
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object UpdateManager {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Сравнивает две версии по числам (major.minor.patch), убирая префикс 'v' или 'V'.
     * Возвращает:
     *  > 0, если remoteVersion новее currentVersion
     *  = 0, если версии равны
     *  < 0, если remoteVersion старее currentVersion
     */
    fun compareVersionsNumeric(remoteTag: String, currentVersion: String = APP_VERSION): Int {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V").substringBefore("-")

        val remoteParts = cleanRemote.split(".").map { it.trim().toIntOrNull() ?: 0 }
        val currentParts = cleanCurrent.split(".").map { it.trim().toIntOrNull() ?: 0 }

        val maxLength = max(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) {
                return r.compareTo(c)
            }
        }
        return 0
    }

    /**
     * Проверяет последний релиз через GitHub API:
     * https://api.github.com/repos/{GITHUB_REPO}/releases/latest
     */
    suspend fun checkForUpdates(customRepoOverride: String? = null): UpdateCheckResult = withContext(Dispatchers.IO) {
        val repo = customRepoOverride?.trim()?.takeIf { it.isNotEmpty() } ?: GITHUB_REPO
        if (repo == "ВАШ_ЛОГИН/ВАШ_РЕПОЗИТОРИЙ" || !repo.contains("/") || repo.startsWith("ВАШ_")) {
            return@withContext UpdateCheckResult.Error(
                "Репозиторий GitHub ещё не настроен ($repo). Укажите свой логин/репозиторий в константе GITHUB_REPO."
            )
        }

        val url = "https://api.github.com/repos/$repo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "AutoService-Android-App/$APP_VERSION")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext UpdateCheckResult.Error(
                        "Репозиторий «$repo» не найден или в нём ещё нет опубликованных релизов (Releases)."
                    )
                }
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error(
                        "Сервер GitHub вернул код ${response.code}. Приложение продолжает работать офлайн."
                    )
                }

                val bodyStr = response.body?.string()
                    ?: return@withContext UpdateCheckResult.Error("Пустой ответ от GitHub API.")

                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "").trim()
                if (tagName.isEmpty()) {
                    return@withContext UpdateCheckResult.Error("В последнем релизе отсутствует tag_name.")
                }

                val cleanVersion = tagName.removePrefix("v").removePrefix("V")
                val releaseNotes = json.optString("body", "").trim().ifEmpty {
                    "Исправление ошибок и улучшение стабильности."
                }
                val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

                var apkUrl: String? = null
                var apkName: String? = null
                var apkSize = 0L

                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotEmpty()) {
                            apkUrl = downloadUrl
                            apkName = name
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                val cmp = compareVersionsNumeric(tagName, APP_VERSION)
                if (cmp > 0) {
                    UpdateCheckResult.UpdateAvailable(
                        ReleaseInfo(
                            version = cleanVersion,
                            tagName = tagName,
                            body = releaseNotes,
                            apkDownloadUrl = apkUrl,
                            apkName = apkName,
                            apkSizeBytes = apkSize,
                            htmlUrl = htmlUrl
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate(APP_VERSION)
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                "Нет подключения к интернету или GitHub недоступен. Приложение работает в автономном режиме."
            )
        }
    }

    /**
     * Скачивает APK из assets релиза с прогрессом (0f..1f) и запускает установку APK.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        apkFileName: String?,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "AutoService-Android-App/$APP_VERSION")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Ошибка скачивания APK: HTTP ${response.code}")
                    )
                }

                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("Пустой файл APK"))

                val totalBytes = body.contentLength()
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val safeName = (apkFileName ?: "autoservice-update.apk")
                    .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val outputFile = File(updatesDir, safeName)

                body.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        onProgress(0.02f)

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val fraction = (downloadedBytes.toFloat() / totalBytes.toFloat())
                                    .coerceIn(0.02f, 0.99f)
                                onProgress(fraction)
                            }
                        }
                        output.flush()
                    }
                }

                onProgress(1.0f)
                withContext(Dispatchers.Main) {
                    promptInstallApk(context, outputFile)
                }
                Result.success(outputFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun promptInstallApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
