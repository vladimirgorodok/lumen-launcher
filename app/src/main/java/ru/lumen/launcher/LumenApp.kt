package ru.lumen.launcher

import android.app.Application
import ru.lumen.launcher.data.cloud.CloudContainer

/**
 * Приложение «Люмен».
 *
 * Здесь запускается тихий автообмен с Яндекс Диском: сверка при запуске,
 * выгрузка изменений в фоне и выгрузка при уходе приложения с экрана.
 */
class LumenApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CloudContainer.of(this).coordinator.start()
    }
}
