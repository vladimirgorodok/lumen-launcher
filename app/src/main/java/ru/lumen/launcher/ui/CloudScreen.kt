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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.lumen.launcher.data.LumenSettings
import ru.lumen.launcher.data.cloud.CloudContainer
import ru.lumen.launcher.data.cloud.RemoteEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Обмен данными с Яндекс Диском: логин, пароль приложения, папка,
 * кнопки «Сохранить» и «Загрузить», список копий в облаке и снимки на устройстве.
 */
@Composable
fun CloudScreen(
    palette: LumenPalette,
    settings: LumenSettings,
    container: CloudContainer,
    onSaveLogin: (String) -> Unit,
    onSavePassword: (String) -> Unit,
    onSaveFolder: (String) -> Unit,
    onAutoSync: (Boolean) -> Unit,
    onKeepCopies: (Int) -> Unit,
    onSendSecrets: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val status by container.coordinator.status.collectAsStateWithLifecycle()

    var login by remember(settings.yandexLogin) { mutableStateOf(settings.yandexLogin) }
    var password by remember(settings.yandexPassword) { mutableStateOf(settings.yandexPassword) }
    var folder by remember(settings.cloudFolder) { mutableStateOf(settings.cloudFolder) }
    var replaceAll by remember { mutableStateOf(true) }
    var folderPicker by remember { mutableStateOf(false) }

    var snapshots by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var snapshotsError by remember { mutableStateOf<String?>(null) }
    var localSnapshots by remember { mutableStateOf(0) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")) }

    fun saveCredentials() {
        onSaveLogin(login)
        onSavePassword(password)
        onSaveFolder(folder)
    }

    fun refreshSnapshots() {
        if (!settings.cloudConfigured) return
        scope.launch {
            container.coordinator.loadSnapshots().fold(
                onSuccess = {
                    snapshots = it
                    snapshotsError = null
                },
                onFailure = {
                    snapshots = emptyList()
                    snapshotsError = it.message ?: "Не удалось прочитать папку на Диске"
                },
            )
        }
    }

    LaunchedEffect(settings.cloudConfigured) {
        if (settings.cloudConfigured) refreshSnapshots()
    }

    LaunchedEffect(Unit) { localSnapshots = container.archive.count() }

    if (folderPicker) {
        CloudFolderScreen(
            palette = palette,
            container = container,
            startPath = folder,
            onPick = { picked ->
                folder = picked
                onSaveFolder(picked)
                folderPicker = false
                refreshSnapshots()
            },
            onBack = { folderPicker = false },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Обмен данными",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Text(
                    if (settings.cloudConfigured) "облако: ${settings.cloudFolder}" else "облако не настроено",
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = palette.textSecondary,
                )
            }
            Icon(Icons.Filled.Cloud, contentDescription = null, tint = palette.accentText)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            status?.message?.let { message ->
                CloudCard(palette, title = "Состояние", icon = Icons.Filled.Refresh) {
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = if (status?.failed == true) palette.accent else palette.textPrimary,
                    )
                    if (status?.busy == true) {
                        Spacer(Modifier.height(4.dp))
                        Text("Идёт обмен…", fontSize = 11.sp, color = palette.textSecondary)
                    }
                }
            }

            CloudCard(palette, title = "Яндекс Диск", icon = Icons.Filled.Cloud) {
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Логин Яндекс") },
                    supportingText = { Text("Логин Яндекс ID, например ivan", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль приложения") },
                    supportingText = {
                        Text(
                            "Обычный пароль не подойдёт. Создайте пароль типа «Файлы» " +
                                "на id.yandex.ru/security/app-passwords. Обмен по WebDAV " +
                                "открыт подписчикам Яндекс 360.",
                            fontSize = 10.sp,
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text("Папка на Диске") },
                    supportingText = { Text("Копии лягут в эту папку, старые удаляются", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { folderPicker = true }, enabled = settings.cloudConfigured) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Выбрать папку", color = palette.accentText)
                    }
                    TextButton(onClick = { saveCredentials() }) {
                        Text("Сохранить настройки", color = palette.accentText)
                    }
                }
            }

            CloudCard(palette, title = "Обмен", icon = Icons.Filled.Upload) {
                TextButton(
                    onClick = {
                        saveCredentials()
                        container.coordinator.checkInBackground(login, password, folder)
                    },
                ) { Text("Проверить подключение", color = palette.accentText) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Автообмен", fontSize = 13.sp, color = palette.textPrimary)
                        Text(
                            "сверка при запуске, выгрузка изменений через 8 секунд и при уходе в фон",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = settings.autoSync,
                        onCheckedChange = onAutoSync,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accentText,
                        ),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Загружать заменой", fontSize = 13.sp, color = palette.textPrimary)
                        Text(
                            "включено — состояние станет как в копии; выключено — совпадающие " +
                                "записи обновятся, свои останутся",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = replaceAll,
                        onCheckedChange = { replaceAll = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accentText,
                        ),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Токены подключений в облако", fontSize = 13.sp, color = palette.textPrimary)
                        Text(
                            "включено — токены и пароли сервисов уедут в копию на Диске; " +
                                "выключено — переносятся только адреса и названия",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = settings.cloudSendSecrets,
                        onCheckedChange = onSendSecrets,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accentText,
                        ),
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            saveCredentials()
                            container.coordinator.pushInBackground()
                        },
                        enabled = settings.cloudConfigured && status?.busy != true,
                    ) { Text("Сохранить в облако", color = palette.accentText) }

                    TextButton(
                        onClick = {
                            saveCredentials()
                            container.coordinator.pullInBackground(replaceAll)
                        },
                        enabled = settings.cloudConfigured && status?.busy != true,
                    ) { Text("Загрузить из облака", color = palette.accentText) }
                }
            }

            CloudCard(palette, title = "Копии в облаке", icon = Icons.Filled.Cloud) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (snapshots.isEmpty()) "Копий пока нет" else "свежих копий: ${snapshots.size}",
                            fontSize = 13.sp,
                            color = palette.textPrimary,
                        )
                        Text(
                            "держим последние ${settings.cloudKeepCopies}, остальные удаляем",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                    IconButton(onClick = { refreshSnapshots() }, enabled = settings.cloudConfigured) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Обновить", tint = palette.textSecondary)
                    }
                }
                snapshots.take(5).forEach { entry ->
                    Text(
                        text = entry.name,
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                snapshotsError?.let { error ->
                    Text(error, fontSize = 11.sp, color = palette.accent)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Сколько копий держать", fontSize = 13.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { onKeepCopies((settings.cloudKeepCopies - 1).coerceAtLeast(1)) },
                        enabled = settings.cloudKeepCopies > 1,
                    ) { Text("−", color = palette.accentText) }
                    Text("${settings.cloudKeepCopies}", fontSize = 13.sp, color = palette.textPrimary)
                    TextButton(
                        onClick = { onKeepCopies((settings.cloudKeepCopies + 1).coerceAtMost(50)) },
                        enabled = settings.cloudKeepCopies < 50,
                    ) { Text("+", color = palette.accentText) }
                }
            }

            CloudCard(palette, title = "Снимки на устройстве", icon = Icons.Filled.Folder) {
                Text(
                    "Перед загрузкой «заменой» состояние сохраняется на телефоне: " +
                        "если в облаке оказалась не та копия, из снимка всё можно вернуть.",
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "снимков: $localSnapshots (держим ${ru.lumen.launcher.data.cloud.LumenArchive.KEEP})",
                        fontSize = 12.sp,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            scope.launch {
                                container.archive.create(force = true)
                                localSnapshots = container.archive.count()
                            }
                        },
                    ) { Text("Сделать снимок", color = palette.accentText) }
                }
                settings.cloudLastSyncAt.takeIf { it > 0L }?.let { at ->
                    Text(
                        "последний обмен: ${dateFormat.format(Date(at))}" +
                            settings.cloudLastFileName.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                        fontSize = 10.sp,
                        color = palette.textSecondary,
                    )
                }
                settings.cloudLastError.takeIf { it.isNotBlank() }?.let { error ->
                    Text("прошлая ошибка: $error", fontSize = 10.sp, color = palette.accent)
                }
            }
        }
    }
}

