package ru.lumen.launcher.service

import android.service.dreams.DreamService
import ru.lumen.launcher.ui.SaverView

/**
 * Системная заставка Android. Пользователь выбирает её в
 * «Настройки → Экран → Заставка»; включается, когда телефон на зарядке.
 */
class LumenDreamService : DreamService() {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setFullscreen(true)
        setInteractive(true)
        setScreenBright(true)

        val saverView = SaverView(this).apply {
            onExit = { finish() }
        }
        setContentView(saverView)
    }
}