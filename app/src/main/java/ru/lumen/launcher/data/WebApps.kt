package ru.lumen.launcher.data

import org.json.JSONArray
import org.json.JSONObject

data class WebApp(
    val name: String,
    val url: String,
    val colorIndex: Int,
)

/**
 * Веб-приложения, собранные в opencode/OpenChamber. Они живут отдельно от
 * PackageManager: открываются в собственном WebView-контейнере.
 */
val SeedWebApps: List<WebApp> = listOf(
    WebApp("Люмен", "https://appqr1zalped2qimaez.teable.app", 0),
    WebApp("Код дня", "https://vladimirgorodok.github.io/kod-dnya/", 1),
    WebApp("Таблица кодов", "https://vladimirgorodok.github.io/tablica-kodov/", 4),
    WebApp("Яндекс.Диск Фото", "https://vladimirgorodok.github.io/yadisk-photo-ai/", 2),
)

private fun JSONArray.toStringList(): List<String> = buildList {
    for (index in 0 until length()) add(optString(index, ""))
}

fun encodeWebApps(apps: List<WebApp>): String {
    val array = JSONArray()
    apps.forEach { app ->
        array.put(
            JSONObject()
                .put("name", app.name)
                .put("url", app.url)
                .put("color", app.colorIndex)
        )
    }
    return array.toString()
}

fun decodeWebApps(raw: String?): List<WebApp> {
    if (raw.isNullOrBlank()) return SeedWebApps
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val url = item.optString("url").trim()
                if (name.isNotEmpty() && url.isNotEmpty()) {
                    add(WebApp(name, url, item.optInt("color", index % 6)))
                }
            }
        }
    }.getOrDefault(SeedWebApps)
}

fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}