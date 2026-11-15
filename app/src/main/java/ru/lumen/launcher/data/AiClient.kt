package ru.lumen.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ChatMessage(
    val role: String,
    val content: String,
    val action: String? = null,
)

object AiClient {

    /** Ответ персонажа через OpenAI-совместимое подключение. */
    suspend fun complete(
        connection: Connection,
        messages: List<ChatMessage>,
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(connection.baseUrl)
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("не указан адрес ИИ"))
        }
        if (connection.token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("не указан ключ (токен) ИИ"))
        }
        if (connection.model.isBlank()) {
            return@withContext Result.failure(IllegalStateException("не указана модель ИИ"))
        }

        val payload = JSONObject()
            .put("model", connection.model.trim())
            .put("temperature", 0.7)
            .put(
                "messages",
                JSONArray().apply {
                    messages.forEach { message ->
                        put(JSONObject().put("role", message.role).put("content", message.content))
                    }
                },
            )
            .toString()

        val url = "$baseUrl/chat/completions"
        val response = Http.postJsonResponse(url, payload, Http.bearer(connection.token))

        if (response.failure != null) {
            return@withContext Result.failure(
                IllegalStateException("нет связи с $url — ${response.failure}")
            )
        }
        if (response.code !in 200..299) {
            val detail = errorMessage(response.body)
            return@withContext Result.failure(
                IllegalStateException(
                    buildString {
                        append("сервер ответил ${response.code}")
                        if (!detail.isNullOrBlank()) append(": $detail")
                        append("\n$url")
                    }
                )
            )
        }

        runCatching {
            val choices = JSONObject(response.body).optJSONArray("choices")
            val content = choices?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
            if (content.isNullOrBlank()) throw IllegalStateException("пустой ответ ИИ")
            content
        }
    }

    /** Понятное описание ошибки из тела ответа OpenAI-совместимого API. */
    private fun errorMessage(body: String): String? {
        if (body.isBlank()) return null
        val json = runCatching { JSONObject(body) }.getOrNull()
        val message = json?.optJSONObject("error")?.optString("message")
            ?: json?.optString("message")
        return message?.trim()?.takeIf { it.isNotBlank() } ?: body.trim().take(200)
    }

    /**
     * Разбор ответа персонажа. Если это команда устройству — возвращаем её,
     * иначе текст для показа.
     */
    fun parseAction(reply: String): DeviceCommand? {
        val trimmed = reply.trim()
        val candidate = when {
            trimmed.startsWith("{") -> trimmed
            else -> {
                // Модель могла обернуть команду в пояснение или блок кода.
                val start = trimmed.indexOf('{')
                val end = trimmed.lastIndexOf('}')
                if (start < 0 || end <= start) return null
                trimmed.substring(start, end + 1)
            }
        }
        val json = runCatching { JSONObject(candidate) }.getOrNull() ?: return null
        val action = json.optString("action").trim()
        if (action.isEmpty()) return null
        return DeviceCommand(
            action = action,
            text = json.optString("text").trim(),
            title = json.optString("title").trim(),
            time = json.optString("time").trim(),
            hour = if (json.has("hour")) json.optInt("hour") else null,
            minute = if (json.has("minute")) json.optInt("minute") else null,
            target = json.optString("target").trim(),
            url = json.optString("url").trim(),
        )
    }
}

data class DeviceCommand(
    val action: String,
    val text: String = "",
    val title: String = "",
    val time: String = "",
    val hour: Int? = null,
    val minute: Int? = null,
    val target: String = "",
    val url: String = "",
)