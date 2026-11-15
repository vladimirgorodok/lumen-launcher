package ru.lumen.launcher.util

import java.time.LocalDate
import java.util.Locale

enum class DayPeriod { MORNING, DAY, EVENING, NIGHT }

enum class Season { SPRING, SUMMER, AUTUMN, WINTER }

enum class WeatherKind {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM, UNKNOWN
}

fun pad2(value: Int): String = value.toString().padStart(2, '0')

/** Часы, минуты и секунды в том же виде, что на главном экране «Люмена». */
data class ClockText(val hours: String, val minutes: String, val seconds: String)

fun clockText(hour: Int, minute: Int, second: Int): ClockText =
    ClockText(pad2(hour), pad2(minute), pad2(second))

fun formatDate(date: LocalDate): String =
    String.format(Locale.US, "%s.%s.%d", pad2(date.dayOfMonth), pad2(date.monthValue), date.year)

fun dayPeriod(hour: Int): DayPeriod = when {
    hour in 5..10 -> DayPeriod.MORNING
    hour in 11..16 -> DayPeriod.DAY
    hour in 17..21 -> DayPeriod.EVENING
    else -> DayPeriod.NIGHT
}

fun seasonFor(month: Int, latitude: Double = 55.0): Season {
    val northernMonth = if (latitude < 0) (month + 5) % 12 + 1 else month
    return when (northernMonth) {
        3, 4, 5 -> Season.SPRING
        6, 7, 8 -> Season.SUMMER
        9, 10, 11 -> Season.AUTUMN
        else -> Season.WINTER
    }
}

fun weatherKindFromCode(code: Int): WeatherKind = when (code) {
    0 -> WeatherKind.CLEAR
    1, 2 -> WeatherKind.PARTLY_CLOUDY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    51, 53, 55, 56, 57 -> WeatherKind.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
    95, 96, 99 -> WeatherKind.STORM
    else -> WeatherKind.UNKNOWN
}

fun dayPeriodLabel(period: DayPeriod): String = when (period) {
    DayPeriod.MORNING -> "Утро"
    DayPeriod.DAY -> "День"
    DayPeriod.EVENING -> "Вечер"
    DayPeriod.NIGHT -> "Ночь"
}

fun seasonLabel(season: Season): String = when (season) {
    Season.SPRING -> "Весна"
    Season.SUMMER -> "Лето"
    Season.AUTUMN -> "Осень"
    Season.WINTER -> "Зима"
}

fun weatherLabel(kind: WeatherKind): String = when (kind) {
    WeatherKind.CLEAR -> "Ясно"
    WeatherKind.PARTLY_CLOUDY -> "Переменная облачность"
    WeatherKind.CLOUDY -> "Облачно"
    WeatherKind.FOG -> "Туман"
    WeatherKind.DRIZZLE -> "Морось"
    WeatherKind.RAIN -> "Дождь"
    WeatherKind.SNOW -> "Снег"
    WeatherKind.STORM -> "Гроза"
    WeatherKind.UNKNOWN -> "Погода недоступна"
}

fun temperatureLabel(temperature: Double): String {
    val rounded = Math.round(temperature).toInt()
    val sign = if (rounded > 0) "+" else ""
    return "$sign$rounded°"
}