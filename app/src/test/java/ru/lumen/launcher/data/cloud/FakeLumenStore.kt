package ru.lumen.launcher.data.cloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import ru.lumen.launcher.data.ChatMessage
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.LumenSettings
import ru.lumen.launcher.data.MemoryFact
import ru.lumen.launcher.data.SkillsState
import ru.lumen.launcher.data.WebApp

/**
 * Состояние в памяти: то же, что DataStore, но без Android.
 *
 * Нужно, чтобы сборку копии, слияние и снимки можно было проверять
 * обычными JVM-тестами.
 */
class FakeLumenStore(initial: LumenSettings = LumenSettings()) : LumenStateStore {

    private val state = MutableStateFlow(initial)
    private val signal = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    override val settings: Flow<LumenSettings> = state

    override val changes: SharedFlow<Unit> get() = signal

    val current: LumenSettings get() = state.value

    /** Сколько раз состояние менялось — как сигнал `changes` у настоящего хранилища. */
    var writeCount: Int = 0
        private set

    private fun update(block: (LumenSettings) -> LumenSettings) {
        state.value = block(state.value)
        writeCount += 1
        signal.tryEmit(Unit)
    }

    override suspend fun setScreensaverMinutes(minutes: Int) {
        update { it.copy(screensaverMinutes = minutes) }
    }

    override suspend fun setShowSeconds(show: Boolean) {
        update { it.copy(showSeconds = show) }
    }

    override suspend fun setBackgroundSeed(seed: Int) {
        update { it.copy(backgroundSeed = seed) }
    }

    override suspend fun setWebApps(apps: List<WebApp>) {
        update { it.copy(webApps = apps) }
    }

    override suspend fun setConnections(connections: List<Connection>) {
        update { it.copy(connections = connections) }
    }

    override suspend fun setCharacterId(id: String) {
        update { it.copy(characterId = id) }
    }

    override suspend fun setSkills(skills: SkillsState) {
        update { it.copy(skills = skills) }
    }

    override suspend fun setBuildsUrl(url: String) {
        update { it.copy(buildsUrl = url) }
    }

    override suspend fun setShowBuilds(show: Boolean) {
        update { it.copy(showBuilds = show) }
    }

    override suspend fun setMemory(facts: List<MemoryFact>) {
        update { it.copy(facts = facts) }
    }

    override suspend fun setRememberChat(remember: Boolean) {
        update { it.copy(rememberChat = remember) }
    }

    override suspend fun setChats(chats: Map<String, List<ChatMessage>>) {
        update { it.copy(chats = chats) }
    }
}
