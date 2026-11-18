package ru.lumen.launcher.data.cloud

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Снимок состояния на устройстве.
 *
 * Делается перед загрузкой из облака «заменой»: если в облаке оказалась не та
 * копия, из снимка всё можно вернуть. Снимки лежат в `filesDir/backups`.
 *
 * Каталог передаётся снаружи — так же, как у обмена с облаком, это позволяет
 * проверять снимки обычными JVM-тестами.
 */
class LumenArchive(private val directory: File, private val transfer: LumenTransfer) {

    companion object {
        /** Сколько последних снимков держим. */
        const val KEEP = 5

        const val PREFIX = "lumen-"
        const val SUFFIX = ".json"

        private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")

        fun directory(context: Context): File = File(context.filesDir, "backups")
    }

    /** Создаёт снимок. false — снимок за эту секунду уже есть, а force не задан. */
    suspend fun create(
        force: Boolean = false,
        at: LocalDateTime = LocalDateTime.now(),
    ): Boolean = withContext(Dispatchers.IO) {
        if (!directory.exists() && !directory.mkdirs()) return@withContext false

        val file = File(directory, "$PREFIX${at.format(STAMP)}$SUFFIX")
        if (file.exists() && !force) return@withContext false

        file.writeText(transfer.snapshotJson())

        // Держим последние KEEP снимков, остальные удаляем.
        filesIn(directory).drop(KEEP).forEach { runCatching { it.delete() } }
        true
    }

    /** Имена снимков, самые свежие — первыми. */
    fun list(): List<String> = filesIn(directory).map { it.name }

    /** Сколько снимков лежит на устройстве. */
    suspend fun count(): Int = withContext(Dispatchers.IO) { filesIn(directory).size }

    private fun filesIn(dir: File): List<File> =
        dir.listFiles { file ->
            file.isFile && file.name.startsWith(PREFIX) && file.name.endsWith(SUFFIX)
        }?.sortedByDescending { it.name }.orEmpty()
}
