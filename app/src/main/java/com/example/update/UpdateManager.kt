package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
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
 * Репозиторий GitHub для проверки и загрузки обновлений через GitHub Releases API.
 */
const val GITHUB_REPO = "vovik06302-ai/avtoServis"

/**
 * Текущая версия приложения из BuildConfig.VERSION_NAME (например, "1.0.0").
 * Отображается на экране «О приложении» и мелким текстом внизу главного экрана.
 */
val APP_VERSION: String = BuildConfig.VERSION_NAME

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
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    /**
     * Сравнивает версию из tag_name (без буквы "v", например v1.0.1 -> 1.0.1)
     * с текущей версией приложения (BuildConfig.VERSION_NAME) по числам major.minor.patch.
     * Возвращает:
     *  > 0, если remoteTag новее currentVersion
     *  = 0, если версии равны
     *  < 0, если remoteTag старее currentVersion
     */
    fun compareVersionsNumeric(
        remoteTag: String,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Int {
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
     * https://api.github.com/repos/vovik06302-ai/avtoServis/releases/latest
     */
    suspend fun checkForUpdates(customRepoOverride: String? = null): UpdateCheckResult = withContext(Dispatchers.IO) {
        val repo = customRepoOverride?.trim()?.takeIf {
            it.isNotEmpty() && it != "ВАШ_ЛОГИН/ВАШ_РЕПОЗИТОРИЙ"
        } ?: GITHUB_REPO

        val url = "https://api.github.com/repos/$repo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "AutoService-Android-App/${BuildConfig.VERSION_NAME}")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext UpdateCheckResult.Error(
                        "В репозитории $repo ещё нет опубликованных релизов (Releases)."
                    )
                }
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error(
                        "Не удалось проверить обновление (код ${response.code}). Приложение продолжает работать."
                    )
                }

                val bodyStr = response.body?.string()
                    ?: return@withContext UpdateCheckResult.Error("Пустой ответ от сервера GitHub.")

                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "").trim()
                if (tagName.isEmpty()) {
                    return@withContext UpdateCheckResult.Error("В релизе на GitHub не указан тег версии (tag_name).")
                }

                val cleanVersion = tagName.removePrefix("v").removePrefix("V")
                val releaseNotes = json.optString("body", "").trim().ifEmpty {
                    "Исправление ошибок и улучшение работы приложения."
                }
                val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases/latest")

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

                val cmp = compareVersionsNumeric(tagName, BuildConfig.VERSION_NAME)
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
                    UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                "Нет подключения к интернету или GitHub недоступен. Приложение продолжает работать в автономном режиме."
            )
        }
    }

    /**
     * Скачивает файл .apk из assets релиза с отображением прогресса (0f..1f)
     * и запускает установку APK через FileProvider.
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
                .header("User-Agent", "AutoService-Android-App/${BuildConfig.VERSION_NAME}")
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

    /**
     * Запускает системный установщик APK через FileProvider.
     * На Android 8.0+ (API 26+) проверяет разрешение canRequestPackageInstalls()
     * и при необходимости открывает системный экран включения установки из этого источника.
     */
    fun promptInstallApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            if (!canInstall) {
                Toast.makeText(
                    context,
                    "Включите переключатель «Разрешить установку из этого источника» и нажмите Назад для установки обновления",
                    Toast.LENGTH_LONG
                ).show()
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

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
