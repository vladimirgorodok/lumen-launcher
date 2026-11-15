package ru.lumen.launcher.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Приложение, собранное «отсюда» — в opencode/OpenChamber. Лежит на GitHub:
 * установленное открывается как обычное приложение, не установленное ведёт
 * на страницу релиза или скачивает APK.
 */
data class BuildApp(
    val id: String,
    val name: String,
    val packageName: String,
    val version: String,
    val description: String,
    val repo: String,
    val releaseUrl: String,
    val apkUrl: String,
    val iconUrl: String?,
    val sizeBytes: Long,
    val discovered: Boolean = false,
)

sealed interface BuildsState {
    data object Loading : BuildsState
    data class Ready(val apps: List<BuildApp>) : BuildsState
    data class Error(val message: String) : BuildsState
}

object BuildsRegistry {

    const val DEFAULT_URL =
        "https://raw.githubusercontent.com/vladimirgorodok/lumen-launcher/master/registry/apps.json"

    suspend fun load(url: String): Result<List<BuildApp>> = withContext(Dispatchers.IO) {
        val target = normalizeBaseUrl(url).ifBlank { DEFAULT_URL }
        val raw = Http.getText(target, timeoutMs = 15000)
            ?: return@withContext Result.failure(IllegalStateException("реестр недоступен: $target"))
        runCatching {
            val apps = JSONObject(raw).optJSONArray("apps") ?: JSONArray()
            buildList {
                for (index in 0 until apps.length()) {
                    val item = apps.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    if (name.isEmpty()) continue
                    add(
                        BuildApp(
                            id = item.optString("id").ifBlank { "build-$index" },
                            name = name,
                            packageName = item.optString("package").trim(),
                            version = item.optString("version").trim(),
                            description = item.optString("description").trim(),
                            repo = item.optString("repo").trim(),
                            releaseUrl = item.optString("releaseUrl").trim(),
                            apkUrl = item.optString("apkUrl").trim(),
                            iconUrl = item.optString("icon").trim().ifBlank { null },
                            sizeBytes = item.optLong("size", 0L),
                        )
                    )
                }
            }.sortedBy { it.name.lowercase() }
        }
    }

    /**
     * Дополнительно смотрит GitHub-подключения: репозитории, у которых в
     * последнем релизе есть APK, но которых нет в реестре.
     */
    suspend fun discover(connection: Connection, known: Set<String>): List<BuildApp> =
        withContext(Dispatchers.IO) {
            val base = normalizeBaseUrl(connection.baseUrl).ifBlank { connection.kind.defaultBaseUrl }
            val login = connection.username.trim()
            val path = if (login.isBlank() || login == "me") "/user/repos" else "/users/$login/repos"
            val headers = Http.bearer(connection.token)
            val reposRaw = Http.getText("$base$path?per_page=100&sort=updated", headers) ?: return@withContext emptyList()
            val repos = runCatching { JSONArray(reposRaw) }.getOrNull() ?: return@withContext emptyList()
            val found = mutableListOf<BuildApp>()
            for (index in 0 until repos.length()) {
                val repo = repos.optJSONObject(index) ?: continue
                val fullName = repo.optString("full_name")
                if (fullName.isBlank() || fullName.lowercase() in known) continue
                val releasesRaw = Http.getText("$base/repos/$fullName/releases?per_page=5", headers) ?: continue
                val releases = runCatching { JSONArray(releasesRaw) }.getOrNull() ?: continue
                for (releaseIndex in 0 until releases.length()) {
                    val release = releases.optJSONObject(releaseIndex) ?: continue
                    val assets = release.optJSONArray("assets") ?: continue
                    var apk: JSONObject? = null
                    for (assetIndex in 0 until assets.length()) {
                        val asset = assets.optJSONObject(assetIndex) ?: continue
                        if (!asset.optString("name").endsWith(".apk")) continue
                        if (apk == null || asset.optLong("size") < apk.optLong("size")) apk = asset
                    }
                    val chosen = apk ?: continue
                    found += BuildApp(
                        id = fullName.lowercase().replace('/', '-'),
                        name = repo.optString("name"),
                        packageName = "",
                        version = release.optString("tag_name"),
                        description = repo.optString("description").trim(),
                        repo = fullName,
                        releaseUrl = release.optString("html_url"),
                        apkUrl = chosen.optString("browser_download_url"),
                        iconUrl = null,
                        sizeBytes = chosen.optLong("size"),
                        discovered = true,
                    )
                    break
                }
            }
            found
        }
}

/** Скачивание и запуск системного установщика APK. */
class BuildInstaller(private val context: Context) {

    private val appContext = context.applicationContext

    suspend fun download(
        app: BuildApp,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        if (app.apkUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("у сборки нет ссылки на APK"))
        }
        val target = File(File(appContext.cacheDir, "apk"), "${app.id}.apk")
        if (target.exists()) target.delete()
        val error = Http.download(app.apkUrl, target, onProgress = onProgress)
        if (error != null) {
            Result.failure(IllegalStateException(error))
        } else if (!target.exists() || target.length() == 0L) {
            Result.failure(IllegalStateException("файл не скачался"))
        } else {
            Result.success(target)
        }
    }

    /** Отдаёт APK системному установщику. */
    fun install(file: File): Result<Unit> = runCatching {
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    /** Убирает скачанные файлы. */
    fun clearCache() {
        runCatching { File(appContext.cacheDir, "apk").deleteRecursively() }
    }
}
