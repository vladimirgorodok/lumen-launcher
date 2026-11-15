package ru.lumen.launcher.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.lumen.launcher.data.AiClient
import ru.lumen.launcher.data.AppsRepository
import ru.lumen.launcher.data.BuildApp
import ru.lumen.launcher.data.BuildInstaller
import ru.lumen.launcher.data.BuildsRegistry
import ru.lumen.launcher.data.BuildsState
import ru.lumen.launcher.data.CatalogLoader
import ru.lumen.launcher.data.CatalogState
import ru.lumen.launcher.data.Character
import ru.lumen.launcher.data.ChatMessage
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.DEFAULT_LAT
import ru.lumen.launcher.data.DEFAULT_LON
import ru.lumen.launcher.data.DeviceActions
import ru.lumen.launcher.data.InstalledApp
import ru.lumen.launcher.data.LocationProvider
import ru.lumen.launcher.data.LumenPreferences
import ru.lumen.launcher.data.LumenSettings
import ru.lumen.launcher.data.Memory
import ru.lumen.launcher.data.MemoryFact
import ru.lumen.launcher.data.ServiceKind
import ru.lumen.launcher.data.Skill
import ru.lumen.launcher.data.WeatherClient
import ru.lumen.launcher.data.WeatherCodec
import ru.lumen.launcher.data.WebApp
import ru.lumen.launcher.data.characterById
import ru.lumen.launcher.util.DayPeriod
import ru.lumen.launcher.util.dayPeriod
import ru.lumen.launcher.util.dayPeriodLabel
import ru.lumen.launcher.util.seasonFor
import ru.lumen.launcher.util.seasonLabel
import ru.lumen.launcher.web.WebAppActivity
import java.time.LocalDateTime

private enum class Screen { CLOCK, APPS, AI, SETTINGS }

