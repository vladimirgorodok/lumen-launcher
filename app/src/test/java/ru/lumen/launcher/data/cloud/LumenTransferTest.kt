package ru.lumen.launcher.data.cloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.lumen.launcher.data.ChatMessage
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.LumenSettings
import ru.lumen.launcher.data.MemoryFact
import ru.lumen.launcher.data.SeedWebApps
import ru.lumen.launcher.data.ServiceKind
import ru.lumen.launcher.data.Skill
import ru.lumen.launcher.data.SkillKind
import ru.lumen.launcher.data.SkillsState
import ru.lumen.launcher.data.WebApp

/** Сборка облачной копии, слияние и подсчёт записей. */
class LumenTransferTest {

    private fun github(token: String = "") = Connection(
        id = "conn-github",
        kind = ServiceKind.GITHUB,
        label = "GitHub",
        baseUrl = "https://api.github.com",
        token = token,
    )

    private fun fullStore() = FakeLumenStore(
        LumenSettings(
            screensaverMinutes = 7,
            showSeconds = false,
            backgroundSeed = 9,
            characterId = "zircon",
            buildsUrl = "https://example.org/builds.json",
            showBuilds = false,
            rememberChat = false,
            webApps = SeedWebApps + WebApp("Мой сайт", "https://example.org", 3),
            connections = listOf(github(token = "ghp_secret_token")),
            facts = listOf(MemoryFact(id = "fact-1", text = "Меня зовут Владимир", createdAt = 1L)),
            chats = mapOf(
                "lumen" to listOf(ChatMessage("user", "привет"), ChatMessage("assistant", "здравствуйте")),
            ),
            skills = SkillsState(
                enabledIds = setOf("opencode"),
                custom = listOf(
                    Skill(id = "skill-1", name = "Свой", description = "описание", kind = SkillKind.PROMPT, prompt = "инструкция", custom = true),
                ),
            ),
            yandexLogin = "login-777",
            yandexPassword = "secret-app-password-42",
            cloudFolder = "/Люмен/Копии",
            autoSync = true,
            cloudKeepCopies = 3,
            cloudSendSecrets = false,
        )
    )

    @Test
    fun copyHasNoAppPasswordOrLogin() = runBlocking {
        val text = LumenTransfer(fullStore()).snapshotJson()

        assertFalse("пароль приложения не должен уезжать в облако", text.contains("secret-app-password-42"))
        assertFalse("логин Яндекса не должен уезжать в облако", text.contains("login-777"))
        assertFalse("папка обмена — служебная настройка", text.contains("/Люмен/Копии"))
        assertTrue(text.contains(LumenTransfer.APP_TAG))
        assertTrue(text.contains("Меня зовут Владимир"))
    }

    @Test
    fun stateMovesToAnotherDevice() = runBlocking {
        val source = fullStore()
        val text = LumenTransfer(source).snapshotJson()

        val target = FakeLumenStore()
        val records = LumenTransfer(target).importFromText(text, replaceAll = true)
        val state = target.current

        assertTrue("в копии есть записи", records > 0)
        assertEquals(7, state.screensaverMinutes)
        assertEquals(false, state.showSeconds)
        assertEquals(9, state.backgroundSeed)
        assertEquals("zircon", state.characterId)
        assertEquals("https://example.org/builds.json", state.buildsUrl)
        assertEquals(false, state.showBuilds)
        assertEquals(false, state.rememberChat)
        assertTrue(state.webApps.any { it.url == "https://example.org" })
        assertEquals(1, state.facts.size)
        assertEquals("Меня зовут Владимир", state.facts.first().text)
        assertEquals(2, state.chats["lumen"]?.size)
        assertEquals(setOf("opencode"), state.skills.enabledIds)
        assertEquals(1, state.skills.custom.size)
    }

    @Test
    fun mergeUpdatesMatchingAndKeepsOwn() = runBlocking {
        val cloud = LumenTransfer(fullStore()).snapshotJson()

        val local = FakeLumenStore(
            LumenSettings(
                webApps = listOf(WebApp("Своё", "https://mine.example", 1)),
                connections = listOf(
                    github(token = "мой-токен"),
                    Connection(id = "conn-teable", kind = ServiceKind.TEABLE, label = "Teable", baseUrl = "https://app.teable.ai"),
                ),
                facts = listOf(MemoryFact(id = "fact-local", text = "Я живу в Москве", createdAt = 2L)),
            )
        )

        LumenTransfer(local).importFromText(cloud, replaceAll = false)
        val state = local.current

        assertTrue("своё веб-приложение осталось", state.webApps.any { it.url == "https://mine.example" })
        assertTrue("веб-приложение из копии добавилось", state.webApps.any { it.url == "https://example.org" })
        assertEquals("своё подключение осталось", 2, state.connections.size)
        assertTrue("свой факт остался", state.facts.any { it.id == "fact-local" })
        assertTrue("факт из копии добавился", state.facts.any { it.id == "fact-1" })
    }

    @Test
    fun mergeKeepsLocalTokenMissingInCopy() = runBlocking {
        val cloud = LumenTransfer(fullStore()).snapshotJson()
        val local = FakeLumenStore(LumenSettings(connections = listOf(github(token = "мой-токен"))))

        LumenTransfer(local).importFromText(cloud, replaceAll = false)

        val connection = local.current.connections.single { it.id == "conn-github" }
        assertEquals("мой-токен", connection.token)
    }

    @Test
    fun tokensGoOnlyWhenAllowed() = runBlocking {
        val without = LumenTransfer(fullStore()).snapshotJson()
        assertFalse("по умолчанию токенов в копии нет", without.contains("ghp_secret_token"))

        val withSecrets = FakeLumenStore(fullStore().current.copy(cloudSendSecrets = true))
        assertTrue("с разрешением токен уезжает", LumenTransfer(withSecrets).snapshotJson().contains("ghp_secret_token"))
    }

    @Test
    fun emptyStateIsNotPushed() = runBlocking {
        val empty = FakeLumenStore()
        assertEquals(0, LumenTransfer(empty).countLocal())
        assertTrue(LumenTransfer(empty).isEmpty())

        val withData = FakeLumenStore(LumenSettings(connections = listOf(github())))
        assertEquals(1, LumenTransfer(withData).countLocal())
        assertFalse(LumenTransfer(withData).isEmpty())
    }

    @Test
    fun recordsCountMatchesDevice() = runBlocking {
        val transfer = LumenTransfer(fullStore())
        val text = transfer.snapshotJson()

        // Записи: одно своё веб-приложение, подключение, факт, два сообщения, свой навык.
        assertEquals(6, transfer.countLocal())
        assertEquals(transfer.countLocal(), transfer.countInText(text))
    }

    @Test
    fun foreignFileGivesClearError() = runBlocking {
        val store = FakeLumenStore()
        val error = runCatching {
            LumenTransfer(store).importFromText("""{"hello":"world"}""", replaceAll = true)
        }.exceptionOrNull()

        assertTrue(error is CloudException)
        assertTrue(error?.message.orEmpty().contains("Люмен"))
    }
}
