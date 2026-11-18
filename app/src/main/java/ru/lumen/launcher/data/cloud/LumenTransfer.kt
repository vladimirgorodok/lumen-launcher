package ru.lumen.launcher.data.cloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import ru.lumen.launcher.data.ChatMessage
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.LumenSettings
import ru.lumen.launcher.data.Memory
import ru.lumen.launcher.data.MemoryFact
import ru.lumen.launcher.data.SeedWebApps
import ru.lumen.launcher.data.SkillsState
import ru.lumen.launcher.data.WebApp
import ru.lumen.launcher.data.decodeChats
import ru.lumen.launcher.data.decodeConnections
import ru.lumen.launcher.data.decodeSkills
import ru.lumen.launcher.data.decodeWebApps
import ru.lumen.launcher.data.encodeChats
import ru.lumen.launcher.data.encodeConnections
import ru.lumen.launcher.data.encodeSkills
import ru.lumen.launcher.data.encodeWebApps

/**
 * Состояние приложения, нужное обмену.
 *
 * Реализация — [LumenPreferences]; отдельный интерфейс позволяет проверять
 * сборку и слияние данных обычными JVM-тестами, без Android.
 */
interface LumenStateStore {
    val settings: Flow<LumenSettings>

    /** Сигнал «данные изменились»: из него взводится флаг невыгруженных правок. */
    val changes: SharedFlow<Unit>

    suspend fun setScreensaverMinutes(minutes: Int)
    suspend fun setShowSeconds(show: Boolean)
    suspend fun setBackgroundSeed(seed: Int)
    suspend fun setWebApps(apps: List<WebApp>)
    suspend fun setConnections(connections: List<Connection>)
    suspend fun setCharacterId(id: String)
    suspend fun setSkills(skills: SkillsState)
    suspend fun setBuildsUrl(url: String)
    suspend fun setShowBuilds(show: Boolean)
    suspend fun setMemory(facts: List<MemoryFact>)
    suspend fun setRememberChat(remember: Boolean)
    suspend fun setChats(chats: Map<String, List<ChatMessage>>)
}

/**
 * Сборка и применение данных «Люмена» для облачной копии.
 *
 * Модуль обмена не пишет в хранилище сам — он отдаёт и принимает текст,
 * а этот класс превращает состояние DataStore в JSON и обратно.
 *
 * В копию попадают настройки, веб-приложения, подключения, память персонажа
 * и история диалогов. Логин, пароль приложения и служебные флаги обмена
 * остаются только на устройстве.
 */
class LumenTransfer(private val store: LumenStateStore) {

    companion object {
        /** Метка приложения в файле копии. */
        const val APP_TAG = "lumen"

        /** Версия формата копии. */
        const val FORMAT_VERSION = 1

        /** Поля состояния, которые уезжают в облако. */
        val SYNCED_FIELDS = listOf(
            "screensaver_minutes",
            "show_seconds",
            "background_seed",
            "character_id",
            "builds_url",
            "show_builds",
            "remember_chat",
            "skills",
            "web_apps",
            "connections",
            "memory_facts",
            "chat_history",
        )
    }

