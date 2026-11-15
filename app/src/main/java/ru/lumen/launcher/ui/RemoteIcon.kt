package ru.lumen.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.lumen.launcher.data.IconCache

/**
 * Иконка веб-приложения: настоящая (манифест PWA или favicon) с падением
 * на цветную плитку с буквой. Используется во всех списках.
 */
@Composable
fun RemoteIcon(
    iconUrl: String?,
    appUrl: String?,
    name: String,
    tint: Color,
    size: Dp = 44.dp,
    corner: Dp = 14.dp,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, iconUrl, appUrl) {
        value = withContext(Dispatchers.IO) {
            IconCache.load(context, iconUrl?.takeIf { it.isNotBlank() } ?: appUrl)
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(tint.copy(alpha = 0.18f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = name,
                modifier = Modifier.size(size - 12.dp).clip(RoundedCornerShape(corner - 4.dp)),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = tint,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}