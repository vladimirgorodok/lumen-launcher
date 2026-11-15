package ru.lumen.launcher.data

import org.json.JSONArray
import org.json.JSONObject

enum class ServiceKind(
    val label: String,
    val defaultBaseUrl: String,
    val hint: String,
) {
    TEABLE(
        label = "Teable",
        defaultBaseUrl = "https://app.teable.ai",
        hint = "Токен: Teable → Настройки → Токены доступа. Показывает приложения, собранные здесь.",
    ),
    GITHUB(
        label = "GitHub",
        defaultBaseUrl = "https://api.github.com",
        hint = "Логин GitHub. Для публичных репозиториев токен не нужен (анонимный лимит 60 запросов в час).",
    ),
    OPENCHAMBER(
        label = "OpenChamber",
        defaultBaseUrl = "",
        hint = "Адрес сервера OpenCode/OpenChamber и логин с паролем. Показывает рабочие пространства и сессии.",
    ),
}

data class Connection(
    val id: String,
    val kind: ServiceKind,
    val label: String,
    val baseUrl: String,
    val token: String = "",
    val username: String = "",
    val password: String = "",
) {
    val displayLabel: String get() = label.ifBlank { kind.label }
}

fun encodeConnections(connections: List<Connection>): String {
    val array = JSONArray()
    connections.forEach { connection ->
        array.put(
            JSONObject()
                .put("id", connection.id)
                .put("kind", connection.kind.name)
                .put("label", connection.label)
                .put("baseUrl", connection.baseUrl)
                .put("token", connection.token)
                .put("username", connection.username)
                .put("password", connection.password)
        )
    }
    return array.toString()
}

fun decodeConnections(raw: String?): List<Connection> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val kind = runCatching {
                    ServiceKind.valueOf(item.optString("kind", ServiceKind.TEABLE.name))
                }.getOrDefault(ServiceKind.TEABLE)
                val baseUrl = item.optString("baseUrl").trim().ifBlank { kind.defaultBaseUrl }
                val id = item.optString("id").ifBlank { "conn-$index" }
                add(
                    Connection(
                        id = id,
                        kind = kind,
                        label = item.optString("label").trim(),
                        baseUrl = baseUrl,
                        token = item.optString("token"),
                        username = item.optString("username"),
                        password = item.optString("password"),
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}