/** Выбор папки на Яндекс Диске. */
@Composable
private fun CloudFolderScreen(
    palette: LumenPalette,
    container: CloudContainer,
    startPath: String,
    onPick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var path by remember { mutableStateOf(if (startPath.startsWith("/")) startPath else "/$startPath") }
    var folders by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }

    fun load(target: String) {
        scope.launch {
            container.coordinator.loadFolders(target).fold(
                onSuccess = {
                    folders = it
                    error = null
                },
                onFailure = {
                    folders = emptyList()
                    error = it.message ?: "Не удалось прочитать папку"
                },
            )
        }
    }

    LaunchedEffect(path) { load(path) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = palette.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Папка на Диске", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                Text(path, fontSize = 10.sp, color = palette.textSecondary)
            }
            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = palette.accentText)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onPick(path) }) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Выбрать эту папку", color = palette.accentText)
                }
                if (path != "/") {
                    TextButton(onClick = { path = path.substringBeforeLast('/').ifBlank { "/" } }) {
                        Text("Наверх", color = palette.accentText)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Новая папка") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isEmpty()) return@IconButton
                        val target = (if (path == "/") "" else path) + "/" + name
                        scope.launch {
                            container.coordinator.createFolder(target).fold(
                                onSuccess = {
                                    newName = ""
                                    path = target
                                    load(target)
                                },
                                onFailure = { error = it.message ?: "Не удалось создать папку" },
                            )
                        }
                    },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Создать папку", tint = palette.accentText)
                }
            }

            error?.let { Text(it, fontSize = 11.sp, color = palette.accent) }

            if (folders.isEmpty() && error == null) {
                Text("Вложенных папок нет — можно выбрать эту.", fontSize = 12.sp, color = palette.textSecondary)
            }

            folders.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.panel)
                        .border(1.dp, palette.panelBorder, RoundedCornerShape(14.dp))
                        .clickable { path = entry.path }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(entry.name, fontSize = 13.sp, color = palette.textPrimary)
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CloudCard(
    palette: LumenPalette,
    title: String,
    icon: ImageVector,
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