@Composable
fun LumenRoot() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val preferences = remember { LumenPreferences(context) }
    val appsRepository = remember { AppsRepository(context) }
    val locationProvider = remember { LocationProvider(context) }
    val scope = rememberCoroutineScope()

    val settings by preferences.settings.collectAsStateWithLifecycle(initialValue = LumenSettings())
    val lifecycleOwner = LocalLifecycleOwner.current

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var screen by rememberSaveable { mutableStateOf(Screen.CLOCK) }
    var saverActive by rememberSaveable { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isDefaultLauncher by remember { mutableStateOf(false) }

    var weather by remember { mutableStateOf<WeatherUiState>(WeatherUiState.Loading) }
    var weatherRefresh by remember { mutableIntStateOf(0) }

    var installed by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var appsLoading by remember { mutableStateOf(true) }
    var appsRefresh by remember { mutableIntStateOf(0) }

    var battery by remember { mutableStateOf(BatteryState()) }
    var catalogs by remember { mutableStateOf<Map<String, CatalogState>>(emptyMap()) }
    var catalogRefresh by remember { mutableIntStateOf(0) }

    var chat by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiStatus by remember { mutableStateOf<String?>(null) }
    val deviceActions = remember { DeviceActions(context, appsRepository) }

    var builds by remember { mutableStateOf<List<BuildApp>>(emptyList()) }
    var buildsState by remember { mutableStateOf<BuildsState>(BuildsState.Loading) }
    var buildsRefresh by remember { mutableIntStateOf(0) }
    var installProgress by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    val buildInstaller = remember { BuildInstaller(context) }

    val character = characterById(settings.characterId)
    val enabledSkills = settings.skills.enabled
    val aiConnection = settings.connections.firstOrNull { it.kind == ServiceKind.AI }

    // Память и диалог переживают перезапуск лаунчера.
    var loadKey by remember { mutableStateOf<String?>(null) }
    var chatLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(settings.characterId, settings.rememberChat) {
        val key = "${settings.characterId}|${settings.rememberChat}"
        if (loadKey == key) return@LaunchedEffect
        chat = if (settings.rememberChat) preferences.chatFor(settings.characterId) else emptyList()
        loadKey = key
        chatLoaded = true
    }

    LaunchedEffect(chat, chatLoaded, settings.rememberChat) {
        if (!chatLoaded || !settings.rememberChat) return@LaunchedEffect
        preferences.setChatHistory(settings.characterId, chat)
    }

    val persistFacts: (List<MemoryFact>) -> Unit = { facts ->
        scope.launch { preferences.setMemory(facts) }
    }

    val sendToCharacter: (String) -> Unit = { raw ->
        val text = raw.trim()
        if (text.isNotEmpty()) {
            chat = chat + ChatMessage("user", text)
            var facts = settings.facts
            var note: String? = null

            // Сначала память: работает даже без модели.
            val toLearn = Memory.extract(text) ?: Memory.extractImplicit(text)
            if (toLearn != null) {
                facts = Memory.learn(facts, toLearn)
                note = "Запомнил: $toLearn"
            }
            val toForget = Memory.extractForget(text)
            if (toForget != null) {
                val before = facts.size
                facts = Memory.forget(facts, toForget)
                note = if (facts.size < before) "Забыл: $toForget" else "В памяти такого не было: $toForget"
            }
            if (facts !== settings.facts) persistFacts(facts)

            val connection = aiConnection
            val fromMemory = Memory.localAnswer(text, facts)
            when {
                // Прямой вопрос о том, что помнится, — отвечаем сразу, без модели.
                fromMemory != null -> chat = chat + ChatMessage("assistant", fromMemory)

                connection == null -> chat = chat + ChatMessage(
                    "assistant",
                    note?.let {
                        "$it.\n\nПодключение ИИ не настроено, поэтому поговорить не получится: " +
                            "добавьте адрес, ключ и модель в «Настройки → Подключения → ИИ»."
                    } ?: "Нет подключения ИИ. Добавьте его в «Настройки → Подключения → ИИ»: адрес, ключ и модель.",
                )

                else -> scope.launch {
                    aiBusy = true
                    aiStatus = "Отправляю запрос…"
                    val system = buildCharacterPrompt(character, enabledSkills, facts)
                    val history = chat.takeLast(20).map { ChatMessage(it.role, it.content) }
                    val result = AiClient.complete(connection, listOf(ChatMessage("system", system)) + history)
                    aiBusy = false
                    aiStatus = null
                    result.fold(
                        onSuccess = { reply ->
                            val command = AiClient.parseAction(reply)
                            val canAct = enabledSkills.any { it.id == "android-device" }
                            when {
                                command == null -> chat = chat + ChatMessage(
                                    "assistant",
                                    withMemoryNote(reply, note),
                                )

                                command.action == "remember" -> {
                                    val fact = command.text.ifBlank { text }
                                    val learned = Memory.learn(facts, fact, "ai")
                                    facts = learned
                                    persistFacts(learned)
                                    chat = chat + ChatMessage("assistant", "Запомнил: $fact")
                                }

                                command.action == "forget" -> {
                                    val query = command.text.ifBlank { text }
                                    val kept = Memory.forget(facts, query)
                                    facts = kept
                                    persistFacts(kept)
                                    chat = chat + ChatMessage("assistant", "Забыл: $query")
                                }

                                canAct -> {
                                    val outcome = deviceActions.execute(command)
                                    chat = chat + ChatMessage("assistant", outcome)
                                }

                                else -> chat = chat + ChatMessage(
                                    "assistant",
                                    "Хотел выполнить действие «${command.action}», но навык «Телефон» выключен. " +
                                        "Включите его в «Настройках → Персонаж → Навыки персонажа».",
                                )
                            }
                        },
                        onFailure = { error ->
                            chat = chat + ChatMessage(
                                "assistant",
                                buildString {
                                    if (note != null) append("$note\n\n")
                                    append("Не получилось: ${error.message ?: "неизвестная ошибка"}")
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) weatherRefresh += 1
    }

    val period = dayPeriod(now.hour)
    val season = seasonFor(now.monthValue)
    val palette = paletteFor(period, season, nightMode = period == DayPeriod.NIGHT)

    LaunchedEffect(Unit) {
        while (isActive) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val percent = if (level >= 0 && scale > 0) level * 100 / scale else 100
                battery = BatteryState(
                    level = percent,
                    charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL,
                )
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    LaunchedEffect(Unit) {
        if (!locationProvider.hasPermission()) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    LaunchedEffect(weatherRefresh) {
        if (weather !is WeatherUiState.Ready) {
            val cached = withContext(Dispatchers.IO) { preferences.settings.first().weatherCache }
            WeatherCodec.decode(cached)?.let { weather = WeatherUiState.Ready(it, cached = true) }
        }
        val coordinates = if (locationProvider.hasPermission()) locationProvider.lastKnown() else null
        val latitude = coordinates?.first ?: DEFAULT_LAT
        val longitude = coordinates?.second ?: DEFAULT_LON
        val result = WeatherClient.fetch(latitude, longitude, if (coordinates == null) "Москва" else null)
        if (result != null) {
            weather = WeatherUiState.Ready(result)
            withContext(Dispatchers.IO) { preferences.setWeatherCache(WeatherCodec.encode(result)) }
        } else if (weather !is WeatherUiState.Ready) {
            weather = WeatherUiState.Error(
                if (!context.isOnline()) "Нет сети — включите интернет" else "Не удалось получить погоду"
            )
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        lastInteraction = System.currentTimeMillis()
        isDefaultLauncher = appsRepository.isDefaultLauncher()
        if (weather is WeatherUiState.Ready) weatherRefresh += 1
        // после установки APK список и набор пакетов должны обновиться
        appsRefresh += 1
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(15 * 60 * 1000L)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                weatherRefresh += 1
            }
        }
    }

    LaunchedEffect(appsRefresh) {
        appsLoading = true
        installed = runCatching { appsRepository.load() }.getOrDefault(emptyList())
        appsLoading = false
        isDefaultLauncher = appsRepository.isDefaultLauncher()
    }

    LaunchedEffect(settings.buildsUrl, settings.showBuilds, buildsRefresh, settings.connections) {
        if (!settings.showBuilds) {
            builds = emptyList()
            buildsState = BuildsState.Ready(emptyList())
            return@LaunchedEffect
        }
        buildsState = BuildsState.Loading
        val registry = BuildsRegistry.load(settings.buildsUrl)
        val known = registry.getOrDefault(emptyList()).map { it.repo.lowercase() }.toSet()
        val discovered = settings.connections
            .filter { it.kind == ServiceKind.GITHUB && it.token.isNotBlank() }
            .flatMap { connection ->
                runCatching { BuildsRegistry.discover(connection, known) }.getOrDefault(emptyList())
            }
        buildsState = registry.fold(
            onSuccess = { apps -> BuildsState.Ready(apps + discovered) },
            onFailure = { error ->
                if (discovered.isNotEmpty()) BuildsState.Ready(discovered)
                else BuildsState.Error(error.message ?: "реестр недоступен")
            },
        )
        builds = when (val state = buildsState) {
            is BuildsState.Ready -> state.apps
            else -> emptyList()
        }
    }

    LaunchedEffect(settings.connections, catalogRefresh) {        val connections = settings.connections
        catalogs = connections.associate { it.id to CatalogState.Loading }
        connections.forEach { connection ->
            val state = CatalogLoader.load(connection)
            catalogs = catalogs + (connection.id to state)
        }
    }

    LaunchedEffect(saverActive, settings.screensaverMinutes) {
        if (saverActive) return@LaunchedEffect
        val minutes = settings.screensaverMinutes
        if (minutes <= 0) return@LaunchedEffect
        while (isActive) {
            delay(5_000)
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) continue
            if (System.currentTimeMillis() - lastInteraction >= minutes * 60_000L) {
                saverActive = true
                break
            }
        }
    }

    LaunchedEffect(saverActive, palette.isNight) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (saverActive) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        controller.isAppearanceLightStatusBars = !palette.isNight && !saverActive
        controller.isAppearanceLightNavigationBars = !palette.isNight && !saverActive
    }

    BackHandler {
        when {
            saverActive -> saverActive = false
            screen != Screen.CLOCK -> screen = Screen.CLOCK
        }
    }

    val registerInteraction: () -> Unit = { lastInteraction = System.currentTimeMillis() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.base)
            .pointerInput(screen, saverActive) {
                var downY = 0f
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        registerInteraction()
                        when (event.type) {
                            PointerEventType.Press -> downY = change.position.y
                            PointerEventType.Release -> {
                                val delta = downY - change.position.y
                                if (!saverActive && screen == Screen.CLOCK && delta > 120f) {
                                    screen = Screen.APPS
                                }
                            }
                        }
                    }
                }
            }
    ) {
        ProceduralBackground(
            period = period,
            season = season,
            weather = (weather as? WeatherUiState.Ready)?.snapshot?.kind
                ?: ru.lumen.launcher.util.WeatherKind.UNKNOWN,
            seed = settings.backgroundSeed,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            if (!saverActive && screen == Screen.CLOCK) {
                ClockHeader(
                    palette = palette,
                    subtitle = "${dayPeriodLabel(period)} · ${seasonLabel(season)}",
                    onMenuAction = { screen = it },
                    onStartSaver = {
                        registerInteraction()
                        saverActive = true
                    },
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (screen) {
                    Screen.CLOCK -> ClockScreen(
                        now = now,
                        palette = palette,
                        weather = weather,
                        battery = battery,
                        showSeconds = settings.showSeconds,
                        onRetryWeather = { weatherRefresh += 1 },
                    )

                    Screen.APPS -> AppsScreen(
                        palette = palette,
                        installed = installed,
                        loading = appsLoading,
                        webApps = settings.webApps,
                        connections = settings.connections,
                        catalogs = catalogs,
                        builds = builds,
                        buildsState = buildsState,
                        showBuilds = settings.showBuilds,
                        installedPackages = installed.map { it.packageName }.toSet() + context.packageName,
                        installProgress = installProgress,
                        selfPackage = context.packageName,
                        onLaunch = { appsRepository.launch(it) },
                        onAppInfo = { appsRepository.openAppInfo(it) },
                        onLaunchPackage = { packageName ->
                            installed.firstOrNull { it.packageName == packageName }?.let { appsRepository.launch(it) }
                                ?: run {
                                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                    if (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        runCatching { context.startActivity(intent) }
                                    }
                                }
                        },
                        onInstallBuild = { build ->
                            scope.launch {
                                installProgress = installProgress + (build.id to 0f)
                                val result = buildInstaller.download(build) { fraction ->
                                    installProgress = installProgress + (build.id to fraction)
                                }
                                installProgress = installProgress - build.id
                                result.fold(
                                    onSuccess = { file ->
                                        buildInstaller.install(file).onFailure { error ->
                                            installMessage = "Не удалось открыть установщик: ${error.message}"
                                        }
                                    },
                                    onFailure = { error ->
                                        installMessage = "Скачать не получилось: ${error.message}"
                                    },
                                )
                            }
                        },
                        onRefreshBuilds = { buildsRefresh += 1 },
                        onOpenBuildRelease = { build -> openExternal(context, build.releaseUrl) },
                        onOpenUrl = { name, url -> openWebContainer(context, name, url) },
                        onAddWebApp = { name, url, iconUrl ->
                            scope.launch {
                                if (settings.webApps.none { it.url == url }) {
                                    preferences.setWebApps(
                                        settings.webApps +
                                            WebApp(name, url, settings.webApps.size % 6, iconUrl)
                                    )
                                }
                            }
                        },
                        onDeleteWebApp = { webApp ->
                            scope.launch {
                                preferences.setWebApps(settings.webApps.filterNot { it.url == webApp.url })
                            }
                        },
                        onRefreshCatalog = { connection ->
                            scope.launch {
                                catalogs = catalogs + (connection.id to CatalogState.Loading)
                                val state = CatalogLoader.load(connection)
                                catalogs = catalogs + (connection.id to state)
                            }
                        },
                        onRefresh = { appsRefresh += 1 },
                        onBack = { screen = Screen.CLOCK },
                    )

                    Screen.AI -> AiScreen(
                        palette = palette,
                        character = character,
                        skills = enabledSkills,
                        aiConnection = aiConnection,
                        messages = chat,
                        busy = aiBusy,
                        status = aiStatus,
                        memoryCount = settings.facts.size,
                        memoryNames = Memory.name(settings.facts),
                        onSend = sendToCharacter,
                        onClear = { chat = emptyList() },
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onBack = { screen = Screen.CLOCK },
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        palette = palette,
                        settings = settings,
                        isDefaultLauncher = isDefaultLauncher,
                        onSaverMinutes = { minutes ->
                            scope.launch { preferences.setScreensaverMinutes(minutes) }
                        },
                        onShowSeconds = { show ->
                            scope.launch { preferences.setShowSeconds(show) }
                        },
                        onRegenerateBackground = {
                            scope.launch { preferences.setBackgroundSeed(settings.backgroundSeed + 1) }
                        },
                        onOpenHomeSettings = {
                            appsRepository.openHomeSettings()
                        },
                        onOpenDreamSettings = {
                            if (!appsRepository.openDreamSettings()) {
                                appsRepository.openHomeSettings()
                            }
                        },
                        onAddWebApp = { name, url ->
                            scope.launch {
                                if (settings.webApps.none { it.url == url }) {
                                    preferences.setWebApps(
                                        settings.webApps + WebApp(name, url, settings.webApps.size % 6)
                                    )
                                }
                            }
                        },
                        onDeleteWebApp = { webApp ->
                            scope.launch {
                                preferences.setWebApps(settings.webApps.filterNot { it.url == webApp.url })
                            }
                        },
                        onUpdateWebApp = { original, updated ->
                            scope.launch {
                                preferences.setWebApps(
                                    settings.webApps.map { if (it.url == original.url) updated else it }
                                )
                            }
                        },
                        onSaveConnection = { connection ->
                            scope.launch {
                                preferences.setConnections(
                                    settings.connections.filterNot { it.id == connection.id } + connection
                                )
                            }
                        },
                        onDeleteConnection = { connection ->
                            scope.launch {
                                preferences.setConnections(settings.connections.filterNot { it.id == connection.id })
                            }
                        },
                        onSelectCharacter = { id ->
                            scope.launch { preferences.setCharacterId(id) }
                        },
                        onSaveSkills = { state ->
                            scope.launch { preferences.setSkills(state) }
                        },
                        onShowBuilds = { show ->
                            scope.launch { preferences.setShowBuilds(show) }
                        },
                        onSaveBuildsUrl = { url ->
                            scope.launch { preferences.setBuildsUrl(url) }
                            buildsRefresh += 1
                        },
                        onRefreshBuilds = { buildsRefresh += 1 },
                        onRememberChat = { remember ->
                            scope.launch { preferences.setRememberChat(remember) }
                        },
                        onAddFact = { fact ->
                            scope.launch { preferences.setMemory(Memory.learn(settings.facts, fact)) }
                        },
                        onDeleteFact = { id ->
                            scope.launch { preferences.setMemory(Memory.remove(settings.facts, id)) }
                        },
                        onClearMemory = {
                            scope.launch { preferences.setMemory(emptyList()) }
                        },
                        onBack = { screen = Screen.CLOCK },
                    )
                }
            }

            if (!saverActive && screen == Screen.CLOCK) {
                ClockFooter(
                    palette = palette,
                    left = if (settings.screensaverMinutes > 0) "Заставка через ${settings.screensaverMinutes} мин" else "Заставка выключена",
                    right = if (weather is WeatherUiState.Ready) "Погода синхронизирована" else "Автономный режим",
                )
            }
        }

        if (saverActive) {
            SaverExitButton(
                palette = palette,
                onExit = {
                    registerInteraction()
                    saverActive = false
                },
            )
        }

        installMessage?.let { message ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { installMessage = null },
                title = { Text("Установка приложения") },
                text = { Text(message) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { installMessage = null }) {
                        Text("Понятно")
                    }
                },
                containerColor = if (palette.isNight) Color(0xFF0E1B22) else Color.White,
                titleContentColor = palette.textPrimary,
                textContentColor = palette.textPrimary,
            )
        }
    }
}

@Composable
private fun ClockHeader(
    palette: LumenPalette,
    subtitle: String,
    onMenuAction: (Screen) -> Unit,
    onStartSaver: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            HeaderIconButton(palette = palette, onClick = { menuOpen = true }) {
                LumenMark(palette)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Главный экран") },
                    leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onMenuAction(Screen.CLOCK)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Мои приложения") },
                    leadingIcon = { Icon(Icons.Filled.GridView, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onMenuAction(Screen.APPS)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Персонаж ИИ") },
                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onMenuAction(Screen.AI)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Настройки") },
                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onMenuAction(Screen.SETTINGS)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Заставка") },
                    leadingIcon = { Icon(Icons.Filled.Fullscreen, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onStartSaver()
                    },
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "ЛЮМЕН",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                letterSpacing = 1.sp,
            )
            Text(
                text = subtitle.uppercase(),
                fontSize = 9.5.sp,
                letterSpacing = 1.sp,
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.weight(1f))
        HeaderIconButton(palette = palette, onClick = onStartSaver) {
            Icon(
                Icons.Filled.Fullscreen,
                contentDescription = "Режим заставки",
                tint = palette.textPrimary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun ClockFooter(
    palette: LumenPalette,
    left: String,
    right: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(left.uppercase(), fontSize = 9.sp, letterSpacing = 0.8.sp, color = palette.textSecondary)
        Text(right.uppercase(), fontSize = 9.sp, letterSpacing = 0.8.sp, color = palette.textSecondary)
    }
}

@Composable
private fun SaverExitButton(
    palette: LumenPalette,
    onExit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = if (palette.isNight) Color(0x33101820) else Color(0x40FFFFFF),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (palette.isNight) Color(0x24FFFFFF) else Color(0x33FFFFFF),
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(14.dp)
                .size(44.dp)
                .clickable(onClick = onExit),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Выйти из заставки",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = "Коснитесь крестика, чтобы вернуться",
            fontSize = 9.sp,
            color = palette.textSecondary.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 18.dp),
        )
    }
}

@Composable
private fun HeaderIconButton(
    palette: LumenPalette,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (palette.isNight) Color(0x14FFFFFF) else Color(0x57FFFFFF))
            .border(1.dp, palette.panelBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Небольшой знак «Люмена»: дуга и точка, как в оригинальном меню. */
@Composable
private fun LumenMark(palette: LumenPalette) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        drawArc(
            color = if (palette.isNight) Color(0xFF67E8F9) else Color(0xFF0EA5B7),
            startAngle = -40f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(w * 0.2f, h * 0.16f),
            size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.66f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
        )
        drawCircle(
            color = Color(0xFFF0529A),
            radius = w * 0.085f,
            center = Offset(w * 0.34f, h * 0.84f),
        )
    }
}

/** Характер персонажа плюс подключённые навыки; для действий — строгий JSON-протокол. */
private fun buildCharacterPrompt(
    character: Character,
    skills: List<Skill>,
    facts: List<MemoryFact>,
): String = buildString {
    append("Ты — ${character.name}, ${character.tagline}. ${character.style} ")
    append("Отвечай по-русски, коротко и по делу.")
    append(Memory.promptBlock(facts))
    if (skills.isNotEmpty()) {
        append("\n\nПодключённые навыки:")
        skills.forEach { skill ->
            if (skill.prompt.isNotBlank()) append("\n- ${skill.name}: ${skill.prompt}")
        }
    }
    if (skills.any { it.id == "android-device" }) {
        append(
            "\n\nТы умеешь действовать на телефоне. Если нужен действие — ответь ровно одной строкой JSON, " +
                "без markdown и без пояснений, например:\n" +
                "{\"action\":\"alarm\",\"hour\":7,\"minute\":0,\"text\":\"Подъём\"}\n" +
                "Доступные action: alarm (hour, minute, text), notification (title, text), " +
                "clipboard (text), open_app (target — название приложения), open_url (url), toast (text).\n" +
                "Если действие не нужно — отвечай обычным текстом."
        )
    }
}

/** Добавляет «Запомнил: …», если модель не сказала об этом сама. */
private fun withMemoryNote(reply: String, note: String?): String {
    if (note == null) return reply
    val lower = reply.lowercase()
    val acknowledged = listOf("запомн", "записал", "помню", "буду помнить", "усвоил")
        .any { lower.contains(it) }
    return if (acknowledged) reply else "$note\n\n$reply"
}

private fun openExternal(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openWebContainer(context, url, url) }
}

private fun openWebContainer(context: Context, name: String, url: String) {    context.startActivity(
        Intent(context, WebAppActivity::class.java)
            .putExtra(WebAppActivity.EXTRA_URL, url)
            .putExtra(WebAppActivity.EXTRA_TITLE, name)
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.isOnline(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}