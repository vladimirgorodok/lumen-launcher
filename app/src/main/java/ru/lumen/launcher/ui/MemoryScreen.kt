package ru.lumen.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lumen.launcher.data.MemoryFact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Полноэкранный список того, что персонаж помнит о собеседнике. */
@Composable
fun MemoryScreen(
    palette: LumenPalette,
    facts: List<MemoryFact>,
    rememberChat: Boolean,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    onRememberChat: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale("ru")) }

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
                    "Память персонажа",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Text(
                    "фактов: ${facts.size} · персонаж опирается на них в разговоре",
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = palette.textSecondary,
                )
            }
            Icon(
                Icons.Filled.Psychology,
                contentDescription = null,
                tint = palette.accentText,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.panel)
                    .border(1.dp, palette.panelBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Помнить диалог", fontSize = 13.sp, color = palette.textPrimary)
                    Text(
                        "выключено — после перезапуска лаунчера диалог начнётся заново, факты останутся",
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                    )
                }
                Switch(
                    checked = rememberChat,
                    onCheckedChange = onRememberChat,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.accentText,
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Что запомнить") },
                    placeholder = { Text("например: меня зовут Владимир", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onAdd(draft)
                            draft = ""
                        }
                    },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить", tint = palette.accentText)
                }
            }

            Spacer(Modifier.height(6.dp))

            if (facts.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Пока пусто.\n\nСкажите персонажу «запомни, что меня зовут …»\n" +
                            "или просто расскажите о себе — «я живу в Москве»,\n" +
                            "«мой день рождения 04.11.1978».\n\nИли добавьте запись здесь.",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(facts.sortedByDescending { it.createdAt }, key = { it.id }) { fact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.panel)
                                .border(1.dp, palette.panelBorder, RoundedCornerShape(16.dp))
                                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fact.text,
                                    fontSize = 13.sp,
                                    color = palette.textPrimary,
                                )
                                Text(
                                    text = buildString {
                                        append(dateFormat.format(Date(fact.createdAt)))
                                        if (fact.source == "ai") append(" · записал персонаж")
                                    },
                                    fontSize = 10.sp,
                                    color = palette.textSecondary,
                                )
                            }
                            IconButton(onClick = { onDelete(fact.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Удалить факт",
                                    tint = palette.textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Память хранится только на телефоне.",
                    fontSize = 10.sp,
                    color = palette.textSecondary,
                )
                TextButton(
                    onClick = { confirmClear = true },
                    enabled = facts.isNotEmpty(),
                ) {
                    Text("Очистить всё", color = palette.accentText)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить память?") },
            text = { Text("Персонаж забудет все ${facts.size} фактов о вас. Диалог останется.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        confirmClear = false
                    },
                ) { Text("Забыть всё", color = palette.accentText) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Отмена") }
            },
            containerColor = if (palette.isNight) Color(0xFF0E1B22) else Color.White,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textPrimary,
        )
    }
}
