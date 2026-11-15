package ru.lumen.launcher.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lumen.launcher.data.CatalogApp
import ru.lumen.launcher.data.CatalogState
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.InstalledApp
import ru.lumen.launcher.data.WebApp
import ru.lumen.launcher.data.normalizeUrl

private sealed interface AppsTab {
    data object Installed : AppsTab
    data object Web : AppsTab
    data class Service(val connection: Connection) : AppsTab
}

@Composable
fun AppsScreen(
    palette: LumenPalette,
    installed: List<InstalledApp>,
    loading: Boolean,
    webApps: List<WebApp>,
    connections: List<Connection>,
    catalogs: Map<String, CatalogState>,
    onLaunch: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onOpenUrl: (name: String, url: String) -> Unit,
    onAddWebApp: (name: String, url: String, iconUrl: String?) -> Unit,
    onDeleteWebApp: (WebApp) -> Unit,
    onRefreshCatalog: (Connection) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember(connections, installed.size, webApps.size) {
        buildList {
            add(AppsTab.Installed)
            add(AppsTab.Web)
            connections.forEach { add(AppsTab.Service(it)) }
        }
    }
    var tabIndex by remember(tabs.size) { mutableIntStateOf(0) }
    val currentTab = tabs.getOrNull(tabIndex.coerceIn(0, tabs.lastIndex)) ?: AppsTab.Installed
    var query by remember { mutableStateOf("") }
    var addDialogOpen by remember { mutableStateOf(false) }

    val filteredInstalled = remember(installed, query) {
        if (query.isBlank()) installed
        else installed.filter { it.label.contains(query, ignoreCase = true) }
    }
    val filteredWeb = remember(webApps, query) {
        if (query.isBlank()) webApps
        else webApps.filter { it.name.contains(query, true) || it.url.contains(query, true) }
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
                    text = "Мои приложения",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Text(
                    text = "установленные · веб · сервисы",
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = palette.textSecondary,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Обновить список", tint = palette.textPrimary)
            }
            IconButton(onClick = { addDialogOpen = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить приложение", tint = palette.textPrimary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                TabPill(
                    text = when (tab) {
                        AppsTab.Installed -> "Приложения · ${installed.size}"
                        AppsTab.Web -> "Веб · ${webApps.size}"
                        is AppsTab.Service -> {
                            val count = (catalogs[tab.connection.id] as? CatalogState.Ready)?.apps?.size
                            if (count == null) tab.connection.displayLabel
                            else "${tab.connection.displayLabel} · $count"
                        }
                    },
                    selected = index == tabIndex,
                    palette = palette,
                    onClick = { tabIndex = index },
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            singleLine = true,
            placeholder = { Text("Поиск", color = palette.textSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = RoundedCornerShape(18.dp),
        )

        when (val tab = currentTab) {
            AppsTab.Installed -> InstalledGrid(
                palette = palette,
                apps = filteredInstalled,
                loading = loading,
                onLaunch = onLaunch,
                onAppInfo = onAppInfo,
                modifier = Modifier.weight(1f),
            )

            AppsTab.Web -> WebAppsList(
                palette = palette,
                apps = filteredWeb,
                onOpen = { onOpenUrl(it.name, it.url) },
                onDelete = onDeleteWebApp,
                modifier = Modifier.weight(1f),
            )

            is AppsTab.Service -> CatalogList(
                palette = palette,
                connection = tab.connection,
                state = catalogs[tab.connection.id] ?: CatalogState.Loading,
                query = query,
                onOpen = { onOpenUrl(it.name, it.url) },
                onSave = { onAddWebApp(it.name, it.url, null) },
                onRetry = { onRefreshCatalog(tab.connection) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (addDialogOpen) {
        AddWebAppDialog(
            palette = palette,
            connections = connections,
            catalogs = catalogs,
            onDismiss = { addDialogOpen = false },
            onAdd = { name, url, icon ->
                onAddWebApp(name, url, icon)
                addDialogOpen = false
            },
        )
    }
}

@Composable
private fun TabPill(
    text: String,
    selected: Boolean,
    palette: LumenPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) {
        palette.accentText.copy(alpha = if (palette.isNight) 0.22f else 0.14f)
    } else {
        palette.panel
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(
                1.dp,
                if (selected) palette.accentText.copy(alpha = 0.45f) else palette.panelBorder,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) palette.accentText else palette.textSecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstalledGrid(
    palette: LumenPalette,
    apps: List<InstalledApp>,
    loading: Boolean,
    onLaunch: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (loading && apps.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = palette.accentText)
        }
        return
    }
    if (apps.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ничего не найдено", color = palette.textSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(apps, key = { it.component.flattenToString() }) { app ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .combinedClickable(
                        onClick = { onLaunch(app) },
                        onLongClick = { onAppInfo(app) },
                    )
                    .padding(vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (palette.isNight) Color(0x1FFFFFFF) else Color(0x66FFFFFF))
                        .border(1.dp, palette.panelBorder, RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = app.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = app.label,
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = app.label.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = palette.accentText,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = app.label,
                    fontSize = 10.5.sp,
                    lineHeight = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = palette.textPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WebAppsList(
    palette: LumenPalette,
    apps: List<WebApp>,
    onOpen: (WebApp) -> Unit,
    onDelete: (WebApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (apps.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Добавьте веб-приложение кнопкой «+»",
                color = palette.textSecondary,
                fontSize = 13.sp,
            )
        }
        return
    }

    var pendingDelete by remember { mutableStateOf<WebApp?>(null) }

    if (pendingDelete != null) {
        val target = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить приложение?") },
            text = { Text("«${target.name}» исчезнет из списка. Само приложение в сервисе не пострадает.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(target)
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
            containerColor = if (palette.isNight) Color(0xFF0E1B22) else Color.White,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textPrimary,
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(apps) { app ->
            val color = WebAppColors[app.colorIndex.mod(WebAppColors.size)]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.panel)
                    .border(1.dp, palette.panelBorder, RoundedCornerShape(20.dp))
                    .combinedClickable(
                        onClick = { onOpen(app) },
                        onLongClick = { pendingDelete = app },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteIcon(
                    iconUrl = app.iconUrl,
                    appUrl = app.url,
                    name = app.name,
                    tint = color,
                    size = 44.dp,
                    corner = 14.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = Uri.parse(app.url).host ?: app.url,
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = "Открыть",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onDelete(app) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Удалить",
                        tint = palette.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        item {
            Text(
                text = "Открываются в отдельном веб-контейнере, а не в системном списке Android. " +
                    "Долгое нажатие или корзина удаляет приложение из списка.",
                fontSize = 10.sp,
                color = palette.textSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CatalogList(
    palette: LumenPalette,
    connection: Connection,
    state: CatalogState,
    query: String,
    onOpen: (CatalogApp) -> Unit,
    onSave: (CatalogApp) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CatalogState.Loading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = palette.accentText)
                Spacer(Modifier.height(10.dp))
                Text("Читаю ${connection.displayLabel}…", color = palette.textSecondary, fontSize = 12.sp)
            }
        }

        is CatalogState.Error -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 28.dp),
            ) {
                Text(state.message, color = palette.textPrimary, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) { Text("Повторить") }
            }
        }

        is CatalogState.Ready -> {
            val apps = if (query.isBlank()) state.apps
            else state.apps.filter { it.name.contains(query, true) || it.description.contains(query, true) }
            if (apps.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("В сервисе пока нет приложений", color = palette.textSecondary, fontSize = 13.sp)
                }
                return
            }
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(apps, key = { it.id }) { app ->
                    CatalogRow(
                        palette = palette,
                        app = app,
                        onOpen = { onOpen(app) },
                        onSave = { onSave(app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogRow(
    palette: LumenPalette,
    app: CatalogApp,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.panel)
            .border(1.dp, palette.panelBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteIcon(
            iconUrl = null,
            appUrl = app.url,
            name = app.name,
            tint = palette.accentText,
            size = 44.dp,
            corner = 14.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.description.ifBlank { Uri.parse(app.url).host ?: app.url },
                fontSize = 11.sp,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onSave) {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = "Добавить в мои",
                tint = palette.accentText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AddWebAppDialog(
    palette: LumenPalette,
    connections: List<Connection>,
    catalogs: Map<String, CatalogState>,
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String, iconUrl: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var iconUrl by remember { mutableStateOf<String?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && normalizeUrl(url).isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое приложение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (connections.isNotEmpty()) {
                    TextButton(
                        onClick = { pickerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Выбрать из подключённых сервисов")
                    }
                } else {
                    Text(
                        text = "Подключения к сервисам (Teable, GitHub, OpenChamber) добавляются в настройках — " +
                            "тогда приложение можно выбрать из списка.",
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        iconUrl = null
                    },
                    label = { Text("Адрес") },
                    placeholder = { Text("https://appqr1zalped2qimaez.teable.app") },
                    supportingText = {
                        Text(
                            "Ссылка на PWA: адрес Teable-приложения, GitHub Pages или сервера OpenChamber",
                            fontSize = 10.sp,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name.trim(), normalizeUrl(url), iconUrl) },
                enabled = valid,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = if (palette.isNight) Color(0xFF0E1B22) else Color.White,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
    )

    if (pickerOpen) {
        CatalogPickerDialog(
            palette = palette,
            connections = connections,
            catalogs = catalogs,
            onDismiss = { pickerOpen = false },
            onPick = { app ->
                name = app.name
                url = app.url
                iconUrl = null
                pickerOpen = false
            },
        )
    }
}

@Composable
fun CatalogPickerDialog(
    palette: LumenPalette,
    connections: List<Connection>,
    catalogs: Map<String, CatalogState>,
    onDismiss: () -> Unit,
    onPick: (CatalogApp) -> Unit,
) {
    var index by remember(connections.size) { mutableIntStateOf(0) }
    val connection = connections.getOrNull(index.coerceIn(0, (connections.size - 1).coerceAtLeast(0)))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Приложения из сервисов") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    connections.forEachIndexed { tabIndex, item ->
                        TabPill(
                            text = item.displayLabel,
                            selected = tabIndex == index,
                            palette = palette,
                            onClick = { index = tabIndex },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.height(320.dp)) {
                    if (connection == null) {
                        Text("Нет подключений", color = palette.textSecondary, fontSize = 13.sp)
                    } else {
                        CatalogList(
                            palette = palette,
                            connection = connection,
                            state = catalogs[connection.id] ?: CatalogState.Loading,
                            query = "",
                            onOpen = { onPick(it) },
                            onSave = { onPick(it) },
                            onRetry = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
        containerColor = if (palette.isNight) Color(0xFF0E1B22) else Color.White,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
    )
}