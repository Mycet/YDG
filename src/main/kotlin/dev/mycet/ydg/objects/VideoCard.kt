package dev.mycet.ydg.objects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
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
            .background(AppTheme.Background)
            .clickable { onClick() }
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
                    .background(AppTheme.Background)
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
    }
}