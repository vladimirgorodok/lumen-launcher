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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightlightRound
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
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.LumenSettings
import ru.lumen.launcher.data.ServiceKind
import ru.lumen.launcher.data.WebApp

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
    onSaveConnection: (Connection) -> Unit,
    onDeleteConnection: (Connection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

            SettingsCard(palette, title = "Подключения", icon = Icons.Filled.CloudDownload) {
                Text(
                    "Сервисы, откуда приложения попадают в списки: Teable, GitHub, OpenChamber. " +
                        "Токен хранится только на устройстве.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                settings.connections.forEach { connection ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
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
                                connection.baseUrl,
                                fontSize = 10.sp,
                                color = palette.textSecondary,
                                maxLines = 1,
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
                ConnectionEditor(palette = palette, onSave = onSaveConnection)
            }

            SettingsCard(palette, title = "Веб-приложения", icon = Icons.Filled.Public) {
                Text(
                    "Собственные приложения в отдельном контейнере. Не смешиваются с PackageManager.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                settings.webApps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
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
                Spacer(Modifier.height(6.dp))
                AddWebAppInline(palette = palette, onAdd = onAddWebApp)
            }

            Text(
                text = "Люмен 1.0 · лаунчер для Android\n" +
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
private fun ConnectionEditor(
    palette: LumenPalette,
    onSave: (Connection) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(ServiceKind.TEABLE) }
    var label by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(ServiceKind.TEABLE.defaultBaseUrl) }
    var token by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            Text("Добавить подключение", fontSize = 13.sp, color = palette.textPrimary)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ServiceKind.entries.forEach { option ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (option == kind) palette.accentText.copy(alpha = 0.14f) else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (option == kind) palette.accentText.copy(alpha = 0.5f) else palette.panelBorder,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            kind = option
                            if (baseUrl.isBlank() || baseUrl == ServiceKind.entries.firstOrNull { it != option }?.defaultBaseUrl) {
                                baseUrl = option.defaultBaseUrl
                            }
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option.label,
                        fontSize = 11.sp,
                        color = if (option == kind) palette.accentText else palette.textSecondary,
                        fontWeight = if (option == kind) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        Text(kind.hint, fontSize = 10.sp, color = palette.textSecondary)
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
            label = { Text("Адрес сервиса") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (kind != ServiceKind.OPENCHAMBER) {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(if (kind == ServiceKind.GITHUB) "Токен (необязательно)" else "Токен") },
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    val resolvedBase = baseUrl.trim().ifBlank { kind.defaultBaseUrl }
                    onSave(
                        Connection(
                            id = "conn-" + System.currentTimeMillis(),
                            kind = kind,
                            label = label.trim(),
                            baseUrl = resolvedBase,
                            token = token.trim(),
                            username = username.trim(),
                            password = password,
                        )
                    )
                    label = ""
                    token = ""
                    username = ""
                    password = ""
                    open = false
                },
            ) { Text("Сохранить") }
            TextButton(onClick = { open = false }) { Text("Отмена") }
        }
    }
}
