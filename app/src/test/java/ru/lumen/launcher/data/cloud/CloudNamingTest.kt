package ru.lumen.launcher.data.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.lumen.launcher.data.DEFAULT_CLOUD_FOLDER
import ru.lumen.launcher.data.normalizeCloudFolder
import java.time.LocalDateTime

/** Имена файлов в облаке и путь папки. */
class CloudNamingTest {

    @Test
    fun snapshotNameLooksLikeLumenStamp() {
        val at = LocalDateTime.of(2026, 11, 18, 7, 15, 30)
        assertEquals("lumen-2026-11-18_071530.json", CloudSync.snapshotName(at))
        assertTrue(CloudSync.snapshotName(at).startsWith(CloudSync.PREFIX))
        assertTrue(CloudSync.snapshotName(at).endsWith(CloudSync.SUFFIX))
    }

    @Test
    fun onlyOwnSnapshotsAreListed() {
        val names = listOf(
            "lumen-2026-11-18_071530.json",
            "lumen-2026-11-17_101010.json",
            "чужой-файл.json",
            "lumen-2026-11-18_071530.txt",
            "lumen-2026-11-18_071530.json.bak",
        )
        val ours = names.filter {
            it.startsWith(CloudSync.PREFIX) && it.endsWith(CloudSync.SUFFIX, ignoreCase = true)
        }
        assertEquals(2, ours.size)
        // Свежие — первыми: так же, как в CloudSync.snapshotsIn.
        assertEquals("lumen-2026-11-18_071530.json", ours.sortedDescending().first())
    }

    @Test
    fun defaultFolderIsLumen() {
        assertEquals("/Люмен", DEFAULT_CLOUD_FOLDER)
        assertEquals("/Люмен", normalizeCloudFolder(""))
        assertEquals("/Люмен", normalizeCloudFolder("   "))
        assertEquals("/Люмен", normalizeCloudFolder("Люмен"))
        assertEquals("/Люмен", normalizeCloudFolder("/Люмен/"))
        assertEquals("/Люмен/Копии", normalizeCloudFolder("//Люмен//Копии/"))
        assertEquals("/Люмен/Копии", normalizeCloudFolder("\\Люмен\\Копии"))
    }
}
