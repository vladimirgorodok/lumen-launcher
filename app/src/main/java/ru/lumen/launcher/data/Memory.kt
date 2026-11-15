package ru.lumen.launcher.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Факт, который персонаж помнит о собеседнике: имя, день рождения, город,
 * привычки. Хранится в DataStore и подмешивается в системную подсказку.
 */
data class MemoryFact(
    val id: String,
    val text: String,
    val createdAt: Long,
    val source: String = "user",
)

/**
 * Память персонажа. Работает без модели: и «запомни …», и обычное
 * «меня зовут …» сохраняются на телефоне, а на прямой вопрос о том,
 * что помнится, персонаж отвечает сам.
 */
object Memory {

    private const val MAX_FACTS = 200
    private const val MAX_LENGTH = 400
    private const val MAX_SENTENCE = 160

    private val learnTriggers = listOf(
        "запомни", "запомните", "запиши", "не забудь", "remember", "сохрани в память",
    )

    private val forgetTriggers = listOf(
        "забудь", "забудьте", "удали из памяти", "убери из памяти", "forget",
    )

    private val implicitRules = listOf(
        Regex("меня зовут\\s+\\S+"),
        Regex("мо[её]\\s+имя\\s+\\S+"),
        Regex("мо[йя]\\s+день рождения"),
        Regex("я родил(ся|ась)"),
        Regex("я живу"),
        Regex("я из\\s+\\S+"),
        Regex("мне\\s+\\d{1,3}\\s*(год|года|лет)"),
        Regex("я работаю"),
        Regex("мо[йя]\\s+телефон"),
        Regex("мо[йя]\\s+почта"),
        Regex("у меня есть\\s+\\S+"),
        Regex("я люблю\\s+\\S+"),
        Regex("мою жену зовут"),
        Regex("моего мужа зовут"),
        Regex("моих детей|мои дети"),
    )

    private val questionStarts = listOf(
        "как ", "что ", "кто ", "где ", "когда ", "зачем ", "почему ", "сколько ",
    )

    private val subjects = listOf(
        "зовут" to "имя",
        "имя" to "имя",
        "день рождения" to "др",
        "родил" to "др",
        "живу" to "город",
        "город" to "город",
        "телефон" to "телефон",
        "почта" to "почта",
        "email" to "почта",
        "работаю" to "работа",
        "работа" to "работа",
    )

    // ---------- хранение ----------

