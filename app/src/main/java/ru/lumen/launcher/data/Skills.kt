package ru.lumen.launcher.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Навык персонажа. DEVICE — реальное действие на телефоне,
 * PROMPT — инструкция, которая подмешивается в системную подсказку.
 */
enum class SkillKind { DEVICE, PROMPT }

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val kind: SkillKind,
    val prompt: String = "",
    val custom: Boolean = false,
)

/** Навыки, доступные «отсюда» — из окружения OpenChamber/opencode. */
val BuiltInSkills: List<Skill> = listOf(
    Skill(
        id = "android-device",
        name = "Телефон",
        description = "Будильник, уведомление, буфер обмена, запуск приложения, ссылки",
        kind = SkillKind.DEVICE,
        prompt = "Умеет ставить будильник, показывать уведомления, копировать текст в буфер, " +
            "открывать приложения и ссылки на телефоне.",
    ),
    Skill(
        id = "opencode",
        name = "OpenCode",
        description = "Справка по OpenCode: агенты, команды, навыки, MCP, провайдеры",
        kind = SkillKind.PROMPT,
        prompt = "Отвечай как знаток OpenCode: агенты, команды, навыки, разрешения, MCP-серверы, " +
            "провайдеры и модели, CLI, TUI, десктоп и веб-приложение.",
    ),
    Skill(
        id = "report",
        name = "Диагностика",
        description = "Помогает собрать отчёт о проблеме: шаги, ожидание, факт, окружение",
        kind = SkillKind.PROMPT,
        prompt = "Помогай описывать проблемы структурно: что делал, что ожидал, что получил, " +
            "окружение и версии. Задавай по одному уточняющему вопросу.",
    ),
)

fun encodeSkills(enabledIds: Set<String>, custom: List<Skill>): String {
    val json = JSONObject()
    json.put("enabled", JSONArray(enabledIds.toList()))
    val customArray = JSONArray()
    custom.forEach { skill ->
        customArray.put(
            JSONObject()
                .put("id", skill.id)
                .put("name", skill.name)
                .put("description", skill.description)
                .put("prompt", skill.prompt)
        )
    }
    json.put("custom", customArray)
    return json.toString()
}

data class SkillsState(
    val enabledIds: Set<String> = setOf("android-device"),
    val custom: List<Skill> = emptyList(),
) {
    val all: List<Skill> get() = BuiltInSkills + custom
    val enabled: List<Skill> get() = all.filter { it.id in enabledIds }
}

fun decodeSkills(raw: String?): SkillsState {
    if (raw.isNullOrBlank()) return SkillsState()
    return runCatching {
        val json = JSONObject(raw)
        val enabledArray = json.optJSONArray("enabled") ?: JSONArray()
        val enabled = buildSet {
            for (index in 0 until enabledArray.length()) add(enabledArray.optString(index))
        }
        val customArray = json.optJSONArray("custom") ?: JSONArray()
        val custom = buildList {
            for (index in 0 until customArray.length()) {
                val item = customArray.optJSONObject(index) ?: continue
                val id = item.optString("id").ifBlank { "custom-$index" }
                val name = item.optString("name").trim()
                if (name.isEmpty()) continue
                add(
                    Skill(
                        id = id,
                        name = name,
                        description = item.optString("description").trim().ifBlank { "свой навык" },
                        kind = SkillKind.PROMPT,
                        prompt = item.optString("prompt").trim(),
                        custom = true,
                    )
                )
            }
        }
        SkillsState(enabledIds = enabled, custom = custom)
    }.getOrDefault(SkillsState())
}