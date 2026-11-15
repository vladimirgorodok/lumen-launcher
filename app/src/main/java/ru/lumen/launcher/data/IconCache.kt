package ru.lumen.launcher.data

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Иконки веб-приложений: сначала манифест PWA, потом favicon.
 * Результат кэшируется в памяти и на диске.
 */
object IconCache {

    private val memory = LruCache<String, ImageBitmap>(96)

    suspend fun load(context: Context, appUrl: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        val target = appUrl?.trim().orEmpty()
        if (target.isBlank()) return@withContext null
        memory.get(target)?.let { return@withContext it }

        val file = diskFile(context, target)
        if (file.exists()) {
            decode(file.readBytes())?.let {
                memory.put(target, it)
                return@withContext it
            }
        }

        val bytes = downloadIconBytes(target) ?: return@withContext null
        decode(bytes)?.let { bitmap ->
            memory.put(target, bitmap)
            runCatching {
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
            }
            bitmap
        }
    }

    private fun downloadIconBytes(appUrl: String): ByteArray? {
        val origin = originOf(appUrl) ?: return null
        if (appUrl.matches(Regex("(?i).*\\.(png|jpg|jpeg|webp|ico)$"))) {
            Http.getBytes(appUrl)?.let { return it }
        }
        manifestIconUrl(origin)?.let { iconUrl ->
            Http.getBytes(iconUrl)?.let { return it }
        }
        return Http.getBytes("$origin/favicon.ico")
    }

    private fun manifestIconUrl(origin: String): String? {
        val raw = Http.getText("$origin/manifest.webmanifest", timeoutMs = 5000)
            ?: Http.getText("$origin/manifest.json", timeoutMs = 5000)
            ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val icons = json.optJSONArray("icons") ?: return null
        var best: String? = null
        var bestScore = -1
        for (index in 0 until icons.length()) {
            val icon = icons.optJSONObject(index) ?: continue
            val src = icon.optString("src").trim()
            if (src.isBlank()) continue
            val purpose = icon.optString("purpose", "any")
            val sizeScore = icon.optString("sizes").split(" ").sumOf { token ->
                token.substringBefore('x').toIntOrNull() ?: 0
            }
            val score = sizeScore + if (purpose.contains("any")) 100 else 0
            if (score > bestScore) {
                bestScore = score
                best = resolve(src, origin)
            }
        }
        return best
    }

    private fun resolve(src: String, origin: String): String = when {
        src.startsWith("http://") || src.startsWith("https://") -> src
        src.startsWith("//") -> "https:$src"
        src.startsWith("/") -> "$origin$src"
        else -> "$origin/$src"
    }

    private fun originOf(url: String): String? = runCatching {
        val parsed = java.net.URI(url)
        val scheme = parsed.scheme ?: return null
        val host = parsed.host ?: return null
        "$scheme://$host"
    }.getOrNull()

    private fun decode(bytes: ByteArray): ImageBitmap? = runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()

    private fun diskFile(context: Context, url: String): File =
        File(File(context.cacheDir, "icons"), sha1(url) + ".img")

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}