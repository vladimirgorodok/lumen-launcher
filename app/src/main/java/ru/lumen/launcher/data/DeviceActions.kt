package ru.lumen.launcher.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Реальные действия персонажа на телефоне. */
class DeviceActions(
    context: Context,
    private val appsRepository: AppsRepository,
) {

    private val appContext = context.applicationContext

    suspend fun execute(command: DeviceCommand): String = withContext(Dispatchers.Main) {
        when (command.action) {
            "alarm" -> setAlarm(command)
            "notification" -> notify(command)
            "clipboard" -> copy(command)
            "open_app" -> openApp(command)
            "open_url" -> openUrl(command)
            "toast" -> {
                Toast.makeText(appContext, command.text.ifBlank { command.title }, Toast.LENGTH_SHORT).show()
                "Показал сообщение на экране."
            }
            else -> "Не знаю такое действие: ${command.action}"
        }
    }

    private fun setAlarm(command: DeviceCommand): String {
        val (hour, minute) = resolveTime(command) ?: return "Не понял время будильника"
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            if (command.text.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, command.text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            appContext.startActivity(intent)
            "Будильник на %02d:%02d поставлен.".format(hour, minute)
        }.getOrElse { "Не удалось поставить будильник: ${it.message}" }
    }

    private fun resolveTime(command: DeviceCommand): Pair<Int, Int>? {
        command.hour?.let { hour -> return hour.coerceIn(0, 23) to (command.minute ?: 0).coerceIn(0, 59) }
        val raw = command.time.trim()
        val match = Regex("(\\d{1,2})[:.\\s-]?(\\d{2})?").find(raw) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }

    private fun notify(command: DeviceCommand): String {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return "Уведомления недоступны"
        val channelId = "lumen-character"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Персонаж", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(command.title.ifBlank { "Люмен" })
            .setContentText(command.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(command.text))
            .setAutoCancel(true)
            .build()
        return runCatching {
            manager.notify(System.currentTimeMillis().toInt(), notification)
            "Уведомление показано."
        }.getOrElse { "Не удалось показать уведомление: ${it.message}" }
    }

    private fun copy(command: DeviceCommand): String {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return "Буфер обмена недоступен"
        clipboard.setPrimaryClip(ClipData.newPlainText("Люмен", command.text))
        return "Скопировал в буфер: ${command.text.take(60)}"
    }

    private suspend fun openApp(command: DeviceCommand): String {
        val target = command.target.ifBlank { command.text }
        if (target.isBlank()) return "Не понял, какое приложение открыть"
        val apps = appsRepository.load()
        val match = apps.firstOrNull { it.label.equals(target, ignoreCase = true) }
            ?: apps.firstOrNull { it.label.contains(target, ignoreCase = true) }
            ?: return "Приложение «$target» не найдено"
        appsRepository.launch(match)
        return "Открываю ${match.label}."
    }

    private fun openUrl(command: DeviceCommand): String {
        val url = command.url.ifBlank { command.target.ifBlank { command.text } }
        if (url.isBlank()) return "Не понял ссылку"
        val normalized = if (url.startsWith("http")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(normalized))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            "Открываю $normalized"
        }.getOrElse { "Не удалось открыть ссылку: ${it.message}" }
    }
}