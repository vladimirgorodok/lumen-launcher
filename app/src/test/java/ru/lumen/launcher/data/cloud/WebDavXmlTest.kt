package ru.lumen.launcher.data.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Разбор ответов Яндекс Диска по WebDAV. */
class WebDavXmlTest {

    private val response = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
          <d:response>
            <d:href>/%D0%9B%D1%8E%D0%BC%D0%B5%D0%BD/</d:href>
            <d:propstat>
              <d:prop>
                <d:displayname>Люмен</d:displayname>
                <d:resourcetype><d:collection/></d:resourcetype>
              </d:prop>
              <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
          </d:response>
          <d:response>
            <d:href>/%D0%9B%D1%8E%D0%BC%D0%B5%D0%BD/lumen-2026-11-18_071530.json</d:href>
            <d:propstat>
              <d:prop>
                <d:displayname>lumen-2026-11-18_071530.json</d:displayname>
                <d:resourcetype/>
                <d:getcontentlength>2048</d:getcontentlength>
                <d:getlastmodified>Wed, 18 Nov 2026 07:15:30 GMT</d:getlastmodified>
              </d:prop>
              <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
          </d:response>
        </d:multistatus>
    """.trimIndent()

    @Test
    fun parsesFolderAndFile() {
        val entries = WebDavXml.parseMultiStatus(response)

        assertEquals(2, entries.size)

        assertEquals("Люмен", entries[0].name)
        assertEquals("/Люмен", entries[0].path)
        assertTrue(entries[0].isDirectory)

        assertEquals("lumen-2026-11-18_071530.json", entries[1].name)
        assertFalse(entries[1].isDirectory)
        assertEquals(2048L, entries[1].size)
        assertTrue("дата изменения разобрана", entries[1].modified > 0)
    }

    @Test
    fun emptyResponseGivesEmptyList() {
        assertTrue(WebDavXml.parseMultiStatus("<d:multistatus xmlns:d=\"DAV:\"/>").isEmpty())
    }

    @Test
    fun pathFromHrefIsDecoded() {
        assertEquals("/Люмен", WebDavXml.pathOf("https://webdav.yandex.ru/%D0%9B%D1%8E%D0%BC%D0%B5%D0%BD/"))
        assertEquals("/Люмен/файл.json", WebDavXml.pathOf("/Люмен/файл.json"))
        assertEquals("/", WebDavXml.pathOf("https://webdav.yandex.ru/"))
    }

    @Test
    fun pathIsEncodedKeepingSlashes() {
        assertEquals("/%D0%9B%D1%8E%D0%BC%D0%B5%D0%BD/lumen-2026.json", WebDavXml.encodePath("/Люмен/lumen-2026.json"))
    }

    @Test
    fun quotaIsReadFromResponse() {
        val xml = "<d:prop><d:quota-available-bytes>10737418240</d:quota-available-bytes></d:prop>"
        assertEquals(10_737_418_240L, WebDavXml.parseQuota(xml))
        assertEquals(0L, WebDavXml.parseQuota("<d:prop/>"))
    }
}
