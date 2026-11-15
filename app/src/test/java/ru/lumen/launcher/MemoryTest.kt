package ru.lumen.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.lumen.launcher.data.Memory
import ru.lumen.launcher.data.MemoryFact

class MemoryTest {

    private fun fact(text: String) = MemoryFact(id = text.hashCode().toString(), text = text, createdAt = 0L)

    @Test
    fun explicitRequestIsStored() {
        assertEquals("меня зовут Владимир", Memory.extract("Запомни, что меня зовут Владимир"))
        assertEquals("я живу в Москве", Memory.extract("запомни: я живу в Москве"))
        assertNull(Memory.extract("Как меня зовут?"))
    }

    @Test
    fun storyAboutYourselfIsStored() {
        assertEquals("Меня зовут Владимир", Memory.extractImplicit("Меня зовут Владимир"))
        assertEquals("Меня зовут Владимир", Memory.extractImplicit("Привет! Меня зовут Владимир"))
        assertEquals("Я живу в Москве", Memory.extractImplicit("Я живу в Москве"))
    }

    @Test
    fun questionsAreNotStored() {
        assertNull(Memory.extractImplicit("Как меня зовут?"))
        assertNull(Memory.extractImplicit("Что ты помнишь обо мне?"))
        assertNull(Memory.extractImplicit("Открой ютуб"))
    }

    @Test
    fun newNameReplacesOldOne() {
        val facts = Memory.learn(emptyList(), "Меня зовут Владимир")
        val updated = Memory.learn(facts, "меня зовут Володя")
        assertEquals(1, updated.size)
        assertEquals("меня зовут Володя", updated.first().text)
    }

    @Test
    fun sameFactIsNotDuplicated() {
        val once = Memory.learn(emptyList(), "Я живу в Москве")
        val twice = Memory.learn(once, "я живу в Москве")
        assertEquals(1, twice.size)
    }

    @Test
    fun forgetRemovesByNameAndBySubject() {
        val facts = Memory.learn(
            Memory.learn(emptyList(), "Меня зовут Владимир"),
            "Я живу в Москве",
        )
        assertEquals(1, Memory.forget(facts, "моё имя").size)
        assertEquals(1, Memory.forget(facts, "Москве").size)
        assertEquals(2, Memory.forget(facts, "велосипед").size)
        assertEquals("моё имя", Memory.extractForget("Забудь моё имя"))
    }

    @Test
    fun answersQuestionsFromMemory() {
        val facts = listOf(
            fact("Меня зовут Владимир"),
            fact("Мой день рождения 04.11.1978"),
            fact("Я живу в Москве"),
        )
        assertEquals("Тебя зовут Владимир.", Memory.localAnswer("Как меня зовут?", facts))
        assertEquals("Твой день рождения — 04.11.1978.", Memory.localAnswer("Когда у меня день рождения?", facts))
        assertEquals("Ты живёшь в Москве.", Memory.localAnswer("Где я живу?", facts))
        assertTrue(Memory.localAnswer("Что ты помнишь обо мне?", facts)!!.startsWith("Помню: "))
        assertNull(Memory.localAnswer("Поставь будильник на 7", facts))
    }

    @Test
    fun promptCarriesFactsAndEmptyHint() {
        val block = Memory.promptBlock(listOf(fact("Меня зовут Владимир")))
        assertTrue(block.contains("- Меня зовут Владимир"))
        assertTrue(block.contains("remember"))
        assertTrue(Memory.promptBlock(emptyList()).contains("ничего не знаешь"))
    }
}
