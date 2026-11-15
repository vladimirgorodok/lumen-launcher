package ru.lumen.launcher.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.lumen.launcher.util.WeatherKind
import ru.lumen.launcher.util.weatherKindFromCode
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherSnapshot(
    val temperature: Double,
    val weatherCode: Int,
    val kind: WeatherKind,
    val place: String,
    val isDay: Boolean,
    val latitude: Double,
    val longitude: Double,
    val updatedAt: Long,
)

object WeatherCodec {
    fun encode(snapshot: WeatherSnapshot): String = JSONObject()
        .put("t", snapshot.temperature)
        .put("c", snapshot.weatherCode)
        .put("p", snapshot.place)
        .put("d", snapshot.isDay)
        .put("lat", snapshot.latitude)
        .put("lon", snapshot.longitude)
        .put("at", snapshot.updatedAt)
        .toString()

    fun decode(raw: String?): WeatherSnapshot? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            val code = json.optInt("c", -1)
            if (code < 0 || !json.has("t")) return null
            WeatherSnapshot(
                temperature = json.getDouble("t"),
                weatherCode = code,
                kind = weatherKindFromCode(code),
                place = json.optString("p").ifBlank { "Текущее местоположение" },
                isDay = json.optBoolean("d", true),
                latitude = json.optDouble("lat", DEFAULT_LAT),
                longitude = json.optDouble("lon", DEFAULT_LON),
                updatedAt = json.optLong("at", 0L),
            )
        }.getOrNull()
    }
}

const val DEFAULT_LAT = 55.7558
const val DEFAULT_LON = 37.6173
private const val DEFAULT_PLACE = "Москва"

private fun httpGet(url: String, timeoutMs: Int = 6500): String? {
    var connection: HttpURLConnection? = null
    return try {
        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LumenLauncher/1.0 (Android)")
        }
        if (connection.responseCode !in 200..299) return null
        connection.inputStream.bufferedReader().use { it.readText() }
    } catch (error: Exception) {
        Log.w("LumenWeather", "request failed: ${error.message}")
        null
    } finally {
        connection?.disconnect()
    }
}

object WeatherClient {

    suspend fun fetch(latitude: Double, longitude: Double, place: String?): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            val forecastUrl = buildString {
                append("https://api.open-meteo.com/v1/forecast?latitude=")
                append(latitude)
                append("&longitude=")
                append(longitude)
                append("&current=temperature_2m,weather_code,is_day&temperature_unit=celsius&timezone=auto")
            }
            val forecastRaw = httpGet(forecastUrl) ?: return@withContext null
            val forecast = runCatching {
                JSONObject(forecastRaw).getJSONObject("current")
            }.getOrNull() ?: return@withContext null

            val code = forecast.optInt("weather_code", -1)
            if (code < 0 || !forecast.has("temperature_2m")) return@withContext null

            val resolvedPlace = place ?: reverseGeocode(latitude, longitude)
            WeatherSnapshot(
                temperature = forecast.getDouble("temperature_2m"),
                weatherCode = code,
                kind = weatherKindFromCode(code),
                place = resolvedPlace,
                isDay = forecast.optInt("is_day", 1) == 1,
                latitude = latitude,
                longitude = longitude,
                updatedAt = System.currentTimeMillis(),
            )
        }

    private fun reverseGeocode(latitude: Double, longitude: Double): String {
        val url = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$latitude&longitude=$longitude&localityLanguage=ru"
        val raw = httpGet(url) ?: return DEFAULT_PLACE
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return DEFAULT_PLACE
        val locality = json.optString("city").ifBlank { json.optString("locality") }
        val region = json.optString("principalSubdivision")
        return when {
            locality.isNotBlank() && region.isNotBlank() && locality != region -> "$locality, $region"
            locality.isNotBlank() -> locality
            region.isNotBlank() -> region
            else -> json.optString("countryName").ifBlank { DEFAULT_PLACE }
        }
    }
}

class LocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun lastKnown(): Pair<Double, Double>? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Pair<Double, Double>? = null
        var bestTime = Long.MIN_VALUE
        for (provider in providers) {
            val location = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (location.time > bestTime) {
                bestTime = location.time
                best = location.latitude to location.longitude
            }
        }
        return best
    }

    fun hasPermission(): Boolean {
        val fine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

fun coordinatesLabel(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.4f, %.4f", latitude, longitude)