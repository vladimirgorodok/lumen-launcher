package ru.lumen.launcher.data.cloud

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Разбор и подготовка путей WebDAV (Яндекс Диск).
 *
 * Вынесено отдельно от сети и от Android-классов, чтобы это можно было
 * покрыть обычными unit-тестами на JVM.
 */
internal object WebDavXml {

    /** Разбирает ответ `207 Multi-Status` в список файлов и папок. */
    fun parseMultiStatus(xml: String): List<RemoteEntry> {
        val parser = XmlPullParserFactory.newInstance()
            .apply { isNamespaceAware = false }
            .newPullParser()
        parser.setInput(StringReader(xml))

        val entries = ArrayList<RemoteEntry>()
        val stack = ArrayDeque<String>()
        var inResponse = false
        var href: String? = null
        var displayName: String? = null
        var isDirectory = false
        var size = 0L
        var modified = 0L

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val local = localName(parser)
                    stack.addLast(local)
                    when (local) {
                        "response" -> {
                            inResponse = true
                            href = null
                            displayName = null
                            isDirectory = false
                            size = 0L
                            modified = 0L
                        }
                        "collection" -> if (inResponse) isDirectory = true
                    }
                }

                XmlPullParser.TEXT -> if (inResponse) {
                    val value = parser.text?.trim().orEmpty()
                    if (value.isNotEmpty()) {
                        when (stack.lastOrNull()) {
                            "href" -> href = value
                            "displayname" -> displayName = value
                            "getcontentlength" -> size = value.toLongOrNull() ?: 0L
                            "getlastmodified" -> modified = parseHttpDate(value)
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val local = localName(parser)
                    if (local == "response" && inResponse) {
                        href?.let { raw ->
                            val path = pathOf(raw)
                            val name = displayName?.takeIf { it.isNotBlank() }
                                ?: path.substringAfterLast('/')
                            entries += RemoteEntry(
                                name = name,
                                path = path,
                                isDirectory = isDirectory,
                                size = size,
                                modified = modified,
                            )
                        }
                        inResponse = false
                    }
                    if (stack.isNotEmpty()) stack.removeLast()
                }
            }
            event = parser.next()
        }
        return entries
    }

    /** Свободное место на Диске из ответа PROPFIND. */
    fun parseQuota(xml: String): Long =
        Regex("(?i)<[^>]*quota-available-bytes[^>]*>\\s*(\\d+)\\s*<")
            .find(xml)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?: 0L

    /** Превращает `href` из ответа в обычный путь «/Папка/файл». */
    fun pathOf(href: String): String {
        val withoutHost = if (href.startsWith("http", ignoreCase = true)) {
            val afterScheme = href.substringAfter("://", href)
            val slash = afterScheme.indexOf('/')
            if (slash >= 0) afterScheme.substring(slash) else "/"
        } else {
            href
        }
        val decoded = percentDecode(withoutHost)
        val withLeading = if (decoded.startsWith("/")) decoded else "/$decoded"
        return withLeading.trimEnd('/').ifBlank { "/" }
    }

    /** Кодирует путь для запроса, сохраняя разделители «/». */
    fun encodePath(path: String): String =
        path.split('/').joinToString("/") { segment ->
            if (segment.isEmpty()) segment
            else URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }

    private fun percentDecode(value: String): String = runCatching {
        // Плюс в путях — обычный символ, а URLDecoder считает его пробелом.
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
    }.getOrDefault(value)

    private fun localName(parser: XmlPullParser): String =
        parser.name?.substringAfter(':')?.lowercase(Locale.ROOT).orEmpty()

    private fun parseHttpDate(value: String): Long = runCatching {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }.getOrDefault(0L)
}