    /** Текст облачной копии: читаемый JSON со всеми данными приложения. */
    suspend fun snapshotJson(): String {
        val state = store.settings.first()
        val values = JSONObject()
        values.put("screensaver_minutes", state.screensaverMinutes)
        values.put("show_seconds", state.showSeconds)
        values.put("background_seed", state.backgroundSeed)
        values.put("character_id", state.characterId)
        values.put("builds_url", state.buildsUrl)
        values.put("show_builds", state.showBuilds)
        values.put("remember_chat", state.rememberChat)
        values.put("skills", JSONObject(encodeSkills(state.skills.enabledIds, state.skills.custom)))
        values.put("web_apps", JSONArray(encodeWebApps(state.webApps)))
        values.put("connections", JSONArray(encodeConnections(connectionsForCloud(state))))
        values.put("memory_facts", JSONArray(Memory.encode(state.facts)))
        values.put("chat_history", JSONObject(encodeChats(state.chats)))

        return JSONObject()
            .put("app", APP_TAG)
            .put("version", FORMAT_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("values", values)
            .toString(2)
    }

    /**
     * Применяет текст копии к состоянию приложения.
     *
     * [replaceAll] = true — полная замена, иначе слияние: совпадающие записи
     * обновляются, свои остаются. Возвращает число записей в применённой копии.
     */
    suspend fun importFromText(raw: String, replaceAll: Boolean): Int {
        val root = try {
            JSONObject(raw)
        } catch (_: Throwable) {
            throw CloudException("Файл копии не разобран: это не JSON")
        }
        val values = root.optJSONObject("values")
            ?: throw CloudException("В файле нет данных приложения «Люмен»")

        val local = store.settings.first()

        values.optInt("screensaver_minutes", local.screensaverMinutes)
            .let { store.setScreensaverMinutes(it) }
        values.optBoolean("show_seconds", local.showSeconds)
            .let { store.setShowSeconds(it) }
        values.optInt("background_seed", local.backgroundSeed)
            .let { store.setBackgroundSeed(it) }
        values.optBoolean("show_builds", local.showBuilds)
            .let { store.setShowBuilds(it) }
        values.optBoolean("remember_chat", local.rememberChat)
            .let { store.setRememberChat(it) }

        values.optString("builds_url").trim()
            .takeIf { it.isNotEmpty() }
            ?.let { store.setBuildsUrl(it) }

        values.optString("character_id").trim()
            .takeIf { it.isNotEmpty() }
            ?.let { store.setCharacterId(it) }

        values.optJSONObject("skills")
            ?.let { store.setSkills(decodeSkills(it.toString())) }

        val apps = decodeWebApps(values.optJSONArray("web_apps")?.toString())
        store.setWebApps(if (replaceAll) apps else mergeWebApps(local.webApps, apps))

        val connections = decodeConnections(values.optJSONArray("connections")?.toString())
        store.setConnections(
            if (replaceAll) connections else mergeConnections(local.connections, connections)
        )

        val facts = Memory.decode(values.optJSONArray("memory_facts")?.toString())
        store.setMemory(if (replaceAll) facts else mergeFacts(local.facts, facts))

        val chats = decodeChats(values.optJSONObject("chat_history")?.toString())
        store.setChats(if (replaceAll) chats else mergeChats(local.chats, chats))

        return countInText(raw)
    }

    /** Сколько записей пользователя в текущем состоянии приложения. */
    suspend fun countLocal(): Int = countOf(store.settings.first())

    /** Сколько записей пользователя в тексте копии. */
    fun countInText(raw: String): Int {
        val values = runCatching { JSONObject(raw).optJSONObject("values") }.getOrNull() ?: return 0
        return countParts(
            apps = decodeWebApps(values.optJSONArray("web_apps")?.toString()),
            connections = decodeConnections(values.optJSONArray("connections")?.toString()),
            facts = Memory.decode(values.optJSONArray("memory_facts")?.toString()),
            chats = decodeChats(values.optJSONObject("chat_history")?.toString()),
            customSkills = decodeSkills(values.optJSONObject("skills")?.toString()).custom.size,
        )
    }

    /** Пустое состояние: выгружать нечего, облачную копию затирать нельзя. */
    suspend fun isEmpty(): Boolean = countLocal() == 0

    // ------------------------------------------------------------------
    // Слияние
    // ------------------------------------------------------------------

    private fun mergeWebApps(local: List<WebApp>, cloud: List<WebApp>): List<WebApp> {
        val byUrl = LinkedHashMap<String, WebApp>()
        local.forEach { byUrl[it.url] = it }
        cloud.forEach { byUrl[it.url] = it }
        return byUrl.values.toList()
    }

    /**
     * Слияние подключений по id. Токены и пароли из облака не затирают
     * свои: если в копии их нет (они не выгружались), остаются локальные.
     */
    private fun mergeConnections(local: List<Connection>, cloud: List<Connection>): List<Connection> {
        val byId = LinkedHashMap<String, Connection>()
        local.forEach { byId[it.id] = it }
        cloud.forEach { remote ->
            val mine = byId[remote.id]
            byId[remote.id] = if (mine == null) {
                remote
            } else {
                remote.copy(
                    token = remote.token.ifBlank { mine.token },
                    username = remote.username.ifBlank { mine.username },
                    password = remote.password.ifBlank { mine.password },
                )
            }
        }
        return byId.values.toList()
    }

    private fun mergeFacts(local: List<MemoryFact>, cloud: List<MemoryFact>): List<MemoryFact> =
        (local + cloud).distinctBy { it.id }

    private fun mergeChats(
        local: Map<String, List<ChatMessage>>,
        cloud: Map<String, List<ChatMessage>>,
    ): Map<String, List<ChatMessage>> {
        val merged = LinkedHashMap(local)
        cloud.forEach { (characterId, messages) ->
            if (messages.isNotEmpty()) merged[characterId] = messages
        }
        return merged
    }

    // ------------------------------------------------------------------
    // Подсчёт записей
    // ------------------------------------------------------------------

    private fun countOf(state: LumenSettings): Int = countParts(
        apps = state.webApps,
        connections = state.connections,
        facts = state.facts,
        chats = state.chats,
        customSkills = state.skills.custom.size,
    )

    private fun countParts(
        apps: List<WebApp>,
        connections: List<Connection>,
        facts: List<MemoryFact>,
        chats: Map<String, List<ChatMessage>>,
        customSkills: Int,
    ): Int =
        apps.count { app -> SeedWebApps.none { it.url == app.url } } +
            connections.size +
            facts.size +
            chats.values.sumOf { it.size } +
            customSkills

    /** Подключения для облака: без токенов и паролей, если это не разрешено явно. */
    private fun connectionsForCloud(state: LumenSettings): List<Connection> =
        if (state.cloudSendSecrets) {
            state.connections
        } else {
            state.connections.map { it.copy(token = "", username = "", password = "") }
        }
}
