package ru.lumen.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lumenDataStore by preferencesDataStore(name = "lumen_prefs")

data class LumenSettings(
    val screensaverMinutes: Int = 2,
    val showSeconds: Boolean = true,
    val backgroundSeed: Int = 1,
    val webApps: List<WebApp> = SeedWebApps,
    val connections: List<Connection> = emptyList(),
    val weatherCache: String? = null,
)

class LumenPreferences(private val context: Context) {

    private object Keys {
        val screensaverMinutes: Preferences.Key<Int> = intPreferencesKey("screensaver_minutes")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val backgroundSeed = intPreferencesKey("background_seed")
        val webApps = stringPreferencesKey("web_apps")
        val connections = stringPreferencesKey("connections")
        val weatherCache = stringPreferencesKey("weather_cache")
    }

    val settings: Flow<LumenSettings> = context.lumenDataStore.data.map { prefs ->
        LumenSettings(
            screensaverMinutes = prefs[Keys.screensaverMinutes] ?: 2,
            showSeconds = prefs[Keys.showSeconds] ?: true,
            backgroundSeed = prefs[Keys.backgroundSeed] ?: 1,
            webApps = decodeWebApps(prefs[Keys.webApps]),
            connections = decodeConnections(prefs[Keys.connections]),
            weatherCache = prefs[Keys.weatherCache],
        )
    }

    suspend fun setScreensaverMinutes(minutes: Int) {
        context.lumenDataStore.edit { it[Keys.screensaverMinutes] = minutes.coerceIn(0, 120) }
    }

    suspend fun setShowSeconds(show: Boolean) {
        context.lumenDataStore.edit { it[Keys.showSeconds] = show }
    }

    suspend fun setBackgroundSeed(seed: Int) {
        context.lumenDataStore.edit { it[Keys.backgroundSeed] = seed.coerceAtLeast(1) }
    }

    suspend fun setWebApps(apps: List<WebApp>) {
        context.lumenDataStore.edit { it[Keys.webApps] = encodeWebApps(apps) }
    }

    suspend fun setConnections(connections: List<Connection>) {
        context.lumenDataStore.edit { it[Keys.connections] = encodeConnections(connections) }
    }

    suspend fun setWeatherCache(raw: String?) {
        context.lumenDataStore.edit { prefs ->
            if (raw.isNullOrBlank()) prefs.remove(Keys.weatherCache) else prefs[Keys.weatherCache] = raw
        }
    }
}