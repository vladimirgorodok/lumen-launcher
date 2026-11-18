package ru.lumen.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.lumen.launcher.data.cloud.LumenStateStore

private val Context.lumenDataStore by preferencesDataStore(name = "lumen_prefs")

/** Папка на Яндекс Диске по умолчанию. */
const val DEFAULT_CLOUD_FOLDER = "/Люмен"

/** Приводит путь к виду «/Папка/Вложенная»: ведущий слэш, без хвостового. */
fun normalizeCloudFolder(path: String?): String {
    val trimmed = path.orEmpty().trim().replace('\\', '/')
    val withLeading = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    val collapsed = withLeading.replace(Regex("/{2,}"), "/")
    return collapsed.trimEnd('/').ifBlank { DEFAULT_CLOUD_FOLDER }
}

data class LumenSettings(
    val screensaverMinutes: Int = 2,
    val showSeconds: Boolean = true,
    val backgroundSeed: Int = 1,
    val webApps: List<WebApp> = SeedWebApps,
    val connections: List<Connection> = emptyList(),
    val characterId: String = Characters.first().id,
    val skills: SkillsState = SkillsState(),
    val buildsUrl: String = BuildsRegistry.DEFAULT_URL,
    val showBuilds: Boolean = true,
    val facts: List<MemoryFact> = emptyList(),
    val rememberChat: Boolean = true,
    val chats: Map<String, List<ChatMessage>> = emptyMap(),
    val weatherCache: String? = null,
    // Обмен с Яндекс Диском. Эти поля не уезжают в облачную копию.
    val yandexLogin: String = "",
    val yandexPassword: String = "",
    val cloudFolder: String = DEFAULT_CLOUD_FOLDER,
    val autoSync: Boolean = false,
    val cloudKeepCopies: Int = 5,
    val cloudSendSecrets: Boolean = false,
    val cloudLastSyncAt: Long = 0L,
    val cloudLastFileName: String = "",
    val cloudDirty: Boolean = false,
    val cloudLastError: String = "",
) {
    /** Заполнены ли логин и пароль приложения. */
    val cloudConfigured: Boolean
        get() = yandexLogin.isNotBlank() && yandexPassword.isNotBlank()
}

class LumenPreferences(private val context: Context) : LumenStateStore {

    private object Keys {
        val screensaverMinutes: Preferences.Key<Int> = intPreferencesKey("screensaver_minutes")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val backgroundSeed = intPreferencesKey("background_seed")
        val webApps = stringPreferencesKey("web_apps")
        val connections = stringPreferencesKey("connections")
        val characterId = stringPreferencesKey("character_id")
        val skills = stringPreferencesKey("skills")
        val buildsUrl = stringPreferencesKey("builds_url")
        val showBuilds = booleanPreferencesKey("show_builds")
        val memory = stringPreferencesKey("memory_facts")
        val rememberChat = booleanPreferencesKey("remember_chat")
        val chatHistory = stringPreferencesKey("chat_history")
        val weatherCache = stringPreferencesKey("weather_cache")
        val cloudLogin = stringPreferencesKey("cloud_login")
        val cloudPassword = stringPreferencesKey("cloud_password")
        val cloudFolder = stringPreferencesKey("cloud_yandex_folder")
        val cloudAutoSync = booleanPreferencesKey("cloud_auto_sync")
        val cloudKeepCopies = intPreferencesKey("cloud_keep_copies")
        val cloudSendSecrets = booleanPreferencesKey("cloud_send_secrets")
        val cloudLastSyncAt = longPreferencesKey("cloud_last_sync_at")
        val cloudLastFile = stringPreferencesKey("cloud_last_file")
        val cloudDirty = booleanPreferencesKey("cloud_dirty")
        val cloudLastError = stringPreferencesKey("cloud_last_error")
    }

    private fun changed() {
        emitChange()
    }

    override val settings: Flow<LumenSettings> = context.lumenDataStore.data.map { prefs ->
        LumenSettings(
            screensaverMinutes = prefs[Keys.screensaverMinutes] ?: 2,
            showSeconds = prefs[Keys.showSeconds] ?: true,
            backgroundSeed = prefs[Keys.backgroundSeed] ?: 1,
            webApps = decodeWebApps(prefs[Keys.webApps]),
            connections = decodeConnections(prefs[Keys.connections]),
            characterId = prefs[Keys.characterId] ?: Characters.first().id,
            skills = decodeSkills(prefs[Keys.skills]),
            buildsUrl = prefs[Keys.buildsUrl] ?: BuildsRegistry.DEFAULT_URL,
            showBuilds = prefs[Keys.showBuilds] ?: true,
            facts = Memory.decode(prefs[Keys.memory]),
            rememberChat = prefs[Keys.rememberChat] ?: true,
            chats = decodeChats(prefs[Keys.chatHistory]),
            weatherCache = prefs[Keys.weatherCache],
            yandexLogin = prefs[Keys.cloudLogin].orEmpty().trim(),
            yandexPassword = prefs[Keys.cloudPassword].orEmpty(),
            cloudFolder = normalizeCloudFolder(prefs[Keys.cloudFolder]),
            autoSync = prefs[Keys.cloudAutoSync] ?: false,
            cloudKeepCopies = prefs[Keys.cloudKeepCopies] ?: 5,
            cloudSendSecrets = prefs[Keys.cloudSendSecrets] ?: false,
            cloudLastSyncAt = prefs[Keys.cloudLastSyncAt] ?: 0L,
            cloudLastFileName = prefs[Keys.cloudLastFile].orEmpty(),
            cloudDirty = prefs[Keys.cloudDirty] ?: false,
            cloudLastError = prefs[Keys.cloudLastError].orEmpty(),
        )
    }

