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
        if (connection.baseUrl.isBlank()) return@withContext Result.failure(IllegalStateException("Не указан адрес ИИ"))
        if (connection.model.isBlank()) return@withContext Result.failure(IllegalStateException("Не указана модель"))

        val payload = JSONObject()
            .put("model", connection.model)
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

        val url = connection.baseUrl.trimEnd('/') + "/chat/completions"
        val raw = Http.postJson(url, payload, Http.bearer(connection.token))
            ?: return@withContext Result.failure(IllegalStateException("ИИ не ответил — проверьте адрес, ключ и модель"))

        runCatching {
            val choices = JSONObject(raw).optJSONArray("choices")
            val content = choices?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
            if (content.isNullOrBlank()) throw IllegalStateException("Пустой ответ ИИ")
            content
        }
    }

    /**
     * Разбор ответа персонажа. Если это команда устройству — возвращаем её,
     * иначе текст для показа.
     */
    fun parseAction(reply: String): DeviceCommand? {
        val trimmed = reply.trim()
        if (!trimmed.startsWith("{")) return null
        val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
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