package ru.lumen.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

data class InstalledApp(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: ImageBitmap?,
)

class AppsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    suspend fun load(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        val collator = Collator.getInstance(Locale("ru", "RU"))
        val seen = HashSet<String>()
        resolved.asSequence()
            .mapNotNull { info ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName ?: return@mapNotNull null
                if (packageName == appContext.packageName) return@mapNotNull null
                val component = ComponentName(packageName, activityInfo.name) ?: return@mapNotNull null
                val key = component.flattenToString()
                if (!seen.add(key)) return@mapNotNull null
                val label = info.loadLabel(packageManager)?.toString()?.trim()
                    ?.ifBlank { packageName }
                    ?: packageName
                val icon = runCatching {
                    info.loadIcon(packageManager).toBitmap(width = 144, height = 144).asImageBitmap()
                }.getOrNull()
                InstalledApp(label, packageName, component, icon)
            }
            .sortedWith { left, right -> collator.compare(left.label, right.label) }
            .toList()
    }

    fun launch(app: InstalledApp) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { appContext.startActivity(intent) }
    }

    fun openAppInfo(app: InstalledApp) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", app.packageName, null))
        runCatching { appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** true, если «Люмен» уже выбран домашним экраном. */
    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = runCatching { packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) }
            .getOrNull() ?: return false
        return resolved.activityInfo?.packageName == appContext.packageName
    }

    fun openHomeSettings() {
        val intents = listOf(
            Intent(android.provider.Settings.ACTION_HOME_SETTINGS),
            Intent(android.provider.Settings.ACTION_SETTINGS),
        )
        for (intent in intents) {
            if (runCatching { appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }

    fun openDreamSettings(): Boolean {
        val intent = Intent("android.settings.DREAM_SETTINGS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }
}