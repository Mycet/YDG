package dev.mycet.ydg.objects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.utils.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.net.URI
import kotlin.math.roundToInt

@Composable
fun rememberThumbnail(url: String, videoId: String = ""): ImageBitmap? {
    val resolvedUrl = when {
        url.isNotEmpty() -> url
        videoId.isNotEmpty() -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
        else -> return null
    }
    var bitmap by remember(resolvedUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(resolvedUrl) {
        if (resolvedUrl.isEmpty()) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            try {
                val bytes = URI(resolvedUrl).toURL().readBytes()
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            } catch (ex: Exception) {
                null
            }
        }
    }
    return bitmap
}

@Composable
fun VideoCard(video: VideoInfo, onClick: () -> Unit = {}) {
    val thumbnail = rememberThumbnail(video.thumbnail, video.id)

    Column(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .background(AppTheme.Surface)
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        // Thumbnail
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(AppTheme.Surface2)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = video.title,
            color = AppTheme.TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))

        // segundos a mm:ss
        val totalSecs = video.duration.roundToInt()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        Text(
            text = "%d:%02d".format(mins, secs),
            color = AppTheme.TextSecondary,
            fontSize = 10.sp,
        )
    }
}