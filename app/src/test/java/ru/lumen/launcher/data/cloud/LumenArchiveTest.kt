package ru.lumen.launcher.data.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.LumenSettings
import ru.lumen.launcher.data.ServiceKind
import java.io.File
import java.time.LocalDateTime

/** Снимки состояния на устройстве: страховка перед загрузкой «заменой». */
class LumenArchiveTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun store() = FakeLumenStore(
        LumenSettings(
            connections = listOf(
                Connection(
                    id = "conn-github",
                    kind = ServiceKind.GITHUB,
                    label = "GitHub",
                    baseUrl = "https://api.github.com",
                    token = "ghp_secret_token",
                ),
            ),
        ),
    )

    @Test
    fun snapshotHoldsCurrentState() = runBlocking {
        val dir = File(temp.root, "backups")
        val transfer = LumenTransfer(store())
        val archive = LumenArchive(dir, transfer)
        val at = LocalDateTime.of(2026, 11, 18, 9, 30, 0)

        assertTrue(archive.create(at = at))
        assertFalse("второй снимок в ту же секунду не нужен", archive.create(at = at))
        assertTrue("по требованию — можно", archive.create(force = true, at = at))

        assertEquals(1, archive.count())
        val text = File(dir, archive.list().first()).readText()
        assertTrue("снимок — та же копия, что уезжает в облако", text.contains(LumenTransfer.APP_TAG))
        assertTrue(text.contains("conn-github"))
        assertFalse("пароль приложения в снимок не попадает", text.contains("cloud_password"))
    }

    @Test
    fun keepsOnlyLastSnapshots() = runBlocking {
        val archive = LumenArchive(File(temp.root, "backups"), LumenTransfer(store()))
        var at = LocalDateTime.of(2026, 11, 18, 10, 0, 0)

        repeat(LumenArchive.KEEP + 2) {
            assertTrue(archive.create(at = at))
            at = at.plusSeconds(1)
        }

        assertEquals(LumenArchive.KEEP, archive.count())
        assertEquals("lumen-2026-11-18_100006.json", archive.list().first())
        assertFalse("самый старый снимок удалён", archive.list().contains("lumen-2026-11-18_100000.json"))
        assertTrue(archive.list().all { it.startsWith(LumenArchive.PREFIX) && it.endsWith(LumenArchive.SUFFIX) })
    }

    @Test
    fun missingDirectoryIsCreated() = runBlocking {
        val dir = File(temp.root, "нет/такого/каталога")
        val archive = LumenArchive(dir, LumenTransfer(store()))

        assertEquals(0, archive.count())
        assertTrue(archive.create())
        assertTrue(dir.isDirectory)
        assertEquals(1, archive.count())
    }
}
