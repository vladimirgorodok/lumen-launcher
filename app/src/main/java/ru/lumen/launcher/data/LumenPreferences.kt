package ru.lumen.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lumenDataStore by preferencesDataStore(name = "lumen_prefs")

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
)

class LumenPreferences(private val context: Context) {

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
    }

    val settings: Flow<LumenSettings> = context.lumenDataStore.data.map { prefs ->
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
        )
    }

    suspend fun setScreensaverMinutes(minutes: Int) {
        context.lumenDataStore.edit { it[Keys.screensaverMinutes] = minutes.coerceIn(0, 120) }
    }

    suspend fun setShowSeconds(show: Boolean) {
        context.lumenDataStore.edit { it[Keys.showSeconds] = show }
    }

    suspend fun setBackgroundSeed(seed: Int) {
        context.lumenDataStore.edit { it[Keys.backgroundSeed] = seed.coerceAtLeast(1) }
    }

    suspend fun setWebApps(apps: List<WebApp>) {
        context.lumenDataStore.edit { it[Keys.webApps] = encodeWebApps(apps) }
    }

    suspend fun setConnections(connections: List<Connection>) {
        context.lumenDataStore.edit { it[Keys.connections] = encodeConnections(connections) }
    }

    suspend fun setCharacterId(id: String) {
        context.lumenDataStore.edit { it[Keys.characterId] = id }
    }

    suspend fun setSkills(skills: SkillsState) {
        context.lumenDataStore.edit { it[Keys.skills] = encodeSkills(skills.enabledIds, skills.custom) }
    }

    suspend fun setBuildsUrl(url: String) {
        context.lumenDataStore.edit {
            it[Keys.buildsUrl] = url.trim().ifBlank { BuildsRegistry.DEFAULT_URL }
        }
    }

    suspend fun setShowBuilds(show: Boolean) {
        context.lumenDataStore.edit { it[Keys.showBuilds] = show }
    }

    suspend fun setMemory(facts: List<MemoryFact>) {
        context.lumenDataStore.edit { it[Keys.memory] = Memory.encode(facts) }
    }

    suspend fun setRememberChat(remember: Boolean) {
        context.lumenDataStore.edit { it[Keys.rememberChat] = remember }
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
    }

    /** История диалога персонажа — читается сразу из хранилища. */
    suspend fun chatFor(characterId: String): List<ChatMessage> =
        settings.first().chats[characterId].orEmpty()

    suspend fun setWeatherCache(raw: String?) {
        context.lumenDataStore.edit { prefs ->
            if (raw.isNullOrBlank()) prefs.remove(Keys.weatherCache) else prefs[Keys.weatherCache] = raw
        }
    }
}