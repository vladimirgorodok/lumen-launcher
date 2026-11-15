package ru.lumen.launcher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lumen.launcher.data.Character
import ru.lumen.launcher.data.ChatMessage
import ru.lumen.launcher.data.Connection
import ru.lumen.launcher.data.Skill

/**
 * Вкладка персонажа: два режима — текст с картинкой и голос,
 * когда виден только говорящий инопланетянин.
 */
@Composable
fun AiScreen(
    palette: LumenPalette,
    character: Character,
    skills: List<Skill>,
    aiConnection: Connection?,
    messages: List<ChatMessage>,
    busy: Boolean,
    status: String?,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var voiceMode by rememberSaveable { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var spokenCount by remember { mutableIntStateOf(messages.size) }

    val voice = rememberVoiceController { text -> onSend(text) }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) voice.listen() }

    LaunchedEffect(character.id) { voice.setPitch(character.pitch) }

    // В голосовом режиме персонаж сам озвучивает новые ответы.
    LaunchedEffect(messages.size, voiceMode) {
        if (!voiceMode || messages.size <= spokenCount) {
            spokenCount = messages.size
            return@LaunchedEffect
        }
        val last = messages.lastOrNull()
        if (last != null && last.role == "assistant") voice.speak(last.content)
        spokenCount = messages.size
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
                    text = character.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Text(
                    text = "${character.tagline} · навыков: ${skills.size}",
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = palette.textSecondary,
                )
            }
            IconButton(onClick = { voice.stopSpeaking(); voice.stopListening() }) {
                Icon(Icons.Filled.Stop, contentDescription = "Замолчать", tint = palette.textPrimary)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Delete, contentDescription = "Очистить диалог", tint = palette.textPrimary)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Настройки персонажа", tint = palette.textPrimary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModePill(
                text = "Текст",
                selected = !voiceMode,
                palette = palette,
                onClick = { voiceMode = false },
                modifier = Modifier.weight(1f),
            )
            ModePill(
                text = "Голос",
                selected = voiceMode,
                palette = palette,
                onClick = { voiceMode = true },
                modifier = Modifier.weight(1f),
            )
        }

        if (aiConnection == null) {
            NoConnectionCard(palette = palette, onOpenSettings = onOpenSettings)
        }

        if (voiceMode) {
            VoicePanel(
                palette = palette,
                character = character,
                speaking = voice.speaking,
                listening = voice.listening,
                partial = voice.partial,
                busy = busy,
                status = status,
                lastReply = messages.lastOrNull { it.role == "assistant" }?.content.orEmpty(),
                onMic = {
                    if (voice.listening) voice.stopListening()
                    else if (voice.hasPermission()) voice.listen()
                    else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AlienCharacter(
                        character = character,
                        speaking = voice.speaking,
                        listening = voice.listening,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(if (voice.speaking || voice.listening) 170.dp else 140.dp),
                    )
                }

                if (messages.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Поздоровайтесь с ${character.name}.\n" +
                                "Персонаж умеет ставить будильник, показывать уведомления,\n" +
                                "копировать текст и открывать приложения.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = palette.textSecondary,
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(messages.size) { index ->
                            val message = messages[index]
                            MessageBubble(
                                palette = palette,
                                message = message,
                                characterColor = character.body,
                                onSpeak = { voice.speak(message.content) },
                            )
                        }
                    }
                }

                if (busy) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color = palette.accentText,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = status ?: "${character.name} думает…",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Спросите ${character.name}…", color = palette.textSecondary) },
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    )
                    Spacer(Modifier.width(6.dp))
                    RoundIconButton(
                        palette = palette,
                        icon = Icons.Filled.Mic,
                        description = "Сказать голосом",
                        active = voice.listening,
                        onClick = {
                            if (voice.listening) voice.stopListening()
                            else if (voice.hasPermission()) voice.listen()
                            else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                        },
                    )
                    Spacer(Modifier.width(6.dp))
                    RoundIconButton(
                        palette = palette,
                        icon = Icons.Filled.Send,
                        description = "Отправить",
                        enabled = input.isNotBlank() && !busy,
                        onClick = {
                            val text = input.trim()
                            if (text.isNotEmpty()) {
                                input = ""
                                onSend(text)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModePill(
    text: String,
    selected: Boolean,
    palette: LumenPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.accentText.copy(alpha = 0.16f) else palette.panel)
            .border(
                1.dp,
                if (selected) palette.accentText.copy(alpha = 0.5f) else palette.panelBorder,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
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

@Composable
private fun VoicePanel(
    palette: LumenPalette,
    character: Character,
    speaking: Boolean,
    listening: Boolean,
    partial: String,
    busy: Boolean,
    status: String?,
    lastReply: String,
    onMic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AlienCharacter(
            character = character,
            speaking = speaking,
            listening = listening,
            modifier = Modifier.size(260.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = when {
                listening -> partial.ifBlank { "Слушаю…" }
                speaking -> "${character.name} говорит…"
                busy -> status ?: "Думаю…"
                else -> "Нажмите и скажите"
            },
            fontSize = 14.sp,
            color = if (listening || speaking) palette.accentText else palette.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (lastReply.isNotBlank() && !speaking) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = lastReply,
                fontSize = 12.sp,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
        }
        Spacer(Modifier.height(26.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    if (listening) character.accent.copy(alpha = 0.28f)
                    else character.body.copy(alpha = 0.22f)
                )
                .border(
                    2.dp,
                    if (listening) character.accent else character.body.copy(alpha = 0.6f),
                    CircleShape,
                )
                .clickable(onClick = onMic),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (listening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (listening) "Остановить" else "Говорить",
                tint = character.body,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun NoConnectionCard(
    palette: LumenPalette,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.panel)
            .border(1.dp, palette.panelBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accentText)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Нет подключения ИИ", fontSize = 13.sp, color = palette.textPrimary)
            Text(
                "Настройки → Подключения → ИИ: адрес, ключ и модель.",
                fontSize = 11.sp,
                color = palette.textSecondary,
            )
        }
        TextButton(onClick = onOpenSettings) { Text("Открыть") }
    }
}

@Composable
private fun MessageBubble(
    palette: LumenPalette,
    message: ChatMessage,
    characterColor: Color,
    onSpeak: () -> Unit,
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isUser) palette.accentText.copy(alpha = 0.16f) else palette.panel)
                .border(
                    1.dp,
                    if (isUser) palette.accentText.copy(alpha = 0.35f) else palette.panelBorder,
                    RoundedCornerShape(18.dp),
                )
                .clickable(enabled = !isUser, onClick = onSpeak)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (isUser || palette.isNight) palette.textPrimary else characterColor,
            )
        }
    }
}

@Composable
private fun RoundIconButton(
    palette: LumenPalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                when {
                    active -> palette.accentText.copy(alpha = 0.25f)
                    enabled -> palette.panel
                    else -> palette.panel.copy(alpha = 0.5f)
                }
            )
            .border(1.dp, palette.panelBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled || active) palette.textPrimary else palette.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
