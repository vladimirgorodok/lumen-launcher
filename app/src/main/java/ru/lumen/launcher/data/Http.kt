package ru.lumen.launcher.data

import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class HttpTextResponse(
    val code: Int,
    val body: String,
    val failure: String? = null,
)

/** Небольшой HTTP-слой: таймауты, заголовки, текст и байты. */
object Http {
    private const val DEFAULT_TIMEOUT = 8000

    fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = DEFAULT_TIMEOUT,
    ): String? = request(url, headers, timeoutMs)?.let { connection ->
        runCatching { connection.inputStream.bufferedReader().use { it.readText() } }
            .onFailure { Log.w("LumenHttp", "read failed: ${it.message}") }
            .getOrNull()
    }

    fun getBytes(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = DEFAULT_TIMEOUT,
    ): ByteArray? = request(url, headers, timeoutMs)?.let { connection ->
        runCatching { connection.inputStream.use { it.readBytes() } }
            .onFailure { Log.w("LumenHttp", "read failed: ${it.message}") }
            .getOrNull()
    }

    private fun request(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Int,
    ): HttpURLConnection? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                setRequestProperty("User-Agent", "LumenLauncher/1.1 (Android)")
                setRequestProperty("Accept", "application/json, text/html;q=0.9, */*;q=0.8")
                headers.forEach { (key, value) -> if (value.isNotBlank()) setRequestProperty(key, value) }
            }
            val code = connection.responseCode
            if (code in 200..299) connection else {
                Log.w("LumenHttp", "GET $url -> $code")
                connection.disconnect()
                null
            }
        } catch (error: Exception) {
            Log.w("LumenHttp", "GET $url failed: ${error.message}")
            connection?.disconnect()
            null
        }
    }

    /** POST с JSON-телом: возвращает тело ответа или null. */
    fun postJson(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 30000,
    ): String? {
        val response = postJsonResponse(url, body, headers, timeoutMs)
        return if (response.code in 200..299) response.body else null
    }

    /** Ответ сервера целиком: код, тело и причина сбоя связи. */
    fun postJsonResponse(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 30000,
    ): HttpTextResponse {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("User-Agent", "LumenLauncher/1.2 (Android)")
                headers.forEach { (key, value) -> if (value.isNotBlank()) setRequestProperty(key, value) }
            }
            connection.outputStream.use { it.write(body.toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) Log.w("LumenHttp", "POST $url -> $code ${text.take(300)}")
            HttpTextResponse(code = code, body = text)
        } catch (error: Exception) {
            Log.w("LumenHttp", "POST $url failed: ${error.message}")
            HttpTextResponse(code = -1, body = "", failure = error.message ?: error.javaClass.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    /** Скачивает файл потоком, сообщая о прогрессе. Возвращает null при успехе. */
    fun download(
        url: String,
        target: File,
        timeoutMs: Int = 60000,
        onProgress: (Float) -> Unit = {},
    ): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "LumenLauncher/1.5 (Android)")
                setRequestProperty("Accept", "application/octet-stream, */*")
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                return "сервер ответил $code"
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            target.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress(written.toFloat() / total)
                    }
                }
            }
            null
        } catch (error: Exception) {
            Log.w("LumenHttp", "download $url failed: ${error.message}")
            runCatching { target.delete() }
            error.message ?: error.javaClass.simpleName
        } finally {
            connection?.disconnect()
        }
    }

    fun bearer(token: String): Map<String, String> =
        if (token.isBlank()) emptyMap() else mapOf("Authorization" to "Bearer $token")

    fun basic(username: String, password: String): Map<String, String> =
        if (username.isBlank() && password.isBlank()) {
            emptyMap()
        } else {
            val raw = "$username:$password"
            val encoded = android.util.Base64.encodeToString(raw.toByteArray(), android.util.Base64.NO_WRAP)
            mapOf("Authorization" to "Basic $encoded")
        }
}