package ru.lumen.launcher.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lumen.launcher.data.InstalledApp
import ru.lumen.launcher.data.WebApp
import ru.lumen.launcher.data.normalizeUrl

private enum class AppsTab { INSTALLED, WEB }

@Composable
fun AppsScreen(
    palette: LumenPalette,
    installed: List<InstalledApp>,
    loading: Boolean,
    webApps: List<WebApp>,
    onLaunch: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onOpenWebApp: (WebApp) -> Unit,
    onAddWebApp: (name: String, url: String) -> Unit,
    onDeleteWebApp: (WebApp) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(AppsTab.INSTALLED) }
    var query by remember { mutableStateOf("") }
    var addDialogOpen by remember { mutableStateOf(false) }

    val filteredInstalled = remember(installed, query) {
        if (query.isBlank()) installed
        else installed.filter { it.label.contains(query, ignoreCase = true) }
    }
    val filteredWeb = remember(webApps, query) {
        if (query.isBlank()) webApps
        else webApps.filter {
            it.name.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }
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
                    text = "установленные · веб-контейнеры",
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = palette.textSecondary,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Обновить список", tint = palette.textPrimary)
            }
            IconButton(onClick = { addDialogOpen = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить веб-приложение", tint = palette.textPrimary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TabPill(
                text = "Приложения · ${installed.size}",
                selected = tab == AppsTab.INSTALLED,
                palette = palette,
                onClick = { tab = AppsTab.INSTALLED },
                modifier = Modifier.weight(1f),
            )
            TabPill(
                text = "Веб · ${webApps.size}",
                selected = tab == AppsTab.WEB,
                palette = palette,
                onClick = { tab = AppsTab.WEB },
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            singleLine = true,
            placeholder = {
                Text(
                    if (tab == AppsTab.INSTALLED) "Поиск приложений" else "Поиск веб-приложений",
                    color = palette.textSecondary,
                )
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = RoundedCornerShape(18.dp),
        )

        when (tab) {
            AppsTab.INSTALLED -> InstalledGrid(
                palette = palette,
                apps = filteredInstalled,
                loading = loading,
                onLaunch = onLaunch,
                onAppInfo = onAppInfo,
                modifier = Modifier.weight(1f),
            )

            AppsTab.WEB -> WebAppsList(
                palette = palette,
                apps = filteredWeb,
                onOpen = onOpenWebApp,
                onDelete = onDeleteWebApp,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (addDialogOpen) {
        AddWebAppDialog(
            palette = palette,
            onDismiss = { addDialogOpen = false },
            onAdd = { name, url ->
                onAddWebApp(name, url)
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
            .padding(vertical = 11.dp),
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
            Text(
                text = "Ничего не найдено",
                color = palette.textSecondary,
                fontSize = 13.sp,
            )
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
                        onLongClick = { onDelete(app) },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.18f))
                        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = app.name.take(1).uppercase(),
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                    )
                }
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
                text = "Долгое нажатие или корзина удаляет приложение из списка. " +
                    "Открываются они в отдельном веб-контейнере, не в общем списке Android.",
                fontSize = 10.sp,
                color = palette.textSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AddWebAppDialog(
    palette: LumenPalette,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && normalizeUrl(url).isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое веб-приложение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Адрес") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name.trim(), normalizeUrl(url)) },
                enabled = valid,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        containerColor = if (palette.isNight) Color(0xFF0E1B22) else Color.White,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
    )
}

@Suppress("unused")
private fun LumenPalette.textOn(color: Color): Color = if (isNight) Color.White else color