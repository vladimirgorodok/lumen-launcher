package ru.lumen.launcher.data.cloud

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import ru.lumen.launcher.data.LumenPreferences
import ru.lumen.launcher.data.normalizeCloudFolder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Что получилось при загрузке из облака. */
data class CloudPullResult(val fileName: String, val records: Int)

/** Что записалось в облако. */
data class CloudPushResult(val dataName: String)

/**
 * Обмен данными «Люмена» с Яндекс Диском.
 *
 * В облачную папку пишется один файл:
 *  - `lumen-<дата_время>.json` — настройки, веб-приложения, подключения,
 *    память персонажа и история диалогов.
 *
 * Хранятся последние [LumenPreferences] копии, более старые удаляются.
 */
class CloudSync(
    private val settings: LumenPreferences,
    private val transfer: LumenTransfer,
) {

    companion object {
        const val PREFIX = "lumen-"
        const val SUFFIX = ".json"

        private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")

        fun snapshotName(at: LocalDateTime = LocalDateTime.now()): String =
            "$PREFIX${at.format(STAMP)}$SUFFIX"
    }

    private suspend fun client(login: String, password: String): YandexDiskClient {
        if (login.isBlank() || password.isBlank()) {
            throw CloudException("Заполните логин Яндекс и пароль приложения")
        }
        return YandexDiskClient(login, password)
    }

    private suspend fun clientFromSettings(): YandexDiskClient {
        val state = settings.settings.first()
        return client(state.yandexLogin, state.yandexPassword)
    }

    private suspend fun folder(): String =
        normalizeCloudFolder(settings.settings.first().cloudFolder)

    /** Проверка логина, пароля и папки (кнопка «Проверить подключение»). */
    suspend fun checkConnection(login: String, password: String, folder: String): String =
        client(login, password).checkAccess(normalizeCloudFolder(folder))

    /** Папки внутри пути — для выбора папки синхронизации. */
    suspend fun listFolders(path: String): List<RemoteEntry> =
        clientFromSettings().list(normalizeCloudFolder(path)).filter { it.isDirectory }

    /** Создаёт папку синхронизации (вместе с родительскими). */
    suspend fun createFolder(path: String) {
        clientFromSettings().createFolderChain(normalizeCloudFolder(path))
    }

    /** Копии в папке, самые свежие — первыми. */
    suspend fun listSnapshots(folder: String? = null): List<RemoteEntry> =
        snapshotsIn(clientFromSettings(), folder?.let(::normalizeCloudFolder) ?: folder())

    /** Копии в папке. Ошибку сети не скрываем: о ней нужно сказать пользователю. */
    private suspend fun snapshotsIn(client: YandexDiskClient, folder: String): List<RemoteEntry> =
        client.list(folder)
            .filter { !it.isDirectory && it.name.startsWith(PREFIX) && it.name.endsWith(SUFFIX, true) }
            .sortedByDescending { it.name }

    // ------------------------------------------------------------------
    // Выгрузка
    // ------------------------------------------------------------------

    /** Сохраняет копию данных. Старые копии сверх лимита удаляются. */
    suspend fun push(): CloudPushResult {
        val current = settings.settings.first()
        val target = normalizeCloudFolder(current.cloudFolder)

        // Пустое состояние не выгружаем: иначе можно затереть облачную копию,
        // где лежат данные с другого устройства.
        if (transfer.isEmpty()) {
            throw CloudException("На устройстве нет данных для выгрузки — облачная копия не тронута")
        }

        val client = client(current.yandexLogin, current.yandexPassword)
        client.createFolderChain(target)

        val dataName = snapshotName(LocalDateTime.now())
        uploadChecked(client, "$target/$dataName", transfer.snapshotJson().toByteArray(Charsets.UTF_8))

        // Чистка старых копий не должна считаться ошибкой выгрузки.
        val keep = current.cloudKeepCopies.coerceIn(1, 50)
        runCatching { snapshotsIn(client, target).drop(keep).forEach { client.delete(it.path) } }

        settings.setCloudSynced(dataName, System.currentTimeMillis())
        return CloudPushResult(dataName)
    }

    /**
     * Выгружает файл и проверяет результат.
     *
     * На мобильном интернете бывает так: файл на Диск записался, а ответ до
     * приложения не дошёл. Тогда это не ошибка — сравниваем размер файла.
     */
    private suspend fun uploadChecked(client: YandexDiskClient, path: String, bytes: ByteArray) {
        try {
            client.upload(path, bytes)
            return
        } catch (error: CloudException) {
            val remoteSize = runCatching { client.remoteSize(path) }.getOrNull()
            if (remoteSize == bytes.size.toLong()) return
        }
        // Вторая попытка: если и она не удастся, ошибка уйдёт наверх.
        client.upload(path, bytes)
    }

    // ------------------------------------------------------------------
    // Загрузка
    // ------------------------------------------------------------------

    /**
     * Загружает свежую копию. [replaceAll] = true — полная замена,
     * иначе совпадающие записи обновляются, свои остаются.
     */
    suspend fun pull(replaceAll: Boolean = true): CloudPullResult {
        val current = settings.settings.first()
        val target = normalizeCloudFolder(current.cloudFolder)
        val client = client(current.yandexLogin, current.yandexPassword)

        val newest = snapshotsIn(client, target).firstOrNull()
            ?: throw CloudException("В папке «$target» нет файлов синхронизации")
        val bytes = client.download(newest.path)
        val records = transfer.importFromText(bytes.toString(Charsets.UTF_8), replaceAll)

        settings.setCloudSynced(newest.name, System.currentTimeMillis())
        return CloudPullResult(newest.name, records)
    }

    /** Имя самой свежей копии (null — копий нет). */
    suspend fun newestSnapshotName(): String? = listSnapshots().firstOrNull()?.name
}