    fun encode(facts: List<MemoryFact>): String {
        val array = JSONArray()
        facts.forEach { fact ->
            array.put(
                JSONObject()
                    .put("id", fact.id)
                    .put("text", fact.text)
                    .put("created", fact.createdAt)
                    .put("source", fact.source)
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<MemoryFact> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val text = item.optString("text").trim()
                    if (text.isEmpty()) continue
                    add(
                        MemoryFact(
                            id = item.optString("id").ifBlank { "fact-$index" },
                            text = text,
                            createdAt = item.optLong("created", 0L),
                            source = item.optString("source").ifBlank { "user" },
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    // ---------- запись ----------

    /** Добавляет факт, заменяя прежний факт о том же (имя, день рождения, город). */
    fun learn(
        facts: List<MemoryFact>,
        text: String,
        source: String = "user",
        now: Long = System.currentTimeMillis(),
    ): List<MemoryFact> {
        val clean = cleanUp(text)
        if (clean.length < 2) return facts
        val normalized = normalize(clean)
        val subject = subjectOf(normalized)
        val kept = facts.filterNot { fact ->
            val other = normalize(fact.text)
            other == normalized || (subject != null && subjectOf(other) == subject)
        }
        val fact = MemoryFact(
            id = UUID.randomUUID().toString().take(8),
            text = clean,
            createdAt = now,
            source = source,
        )
        return (kept + fact).takeLast(MAX_FACTS)
    }

    /** Убирает факты, подходящие под запрос: «имя», «день рождения» или кусок текста. */
    fun forget(facts: List<MemoryFact>, query: String): List<MemoryFact> {
        val q = normalize(query)
        if (q.isBlank()) return facts
        val subject = subjectOf(q)
        return facts.filterNot { fact ->
            val other = normalize(fact.text)
            other.contains(q) || (subject != null && subjectOf(other) == subject)
        }
    }

    fun remove(facts: List<MemoryFact>, id: String): List<MemoryFact> =
        facts.filterNot { it.id == id }

    // ---------- разбор реплик ----------

    /** «Запомни, что меня зовут Владимир» → «меня зовут Владимир». */
    fun extract(text: String): String? {
        val lower = text.lowercase()
        val trigger = learnTriggers.firstOrNull { lower.contains(it) } ?: return null
        val rest = text.substring(lower.indexOf(trigger) + trigger.length)
        return cleanUp(rest).takeIf { it.length >= 2 }
    }

    /** «Забудь моё имя» → «моё имя». */
    fun extractForget(text: String): String? {
        val lower = text.lowercase()
        val trigger = forgetTriggers.firstOrNull { lower.contains(it) } ?: return null
        val rest = text.substring(lower.indexOf(trigger) + trigger.length)
        return cleanUp(rest).takeIf { it.length >= 2 }
    }

    /**
     * Обычное сообщение о себе: «Меня зовут Владимир», «я живу в Москве»,
     * «мой день рождения 04.11.1978». Вопросы не записываются.
     */
    fun extractImplicit(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.endsWith("?")) return null
        val sentences = trimmed.split(Regex("(?<=[.!?])\\s+|\\n"))
        return sentences
            .filter { it.length in 3..MAX_SENTENCE }
            .firstOrNull { sentence ->
                val normalized = normalize(sentence)
                questionStarts.none { normalized.startsWith(it) } &&
                    implicitRules.any { it.containsMatchIn(normalized) }
            }
            ?.let { cleanUp(it) }
            ?.takeIf { it.length >= 3 }
    }

    // ---------- ответы из памяти ----------

    private val nameRegex = Regex(
        "(?iu)(?:меня\\s+зовут|мо[её]\\s+имя|имя)\\s*[:\\-—]?\\s*([\\p{L}][\\p{L}\\-]+)"
    )
    private val dateRegex = Regex("\\d{1,2}[.\\-/]\\d{1,2}(?:[.\\-/]\\d{2,4})?")
    private val birthdayRegex = Regex("(?iu)день\\s+рождения\\s*[:\\-—]?\\s*(.+)")
    private val cityRegex = Regex("(?iu)(?:живу в|живу|город)\\s+([\\p{L}][\\p{L}\\-]+)")

    fun name(facts: List<MemoryFact>): String? = facts.asSequence()
        .mapNotNull { nameRegex.find(it.text)?.groupValues?.getOrNull(1)?.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.take(40)
        ?.replaceFirstChar { it.uppercase() }

    fun birthday(facts: List<MemoryFact>): String? = facts.asSequence()
        .filter {
            val text = normalize(it.text)
            text.contains("рожден") || text.contains("день рождения")
        }
        .mapNotNull { fact ->
            dateRegex.find(fact.text)?.value
                ?: birthdayRegex.find(fact.text)?.groupValues?.getOrNull(1)?.trim()
        }
        .firstOrNull { !it.isNullOrBlank() }

    fun city(facts: List<MemoryFact>): String? = facts.asSequence()
        .mapNotNull { cityRegex.find(it.text)?.groupValues?.getOrNull(1)?.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.replaceFirstChar { it.uppercase() }

    /** Ответ на прямой вопрос о том, что персонаж помнит. null — пусть отвечает модель. */
    fun localAnswer(question: String, facts: List<MemoryFact>): String? {
        if (facts.isEmpty()) return null
        val q = normalize(question)
        return when {
            nameQuestion(q) -> name(facts)?.let { "Тебя зовут $it." }
            birthdayQuestion(q) -> birthday(facts)?.let { "Твой день рождения — $it." }
            cityQuestion(q) -> city(facts)?.let { "Ты живёшь в $it." }
            listQuestion(q) -> listOfFacts(facts)
            else -> null
        }
    }

    fun listOfFacts(facts: List<MemoryFact>): String = buildString {
        append("Помню: ")
        append(facts.takeLast(12).joinToString("; ") { it.text })
        append(".")
    }

    private fun nameQuestion(q: String): Boolean =
        q.contains("как меня зовут") || q.contains("как моё имя") || q.contains("как мое имя") ||
            q.contains("моё имя") || q.contains("мое имя") || q.contains("как меня звать") ||
            q.contains("помнишь моё имя") || q.contains("помнишь мое имя")

    private fun birthdayQuestion(q: String): Boolean =
        q.contains("день рождения") || q.contains("когда я родил")

    private fun cityQuestion(q: String): Boolean =
        q.contains("где я живу") || q.contains("в каком городе") || q.contains("мой город")

    private fun listQuestion(q: String): Boolean =
        q.contains("что ты помнишь") || q.contains("что помнишь") ||
            q.contains("что ты знаешь обо мне") || q.contains("что знаешь обо мне")

    // ---------- подсказка для модели ----------

    fun promptBlock(facts: List<MemoryFact>): String = buildString {
        if (facts.isEmpty()) {
            append(
                "\n\nТы пока ничего не знаешь о собеседнике. Если он расскажет о себе " +
                    "(имя, день рождения, город, привычки) — сохрани это действием remember."
            )
        } else {
            append("\n\nТы помнишь о собеседнике — опирайся на это и не переспрашивай:\n")
            facts.takeLast(60).forEach { append("- ${it.text}\n") }
        }
        append(
            "\nЕсли собеседник просит что-то запомнить, ответь ровно одной строкой JSON без пояснений: " +
                "{\"action\":\"remember\",\"text\":\"что запомнить\"}\n" +
                "Если просит забыть: {\"action\":\"forget\",\"text\":\"что забыть\"}\n" +
                "Не выдумывай факты о собеседнике: помни только перечисленное выше."
        )
    }

    // ---------- служебное ----------

    private fun cleanUp(text: String): String {
        var value = text.replace(Regex("\\s+"), " ").trim()
        val edge = "\"'«»:;,.—-– "
        value = value.trim(*edge.toCharArray())
        val lead = Regex("(?iu)^(что|это|то, что|о том, что|про то, что|пожалуйста)\\s+")
        var previous: String
        do {
            previous = value
            value = value.replace(lead, "").trim(*edge.toCharArray())
        } while (value != previous)
        return value.take(MAX_LENGTH)
    }

    private fun normalize(text: String): String = text
        .lowercase()
        .replace('ё', 'е')
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    private fun subjectOf(normalized: String): String? =
        subjects.firstOrNull { normalized.contains(it.first) }?.second
}
