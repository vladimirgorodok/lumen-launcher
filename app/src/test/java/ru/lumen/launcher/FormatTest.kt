package ru.lumen.launcher

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.lumen.launcher.util.DayPeriod
import ru.lumen.launcher.util.Season
import ru.lumen.launcher.util.WeatherKind
import ru.lumen.launcher.util.clockText
import ru.lumen.launcher.util.dayPeriod
import ru.lumen.launcher.util.formatDate
import ru.lumen.launcher.util.pad2
import ru.lumen.launcher.util.seasonFor
import ru.lumen.launcher.util.temperatureLabel
import ru.lumen.launcher.util.weatherKindFromCode
import java.time.LocalDate

class FormatTest {

    @Test
    fun padsSingleDigits() {
        assertEquals("00", pad2(0))
        assertEquals("07", pad2(7))
        assertEquals("42", pad2(42))
    }

    @Test
    fun clockTextMatchesLumenReadout() {
        val text = clockText(9, 5, 3)
        assertEquals("09", text.hours)
        assertEquals("05", text.minutes)
        assertEquals("03", text.seconds)
    }

    @Test
    fun dateUsesDayMonthYear() {
        assertEquals("01.02.2026", formatDate(LocalDate.of(2026, 2, 1)))
        assertEquals("31.12.2025", formatDate(LocalDate.of(2025, 12, 31)))
    }

    @Test
    fun dayPeriodBoundaries() {
        assertEquals(DayPeriod.NIGHT, dayPeriod(4))
        assertEquals(DayPeriod.MORNING, dayPeriod(5))
        assertEquals(DayPeriod.MORNING, dayPeriod(10))
        assertEquals(DayPeriod.DAY, dayPeriod(11))
        assertEquals(DayPeriod.DAY, dayPeriod(16))
        assertEquals(DayPeriod.EVENING, dayPeriod(17))
        assertEquals(DayPeriod.EVENING, dayPeriod(21))
        assertEquals(DayPeriod.NIGHT, dayPeriod(22))
    }

    @Test
    fun seasonFlipsForSouthernHemisphere() {
        assertEquals(Season.AUTUMN, seasonFor(10, latitude = 55.0))
        assertEquals(Season.SPRING, seasonFor(10, latitude = -33.0))
        assertEquals(Season.WINTER, seasonFor(1, latitude = 55.0))
    }

    @Test
    fun weatherCodesMapToLumenKinds() {
        assertEquals(WeatherKind.CLEAR, weatherKindFromCode(0))
        assertEquals(WeatherKind.PARTLY_CLOUDY, weatherKindFromCode(2))
        assertEquals(WeatherKind.FOG, weatherKindFromCode(45))
        assertEquals(WeatherKind.RAIN, weatherKindFromCode(63))
        assertEquals(WeatherKind.SNOW, weatherKindFromCode(75))
        assertEquals(WeatherKind.STORM, weatherKindFromCode(99))
        assertEquals(WeatherKind.UNKNOWN, weatherKindFromCode(1234))
    }

    @Test
    fun temperatureKeepsSign() {
        assertEquals("+21°", temperatureLabel(20.6))
        assertEquals("0°", temperatureLabel(0.2))
        assertEquals("-5°", temperatureLabel(-4.6))
    }
}