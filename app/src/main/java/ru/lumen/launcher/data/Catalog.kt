package ru.lumen.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Приложение, найденное в подключённом сервисе. */
data class CatalogApp(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val connectionId: String,
    val connectionLabel: String,
)

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val apps: List<CatalogApp>) : CatalogState
    data class Error(val message: String) : CatalogState
}

object CatalogLoader {

    suspend fun load(connection: Connection): CatalogState = withContext(Dispatchers.IO) {
        runCatching {
            when (connection.kind) {
                ServiceKind.TEABLE -> loadTeable(connection)
                ServiceKind.GITHUB -> loadGitHub(connection)
                ServiceKind.OPENCHAMBER -> loadOpenChamber(connection)
                ServiceKind.AI -> CatalogState.Error("Подключение ИИ не даёт список приложений")
            }
        }.getOrElse { error ->
            CatalogState.Error(error.message ?: "Сервис недоступен")
        }
    }

    private fun base(connection: Connection) = normalizeBaseUrl(connection.baseUrl)

    // ── Teable: приложения App Builder ─────────────────────────────

    private fun loadTeable(connection: Connection): CatalogState {
        if (connection.token.isBlank()) {
            return CatalogState.Error("Нужен токен Teable")
        }
        val headers = Http.bearer(connection.token)
        val basesRaw = Http.getText("${base(connection)}/api/base/access/all", headers)
            ?: return CatalogState.Error("Teable не ответил — проверьте адрес и токен")

        val bases = runCatching { JSONArray(basesRaw) }.getOrNull()
            ?: return CatalogState.Error("Неожиданный ответ Teable")

        val apps = mutableListOf<CatalogApp>()
        for (index in 0 until bases.length()) {
            val baseId = bases.optJSONObject(index)?.optString("id").orEmpty()
            if (baseId.isBlank()) continue
            val listRaw = Http.getText(
                "${base(connection)}/api/base/$baseId/app/list?skip=0&take=100&order=desc",
                headers,
            ) ?: continue
            val list = runCatching { JSONObject(listRaw).optJSONArray("list") }.getOrNull() ?: continue
            for (appIndex in 0 until list.length()) {
                val app = list.optJSONObject(appIndex) ?: continue
                if (!app.optBoolean("isPublished", false)) continue
                val url = firstUrl(
                    app.optString("accessUrl"),
                    app.optString("systemDomainUrl"),
                    if (app.optBoolean("customDomainEnabled", false)) app.optString("customDomain") else "",
                    app.optJSONObject("metaData")?.optString("publicUrl").orEmpty(),
                    app.optJSONObject("metaData")?.optString("previewUrl").orEmpty(),
                ) ?: continue
                val id = app.optString("id").ifBlank { url }
                apps += CatalogApp(
                    id = id,
                    name = app.optString("name").ifBlank { id },
                    description = app.optString("description").trim(),
                    url = url,
                    connectionId = connection.id,
                    connectionLabel = connection.displayLabel,
                )
            }
        }
        return CatalogState.Ready(apps.sortedBy { it.name.lowercase() })
    }

    // ── GitHub: репозитории с GitHub Pages ─────────────────────────

    private fun loadGitHub(connection: Connection): CatalogState {
        val login = connection.username.trim()
        val path = if (login.isBlank() || login == "me") "/user/repos" else "/users/$login/repos"
        val url = "${base(connection)}$path?per_page=100&sort=updated"
        val raw = Http.getText(url, Http.bearer(connection.token))
            ?: return CatalogState.Error(
                if (login.isBlank()) "Укажите логин GitHub (или токен для /user/repos)"
                else "GitHub не ответил — проверьте логин или лимит запросов"
            )

        val repos = runCatching { JSONArray(raw) }.getOrNull()
            ?: return CatalogState.Error("Неожиданный ответ GitHub")

        val apps = mutableListOf<CatalogApp>()
        for (index in 0 until repos.length()) {
            val repo = repos.optJSONObject(index) ?: continue
            val hasPages = repo.optBoolean("has_pages", false)
            val homepage = repo.optString("homepage").trim()
            if (!hasPages && !homepage.startsWith("http")) continue
            val owner = repo.optJSONObject("owner")?.optString("login").orEmpty().ifBlank { login }
            val name = repo.optString("name")
            val target = when {
                homepage.startsWith("http") -> homepage
                else -> "https://$owner.github.io/$name/"
            }
            apps += CatalogApp(
                id = repo.optString("full_name").ifBlank { name },
                name = name,
                description = repo.optString("description").trim(),
                url = target,
                connectionId = connection.id,
                connectionLabel = connection.displayLabel,
            )
        }
        return CatalogState.Ready(apps.sortedBy { it.name.lowercase() })
    }

    // ── OpenChamber/OpenCode: рабочие пространства и сессии ────────

    private fun loadOpenChamber(connection: Connection): CatalogState {
        if (connection.baseUrl.isBlank()) {
            return CatalogState.Error("Укажите адрес сервера OpenChamber")
        }
        val headers = Http.basic(connection.username, connection.password)
        val root = base(connection)

        val projectsRaw = Http.getText("$root/project", headers)
        val sessionsRaw = Http.getText("$root/session", headers)
        if (projectsRaw == null && sessionsRaw == null) {
            return CatalogState.Error("Сервер не ответил на /project и /session")
        }

        val apps = mutableListOf<CatalogApp>()
        projectsRaw?.let { raw ->
            val projects = runCatching { JSONArray(raw) }.getOrNull() ?: JSONArray()
            for (index in 0 until projects.length()) {
                val project = projects.optJSONObject(index) ?: continue
                val id = project.optString("id").ifBlank { project.optString("worktree") }
                if (id.isBlank()) continue
                val worktree = project.optString("worktree").ifBlank { project.optString("path") }
                apps += CatalogApp(
                    id = "project-$id",
                    name = worktree.substringAfterLast('/').ifBlank { id },
                    description = worktree,
                    url = root,
                    connectionId = connection.id,
                    connectionLabel = connection.displayLabel,
                )
            }
        }
        sessionsRaw?.let { raw ->
            val sessions = runCatching { JSONArray(raw) }.getOrNull() ?: JSONArray()
            for (index in 0 until sessions.length()) {
                val session = sessions.optJSONObject(index) ?: continue
                val id = session.optString("id")
                if (id.isBlank()) continue
                val title = session.optString("title").ifBlank { id }
                val directory = session.optJSONObject("location")?.optString("directory").orEmpty()
                apps += CatalogApp(
                    id = "session-$id",
                    name = title,
                    description = directory.ifBlank { "сессия OpenChamber" },
                    url = root,
                    connectionId = connection.id,
                    connectionLabel = connection.displayLabel,
                )
            }
        }
        return CatalogState.Ready(apps.distinctBy { it.id })
    }

    private fun firstUrl(vararg candidates: String): String? =
        candidates.map { it.trim() }.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
}