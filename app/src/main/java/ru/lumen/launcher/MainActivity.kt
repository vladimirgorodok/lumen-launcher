package ru.lumen.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.lumen.launcher.data.cloud.CloudContainer
import ru.lumen.launcher.ui.LumenRoot

class MainActivity : ComponentActivity() {

    private val cloud by lazy { CloudContainer.of(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LumenRoot() }
    }

    override fun onStart() {
        super.onStart()
        // Вернулись на экран: если есть невыгруженные правки — выгружаем.
        cloud.coordinator.onAppForeground()
    }

    override fun onStop() {
        // Уходим в фон: выгрузка не должна потеряться.
        cloud.coordinator.onAppStop()
        super.onStop()
    }
}
