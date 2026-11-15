package ru.lumen.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lumen.launcher.data.WeatherSnapshot
import ru.lumen.launcher.util.clockText
import ru.lumen.launcher.util.formatDate
import ru.lumen.launcher.util.temperatureLabel
import ru.lumen.launcher.util.weatherLabel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Ready(val snapshot: WeatherSnapshot, val cached: Boolean = false) : WeatherUiState
    data class Error(val reason: String) : WeatherUiState
}

data class BatteryState(val level: Int = 100, val charging: Boolean = false)

@Composable
fun ClockScreen(
    now: LocalDateTime,
    palette: LumenPalette,
    weather: WeatherUiState,
    battery: BatteryState,
    showSeconds: Boolean,
    onRetryWeather: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = clockText(now.hour, now.minute, now.second)
    val weatherText = when (weather) {
        is WeatherUiState.Ready -> weatherLabel(weather.snapshot.kind)
        WeatherUiState.Loading -> "Обновляю погоду…"
        is WeatherUiState.Error -> "Погода недоступна"
    }

    BoxWithConstraints(modifier = modifier) {
        val narrow = maxWidth < 380.dp
        val hoursSize = if (narrow) 54.sp else 70.sp
        val minutesSize = hoursSize
        val colonSize = if (narrow) 28.sp else 38.sp
        val secondsSize = if (narrow) 30.sp else 42.sp
        val dateSize = if (narrow) 22.sp else 27.sp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(palette.panel)
                    .border(1.dp, palette.panelBorder, RoundedCornerShape(28.dp))
                    .padding(horizontal = 20.dp, vertical = 22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(Cyan, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ЛОКАЛЬНОЕ ВРЕМЯ · 24 ЧАСА",
                        color = palette.textSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(14.dp))
                TimeReadout(
                    hours = time.hours,
                    minutes = time.minutes,
                    seconds = time.seconds,
                    showSeconds = showSeconds,
                    hoursSize = hoursSize,
                    minutesSize = minutesSize,
                    colonSize = colonSize,
                    secondsSize = secondsSize,
                    palette = palette,
                )

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(if (narrow) 30.dp else 52.dp)
                            .height(1.dp)
                            .background(Coral)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = formatDate(now.toLocalDate()),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = dateSize,
                        color = palette.textPrimary,
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.BatteryFull,
                        contentDescription = null,
                        tint = if (battery.level <= 15) Coral else palette.textSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${battery.level}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = palette.textPrimary,
                    )
                    if (battery.charging) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Заряжается",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "заряд",
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (palette.isNight) Color(0x1FFFFFFF) else Color(0x141F2937))
                )
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (weather) {
                                is WeatherUiState.Ready -> temperatureLabel(weather.snapshot.temperature)
                                else -> "—"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (narrow) 30.sp else 38.sp,
                            color = palette.textPrimary,
                        )
                        Text(
                            text = weatherText,
                            fontSize = 13.sp,
                            color = palette.textPrimary.copy(alpha = 0.82f),
                        )
                        if (weather is WeatherUiState.Ready) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = weather.snapshot.place,
                                    fontSize = 11.sp,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                text = buildString {
                                    append("обновлено ")
                                    append(
                                        java.time.Instant.ofEpochMilli(weather.snapshot.updatedAt)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                                    )
                                    if (weather.cached) append(" · из кэша")
                                },
                                fontSize = 10.sp,
                                color = palette.textSecondary.copy(alpha = 0.8f),
                            )
                        } else if (weather is WeatherUiState.Error) {
                            Text(
                                text = weather.reason,
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                                maxLines = 2,
                            )
                        }
                    }
                    IconButton(onClick = onRetryWeather) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Обновить погоду",
                            tint = palette.accentText,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(palette.panel)
                    .border(1.dp, palette.panelBorder, RoundedCornerShape(28.dp))
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "АНАЛОГОВЫЙ КОНТУР",
                        color = palette.textSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                    )
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Fuchsia,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                AnalogClock(
                    now = now,
                    palette = palette,
                    modifier = Modifier.size(if (narrow) 214.dp else 246.dp),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(96.dp)
                        .height(1.dp)
                        .background(Cyan.copy(alpha = 0.55f))
                )
            }
        }
    }
}

@Composable
private fun TimeReadout(
    hours: String,
    minutes: String,
    seconds: String,
    showSeconds: Boolean,
    hoursSize: androidx.compose.ui.unit.TextUnit,
    minutesSize: androidx.compose.ui.unit.TextUnit,
    colonSize: androidx.compose.ui.unit.TextUnit,
    secondsSize: androidx.compose.ui.unit.TextUnit,
    palette: LumenPalette,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = hours,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = hoursSize,
            color = palette.textPrimary,
            textAlign = TextAlign.Start,
            lineHeight = hoursSize,
        )
        Text(
            text = ":",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = colonSize,
            color = if (palette.isNight) Color(0xFF67E8F9) else Color(0xFF0891B2),
            modifier = Modifier.padding(horizontal = 2.dp),
            lineHeight = colonSize,
        )
        Text(
            text = minutes,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = minutesSize,
            color = palette.textPrimary,
            lineHeight = minutesSize,
        )
        if (showSeconds) {
            Text(
                text = ":",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = colonSize,
                color = Fuchsia,
                modifier = Modifier.padding(horizontal = 2.dp),
                lineHeight = colonSize,
            )
            Text(
                text = seconds,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = secondsSize,
                color = palette.textPrimary.copy(alpha = 0.82f),
                modifier = Modifier.padding(bottom = 4.dp),
                lineHeight = secondsSize,
            )
        }
    }
}