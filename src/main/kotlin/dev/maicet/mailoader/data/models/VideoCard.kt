package dev.mycet.ydg.data.models

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    video: VideoInfo,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val thumbnail = rememberThumbnail(video.thumbnail, video.id)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var isTruncated by remember(video.title) { mutableStateOf(false) }

    val alpha by animateFloatAsState(targetValue = when {
        isSelected -> 1f
        isHovered -> 0.85f
        else -> 0.5f
    })

    Column(
        modifier = Modifier
            .width(120.dp)
            .fillMaxHeight()
            .background(AppTheme.Background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick() }
            )
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(Sizes.Round12)
                .background(AppTheme.Surface)
                .alpha(alpha)
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        val titleContent = @Composable {
            Text(
                text = video.title,
                color = if (isSelected) AppTheme.Font1 else if (isHovered) AppTheme.Font1.copy(alpha = 0.85f) else AppTheme.Font2,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    isTruncated = textLayoutResult.hasVisualOverflow
                }
            )
        }

        if (isTruncated) {
            TooltipArea(
                delayMillis = 400,
                tooltip = {
                    Box(
                        modifier = Modifier
                            .background(AppTheme.Sunken, Sizes.Round8)
                            .border(1.dp, AppTheme.Border, Sizes.Round8)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = video.title,
                            color = AppTheme.Font1,
                            fontSize = Sizes.TooltipFont
                        )
                    }
                },
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.TopCenter,
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset(0.dp, (-12).dp) // Desplaza 6dp hacia arriba del texto
                )
            ) {
                titleContent()
            }
        } else {
            titleContent()
        }
    }
}