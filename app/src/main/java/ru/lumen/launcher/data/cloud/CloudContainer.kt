package ru.lumen.launcher.data.cloud

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.lumen.launcher.data.LumenPreferences

/**
 * Общие объекты обмена с Яндекс Диском.
 *
 * Живут столько же, сколько приложение: выгрузка не должна обрываться,
 * когда пользователь ушёл с экрана настроек.
 */
class CloudContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Скоуп приложения: обмен продолжается при уходе с экрана. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val preferences = LumenPreferences(appContext)
    val transfer = LumenTransfer(preferences)
    val archive = LumenArchive(LumenArchive.directory(appContext), transfer)
    val sync = CloudSync(preferences, transfer)
    val coordinator = CloudSyncCoordinator(preferences, transfer, sync, archive, appScope)

    companion object {
        @Volatile
        private var instance: CloudContainer? = null

        /** Единственный контейнер на приложение. */
        fun of(context: Context): CloudContainer =
            instance ?: synchronized(this) {
                instance ?: CloudContainer(context.applicationContext).also { instance = it }
            }
    }
}
