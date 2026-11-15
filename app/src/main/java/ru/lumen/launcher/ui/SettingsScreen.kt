package ru.lumen.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.LumenSettings
import ru.lumen.launcher.data.ServiceKind
import ru.lumen.launcher.data.WebApp
import ru.lumen.launcher.data.normalizeUrl

@Composable
fun SettingsScreen(
    palette: LumenPalette,
    settings: LumenSettings,
    isDefaultLauncher: Boolean,
    onSaverMinutes: (Int) -> Unit,
    onShowSeconds: (Boolean) -> Unit,
    onRegenerateBackground: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenDreamSettings: () -> Unit,
    onAddWebApp: (String, String) -> Unit,
    onDeleteWebApp: (WebApp) -> Unit,
    onUpdateWebApp: (WebApp, WebApp) -> Unit,
    onSaveConnection: (Connection) -> Unit,
    onDeleteConnection: (Connection) -> Unit,
    onSelectCharacter: (String) -> Unit,
    onSaveSkills: (ru.lumen.launcher.data.SkillsState) -> Unit,
    onShowBuilds: (Boolean) -> Unit,
    onSaveBuildsUrl: (String) -> Unit,
    onRefreshBuilds: () -> Unit,
    onRememberChat: (Boolean) -> Unit,
    onAddFact: (String) -> Unit,
    onDeleteFact: (String) -> Unit,
    onClearMemory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var addingConnection by remember { mutableStateOf(false) }
    var editingConnection by remember { mutableStateOf<Connection?>(null) }
    var buildsUrl by remember(settings.buildsUrl) { mutableStateOf(settings.buildsUrl) }
    var memoryOpen by remember { mutableStateOf(false) }

    if (memoryOpen) {
        MemoryScreen(
            palette = palette,
            facts = settings.facts,
            rememberChat = settings.rememberChat,
            onAdd = onAddFact,
            onDelete = onDeleteFact,
            onClear = onClearMemory,
            onRememberChat = onRememberChat,
            onBack = { memoryOpen = false },
        )
        return
    }

    if (addingConnection || editingConnection != null) {
        ConnectionEditorScreen(
            palette = palette,
            initial = editingConnection,
            onSave = { connection ->
                onSaveConnection(connection)
                addingConnection = false
                editingConnection = null
            },
            onCancel = {
                addingConnection = false
                editingConnection = null
            },
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = palette.textPrimary)
            }
            Column {
                Text(
                    "Настройки",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Text(
                    "экран, фон и заставка",
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = palette.textSecondary,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsCard(palette, title = "Заставка", icon = Icons.Filled.Timer) {
                Text(
                    "Через сколько бездействия включать режим заставки",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 1, 2, 5, 10, 30).forEach { minutes ->
                        ChoicePill(
                            text = if (minutes == 0) "выкл" else "${minutes}м",
                            selected = settings.screensaverMinutes == minutes,
                            palette = palette,
                            onClick = { onSaverMinutes(minutes) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Секунды на главном", fontSize = 13.sp, color = palette.textPrimary)
                        Text(
                            "как в «Люмене» — третий блок времени",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = settings.showSeconds,
                        onCheckedChange = onShowSeconds,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accentText,
                        ),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedAction(
                    text = "Системная заставка Android",
                    subtitle = "DreamService · экран, когда телефон на зарядке",
                    palette = palette,
                    onClick = onOpenDreamSettings,
                )
            }

            SettingsCard(palette, title = "Фон", icon = Icons.Filled.AutoAwesome) {
                Text(
                    "Процедурный фон: цвет зависит от времени суток, сезона и погоды.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "вариант №${settings.backgroundSeed}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedAction(
                    text = "Сгенерировать другой вариант",
                    subtitle = "новые частицы и положение свечения",
                    palette = palette,
                    onClick = onRegenerateBackground,
                )
            }

            SettingsCard(palette, title = "Домашний экран", icon = Icons.Filled.NightlightRound) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                if (isDefaultLauncher) Color(0xFF10B981) else Coral,
                                RoundedCornerShape(4.dp),
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isDefaultLauncher) {
                            "«Люмен» выбран домашним экраном"
                        } else {
                            "«Люмен» пока не выбран домашним экраном"
                        },
                        fontSize = 12.sp,
                        color = palette.textPrimary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedAction(
                    text = if (isDefaultLauncher) "Сменить домашний экран" else "Выбрать «Люмен» домашним",
                    subtitle = "системный выбор Android, без тихой подмены",
                    palette = palette,
                    onClick = onOpenHomeSettings,
                )
            }

            SettingsCard(palette, title = "Персонаж", icon = Icons.Filled.AutoAwesome) {
                Text(
                    "Кто отвечает на вкладке ИИ: имя, характер и голос. Навыки добавляются кнопкой ниже.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                ru.lumen.launcher.data.Characters.forEach { character ->
                    val selected = settings.characterId == character.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) character.body.copy(alpha = 0.14f) else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (selected) character.body.copy(alpha = 0.45f) else palette.panelBorder,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelectCharacter(character.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlienCharacter(
                            character = character,
                            speaking = false,
                            listening = false,
                            modifier = Modifier.size(46.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(character.name, fontSize = 13.sp, color = palette.textPrimary)
                            Text(
                                character.tagline,
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Выбран",
                                tint = character.body,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(4.dp))
                var skillsOpen by remember { mutableStateOf(false) }
                OutlinedAction(
                    text = "Навыки персонажа",
                    subtitle = "подключено ${settings.skills.enabledIds.size} из ${settings.skills.all.size}",
                    palette = palette,
                    onClick = { skillsOpen = true },
                )
                if (skillsOpen) {
                    SkillsDialog(
                        palette = palette,
                        state = settings.skills,
                        onSave = {
                            onSaveSkills(it)
                            skillsOpen = false
                        },
                        onDismiss = { skillsOpen = false },
                    )
                }
            }

            SettingsCard(palette, title = "Память персонажа", icon = Icons.Filled.Psychology) {
                Text(
                    "Персонаж помнит то, что вы рассказали: имя, день рождения, город, привычки. " +
                        "Достаточно сказать «запомни, что меня зовут …» — память работает даже без " +
                        "подключения ИИ.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                if (settings.facts.isEmpty()) {
                    Text(
                        "Пока пусто.",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                    )
                } else {
                    settings.facts.takeLast(3).forEach { fact ->
                        Text(
                            text = "• ${fact.text}",
                            fontSize = 12.sp,
                            color = palette.textPrimary,
                        )
                    }
                    if (settings.facts.size > 3) {
                        Text(
                            "и ещё ${settings.facts.size - 3}",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedAction(
                    text = "Память персонажа",
                    subtitle = "фактов: ${settings.facts.size}",
                    palette = palette,
                    onClick = { memoryOpen = true },
                )
            }

            SettingsCard(palette, title = "Приложения отсюда", icon = Icons.Filled.Android) {
                Text(
                    "Вкладка «Отсюда» в списке приложений: сборки, сделанные в opencode/OpenChamber. " +
                        "Список берётся из реестра, а репозитории с APK подхватываются с GitHub " +
                        "автоматически — но только если у подключения GitHub указан токен.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Показывать вкладку", fontSize = 13.sp, color = palette.textPrimary)
                        Text(
                            "выключено — вкладка скрыта, реестр не читается",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = settings.showBuilds,
                        onCheckedChange = onShowBuilds,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accentText,
                        ),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = buildsUrl,
                    onValueChange = { buildsUrl = it },
                    label = { Text("Адрес реестра сборок") },
                    supportingText = {
                        Text("JSON со списком: имя, пакет, версия, ссылки на APK и иконку", fontSize = 10.sp)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onSaveBuildsUrl(buildsUrl) }) { Text("Сохранить адрес") }
                    TextButton(onClick = { buildsUrl = ru.lumen.launcher.data.BuildsRegistry.DEFAULT_URL }) {
                        Text("По умолчанию")
                    }
                    TextButton(onClick = onRefreshBuilds) { Text("Обновить") }
                }
            }

            SettingsCard(palette, title = "Подключения", icon = Icons.Filled.CloudDownload) {
                Text(
                    "Сервисы, откуда приложения попадают в списки, и подключение ИИ для персонажа. " +
                        "Коснитесь строки, чтобы изменить. Токен хранится только на устройстве.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                settings.connections.forEach { connection ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { editingConnection = connection }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(palette.accentText.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                connection.kind.label.take(1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentText,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(connection.displayLabel, fontSize = 13.sp, color = palette.textPrimary)
                            Text(
                                text = buildString {
                                    append(connection.baseUrl)
                                    if (connection.model.isNotBlank()) append(" · ${connection.model}")
                                    if (connection.token.isNotBlank()) append(" · ключ есть")
                                },
                                fontSize = 10.sp,
                                color = palette.textSecondary,
                                maxLines = 1,
                            )
                        }
                        IconButton(onClick = { editingConnection = connection }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Изменить подключение",
                                tint = palette.accentText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = { onDeleteConnection(connection) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Удалить подключение",
                                tint = palette.textSecondary.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedAction(
                    text = "Добавить подключение",
                    subtitle = "Teable, GitHub, ИИ или OpenChamber",
                    palette = palette,
                    onClick = { addingConnection = true },
                )
            }

            SettingsCard(palette, title = "Веб-приложения", icon = Icons.Filled.Public) {
                Text(
                    "Собственные приложения в отдельном контейнере. Не смешиваются с PackageManager.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                var editingWebApp by remember { mutableStateOf<WebApp?>(null) }
                settings.webApps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { editingWebApp = app }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(WebAppColors[app.colorIndex.mod(WebAppColors.size)].copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                app.name.take(1).uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebAppColors[app.colorIndex.mod(WebAppColors.size)],
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.name,
                                fontSize = 13.sp,
                                color = palette.textPrimary,
                            )
                            Text(
                                app.url,
                                fontSize = 10.sp,
                                color = palette.textSecondary,
                                maxLines = 1,
                            )
                        }
                        IconButton(onClick = { editingWebApp = app }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Изменить",
                                tint = palette.accentText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = { onDeleteWebApp(app) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Удалить",
                                tint = palette.textSecondary.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                editingWebApp?.let { app ->
                    WebAppDialog(
                        palette = palette,
                        initial = app,
                        onDismiss = { editingWebApp = null },
                        onSave = { updated ->
                            onUpdateWebApp(app, updated)
                            editingWebApp = null
                        },
                    )
                }
                Spacer(Modifier.height(6.dp))
                AddWebAppInline(palette = palette, onAdd = onAddWebApp)
            }

            Text(
                text = "Люмен 1.6 · лаунчер для Android\n" +
                    "Главный экран повторяет «Люмен» из Teable, остальные экраны — свои.",
                fontSize = 10.sp,
                color = palette.textSecondary.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun SettingsCard(
    palette: LumenPalette,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.panel)
            .border(1.dp, palette.panelBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ChoicePill(
    text: String,
    selected: Boolean,
    palette: LumenPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) palette.accentText.copy(alpha = if (palette.isNight) 0.22f else 0.14f)
                else Color.Transparent
            )
            .border(
                1.dp,
                if (selected) palette.accentText.copy(alpha = 0.5f) else palette.panelBorder,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (selected) palette.accentText else palette.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun OutlinedAction(
    text: String,
    subtitle: String,
    palette: LumenPalette,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, palette.panelBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text, fontSize = 13.sp, color = palette.textPrimary)
            Text(subtitle, fontSize = 10.sp, color = palette.textSecondary)
        }
        Icon(Icons.Filled.Check, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun AddWebAppInline(
    palette: LumenPalette,
    onAdd: (String, String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    if (!open) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, palette.panelBorder, RoundedCornerShape(14.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Добавить веб-приложение", fontSize = 13.sp, color = palette.textPrimary)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Адрес") },
            placeholder = { Text("https://…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onAdd(name.trim(), url.trim())
                        name = ""
                        url = ""
                        open = false
                    }
                },
            ) { Text("Добавить") }
            TextButton(onClick = { open = false }) { Text("Отмена") }
        }
    }
}

@Composable
private fun ConnectionEditorScreen(
    palette: LumenPalette,
    initial: Connection?,
    onSave: (Connection) -> Unit,
    onCancel: () -> Unit,
) {
    var kind by remember(initial) { mutableStateOf(initial?.kind ?: ServiceKind.TEABLE) }
    var label by remember(initial) { mutableStateOf(initial?.label.orEmpty()) }
    var baseUrl by remember(initial) {
        mutableStateOf(initial?.baseUrl?.ifBlank { null } ?: (initial?.kind ?: ServiceKind.TEABLE).defaultBaseUrl)
    }
    var token by remember(initial) { mutableStateOf(initial?.token.orEmpty()) }
    var username by remember(initial) { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember(initial) { mutableStateOf(initial?.password.orEmpty()) }
    var model by remember(initial) { mutableStateOf(initial?.model.orEmpty()) }
    var checking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<String?>(null) }
    var checkOk by remember { mutableStateOf<Boolean?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = palette.textPrimary)
            }
            Column {
                Text(
                    text = if (initial == null) "Новое подключение" else "Изменить подключение",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Text(
                    text = kind.hint.take(60),
                    fontSize = 10.sp,
                    color = palette.textSecondary,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceKind.entries.take(2).forEach { option ->
                    KindPill(option, kind, palette, Modifier.weight(1f)) { selected ->
                        kind = selected
                        if (baseUrl.isBlank() || ServiceKind.entries.any { it != selected && it.defaultBaseUrl == baseUrl }) {
                            baseUrl = selected.defaultBaseUrl
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceKind.entries.drop(2).forEach { option ->
                    KindPill(option, kind, palette, Modifier.weight(1f)) { selected ->
                        kind = selected
                        if (baseUrl.isBlank() || ServiceKind.entries.any { it != selected && it.defaultBaseUrl == baseUrl }) {
                            baseUrl = selected.defaultBaseUrl
                        }
                    }
                }
            }
            Text(kind.hint, fontSize = 11.sp, color = palette.textSecondary)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Название (необязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text(if (kind == ServiceKind.AI) "Адрес API" else "Адрес сервиса") },
                placeholder = { Text(kind.defaultBaseUrl.ifBlank { "http://192.168.1.10:4096" }) },
                supportingText = {
                    Text(
                        if (kind == ServiceKind.AI) "Например https://api.aitunnel.ru/v1 — без кавычек и пробелов"
                        else "Без кавычек и пробелов; https:// добавится сам",
                        fontSize = 10.sp,
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (kind != ServiceKind.OPENCHAMBER) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(if (kind == ServiceKind.GITHUB) "Токен (необязательно)" else "Ключ / токен") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (kind == ServiceKind.GITHUB || kind == ServiceKind.OPENCHAMBER) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (kind == ServiceKind.GITHUB) "Логин GitHub" else "Логин") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (kind == ServiceKind.OPENCHAMBER) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (kind == ServiceKind.AI) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Модель") },
                    placeholder = { Text("например deepseek-v4.1-flash") },
                    supportingText = { Text("Название модели как в кабинете провайдера", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedAction(
                    text = if (checking) "Проверяю…" else "Проверить связь",
                    subtitle = "настоящий запрос к модели",
                    palette = palette,
                    onClick = {
                        checking = true
                        checkResult = null
                        checkOk = null
                        scope.launch {
                            val probe = Connection(
                                id = initial?.id ?: "probe",
                                kind = ServiceKind.AI,
                                label = "",
                                baseUrl = ru.lumen.launcher.data.normalizeBaseUrl(baseUrl)
                                    .ifBlank { kind.defaultBaseUrl },
                                token = token.trim(),
                                model = model.trim(),
                            )
                            val result = ru.lumen.launcher.data.AiClient.complete(
                                probe,
                                listOf(
                                    ru.lumen.launcher.data.ChatMessage(
                                        "user",
                                        "Ответь одним словом: работает",
                                    )
                                ),
                            )
                            checking = false
                            checkOk = result.isSuccess
                            checkResult = result.fold(
                                onSuccess = { "Модель ответила: ${it.take(200)}" },
                                onFailure = { it.message ?: "неизвестная ошибка" },
                            )
                        }
                    },
                )
                checkResult?.let { message ->
                    Text(
                        text = message,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = if (checkOk == true) palette.accentText else Color(0xFFE24B9A),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Отмена") }
            TextButton(
                onClick = {
                    onSave(
                        Connection(
                            id = initial?.id ?: "conn-" + System.currentTimeMillis(),
                            kind = kind,
                            label = label.trim(),
                            baseUrl = ru.lumen.launcher.data.normalizeBaseUrl(baseUrl)
                                .ifBlank { kind.defaultBaseUrl },
                            token = token.trim(),
                            username = username.trim(),
                            password = password,
                            model = model.trim(),
                        )
                    )
                },
                modifier = Modifier.weight(1f),
            ) { Text("Сохранить") }
        }
    }
}

@Composable
private fun KindPill(
    option: ServiceKind,
    current: ServiceKind,
    palette: LumenPalette,
    modifier: Modifier = Modifier,
    onSelect: (ServiceKind) -> Unit,
) {
    val selected = option == current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.accentText.copy(alpha = 0.16f) else palette.panel)
            .border(
                1.dp,
                if (selected) palette.accentText.copy(alpha = 0.5f) else palette.panelBorder,
                RoundedCornerShape(14.dp),
            )
            .clickable { onSelect(option) }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = option.label,
            fontSize = 13.sp,
            maxLines = 1,
            color = if (selected) palette.accentText else palette.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Окно «Навыки персонажа»: переключатели встроенных навыков и свой навык. */
@Composable
private fun SkillsDialog(
    palette: LumenPalette,
    state: ru.lumen.launcher.data.SkillsState,
    onSave: (ru.lumen.launcher.data.SkillsState) -> Unit,
    onDismiss: () -> Unit,
) {
    var enabled by remember(state) { mutableStateOf(state.enabledIds) }
    var custom by remember(state) { mutableStateOf(state.custom) }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPrompt by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Навыки персонажа") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .height(if (adding) 300.dp else 360.dp),
            ) {
                Text(
                    "Подключённые навыки добавляются к характеру персонажа. «Телефон» — это реальные действия.",
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                state.all.forEach { skill ->
                    val isOn = skill.id in enabled
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (skill.kind == ru.lumen.launcher.data.SkillKind.DEVICE) {
                                    "${skill.name} · действия"
                                } else {
                                    skill.name
                                },
                                fontSize = 13.sp,
                                color = palette.textPrimary,
                            )
                            Text(
                                skill.description,
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                            )
                        }
                        if (skill.custom) {
                            IconButton(
                                onClick = { custom = custom.filterNot { it.id == skill.id } },
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Удалить навык",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Switch(
                            checked = isOn,
                            onCheckedChange = { checked ->
                                enabled = if (checked) enabled + skill.id else enabled - skill.id
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.accentText,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                if (adding) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Название навыка") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newPrompt,
                        onValueChange = { newPrompt = it },
                        label = { Text("Что персонаж должен делать") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                    )
                } else {
                    OutlinedAction(
                        text = "Добавить свой навык",
                        subtitle = "название и инструкция для персонажа",
                        palette = palette,
                        onClick = { adding = true },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (adding && newName.isNotBlank()) {
                        val skill = ru.lumen.launcher.data.Skill(
                            id = "custom-" + System.currentTimeMillis(),
                            name = newName.trim(),
                            description = newPrompt.trim().take(80).ifBlank { "свой навык" },
                            kind = ru.lumen.launcher.data.SkillKind.PROMPT,
                            prompt = newPrompt.trim(),
                            custom = true,
                        )
                        custom = custom + skill
                        enabled = enabled + skill.id
                    }
                    onSave(ru.lumen.launcher.data.SkillsState(enabledIds = enabled, custom = custom))
                },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = if (palette.isNight) Color(0xFF0E1B22) else Color.White,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
    )
}

/** Окно изменения веб-приложения: название, адрес и своя иконка. */
@Composable
private fun WebAppDialog(
    palette: LumenPalette,
    initial: WebApp,
    onSave: (WebApp) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var url by remember(initial) { mutableStateOf(initial.url) }
    var iconUrl by remember(initial) { mutableStateOf(initial.iconUrl.orEmpty()) }
    val valid = name.isNotBlank() && url.isNotBlank()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить приложение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Адрес") },
                    supportingText = { Text("Ссылка на PWA или сайт", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Иконка (необязательно)") },
                    supportingText = {
                        Text("Прямая ссылка на картинку; пусто — иконка из манифеста", fontSize = 10.sp)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            url = normalizeUrl(url),
                            iconUrl = iconUrl.trim().ifBlank { null },
                        )
                    )
                },
                enabled = valid,
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = if (palette.isNight) Color(0xFF0E1B22) else Color.White,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
    )
}