    override suspend fun setScreensaverMinutes(minutes: Int) {
        context.lumenDataStore.edit { it[Keys.screensaverMinutes] = minutes.coerceIn(0, 120) }
        changed()
    }

    override suspend fun setShowSeconds(show: Boolean) {
        context.lumenDataStore.edit { it[Keys.showSeconds] = show }
        changed()
    }

    override suspend fun setBackgroundSeed(seed: Int) {
        context.lumenDataStore.edit { it[Keys.backgroundSeed] = seed.coerceAtLeast(1) }
        changed()
    }

    override suspend fun setWebApps(apps: List<WebApp>) {
        context.lumenDataStore.edit { it[Keys.webApps] = encodeWebApps(apps) }
        changed()
    }

    override suspend fun setConnections(connections: List<Connection>) {
        context.lumenDataStore.edit { it[Keys.connections] = encodeConnections(connections) }
        changed()
    }

    override suspend fun setCharacterId(id: String) {
        context.lumenDataStore.edit { it[Keys.characterId] = id }
        changed()
    }

    override suspend fun setSkills(skills: SkillsState) {
        context.lumenDataStore.edit { it[Keys.skills] = encodeSkills(skills.enabledIds, skills.custom) }
        changed()
    }

    override suspend fun setBuildsUrl(url: String) {
        context.lumenDataStore.edit {
            it[Keys.buildsUrl] = url.trim().ifBlank { BuildsRegistry.DEFAULT_URL }
        }
        changed()
    }

    override suspend fun setShowBuilds(show: Boolean) {
        context.lumenDataStore.edit { it[Keys.showBuilds] = show }
        changed()
    }

    override suspend fun setMemory(facts: List<MemoryFact>) {
        context.lumenDataStore.edit { it[Keys.memory] = Memory.encode(facts) }
        changed()
    }

    override suspend fun setRememberChat(remember: Boolean) {
        context.lumenDataStore.edit { it[Keys.rememberChat] = remember }
        changed()
    }

    /** История диалога: пишем только последние сообщения каждого персонажа. */
    suspend fun setChatHistory(characterId: String, messages: List<ChatMessage>) {
        context.lumenDataStore.edit { prefs ->
            val current = decodeChats(prefs[Keys.chatHistory]).toMutableMap()
            if (messages.isEmpty()) {
                current.remove(characterId)
            } else {
                current[characterId] = messages.takeLast(CHAT_HISTORY_LIMIT)
            }
            prefs[Keys.chatHistory] = encodeChats(current)
        }
        changed()
    }

    /** История диалога персонажа — читается сразу из хранилища. */
    suspend fun chatFor(characterId: String): List<ChatMessage> =
        settings.first().chats[characterId].orEmpty()

    suspend fun setWeatherCache(raw: String?) {
        context.lumenDataStore.edit { prefs ->
            if (raw.isNullOrBlank()) prefs.remove(Keys.weatherCache) else prefs[Keys.weatherCache] = raw
        }
    }

    // ------------------------------------------------------------------
    // Обмен с Яндекс Диском
    // ------------------------------------------------------------------

    override suspend fun setChats(chats: Map<String, List<ChatMessage>>) {
        context.lumenDataStore.edit {
            it[Keys.chatHistory] = encodeChats(
                chats.mapValues { (_, messages) -> messages.takeLast(CHAT_HISTORY_LIMIT) }
            )
        }
        changed()
    }

    suspend fun setYandexLogin(login: String) {
        context.lumenDataStore.edit { it[Keys.cloudLogin] = login.trim() }
    }

    suspend fun setYandexPassword(password: String) {
        context.lumenDataStore.edit { it[Keys.cloudPassword] = password }
    }

    suspend fun setCloudFolder(folder: String) {
        context.lumenDataStore.edit { it[Keys.cloudFolder] = normalizeCloudFolder(folder) }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.lumenDataStore.edit { it[Keys.cloudAutoSync] = enabled }
    }

    suspend fun setCloudKeepCopies(count: Int) {
        context.lumenDataStore.edit { it[Keys.cloudKeepCopies] = count.coerceIn(1, 50) }
    }

    suspend fun setCloudSendSecrets(enabled: Boolean) {
        context.lumenDataStore.edit { it[Keys.cloudSendSecrets] = enabled }
    }

    /** Запоминает имя выгруженной копии и снимает флаг невыгруженных правок. */
    suspend fun setCloudSynced(fileName: String, atMillis: Long) {
        context.lumenDataStore.edit {
            it[Keys.cloudLastFile] = fileName
            it[Keys.cloudLastSyncAt] = atMillis
            it[Keys.cloudDirty] = false
            it[Keys.cloudLastError] = ""
        }
    }

    suspend fun setCloudDirty(dirty: Boolean) {
        context.lumenDataStore.edit { it[Keys.cloudDirty] = dirty }
    }

    suspend fun setCloudLastError(message: String) {
        context.lumenDataStore.edit { it[Keys.cloudLastError] = message }
    }

    companion object {
        /**
         * Сигнал «данные изменились»: из него взводится флаг невыгруженных правок.
         *
         * Общий на приложение: экземпляров [LumenPreferences] может быть несколько
         * (интерфейс и контейнер обмена), а хранилище одно.
         */
        private val signal = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

        /** Один сигнал изменений на приложение: экземпляров хранилища бывает несколько. */
        internal fun emitChange() {
            signal.tryEmit(Unit)
        }
    }

    override val changes: SharedFlow<Unit> get() = signal.asSharedFlow()
}
