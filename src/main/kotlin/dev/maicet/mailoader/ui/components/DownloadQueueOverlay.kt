package dev.mycet.ydg.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.mycet.ydg.data.models.DownloadItem
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
import dev.mycet.ydg.ui.theme.toDP
import dev.mycet.ydg.utils.fieldStyle
import dev.mycet.ydg.utils.lerpColor
import dev.mycet.ydg.utils.toSVG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadQueueOverlay(downloads: MutableList<DownloadItem>, modifier: Modifier = Modifier) {
    val maxVisible = 4
    val visibleTasks = downloads.take(maxVisible)
    val queuedCount = (downloads.size - visibleTasks.size).coerceAtLeast(0)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.width(300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        visibleTasks.forEach { task ->
            // key(task.id) es para que cada task tenga su propio id, para que no se repitan ni se asigne por el orden
            key(task.id) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                LaunchedEffect(task.isCompleted, task.hasError, isHovered) {
                    if (isHovered) return@LaunchedEffect

                    val delay = if (task.isCompleted) 7.seconds else if (task.hasError) 15.seconds else (-1).seconds
                    if (delay.isPositive()) {
                        delay(delay)
                        task.isVisible = false
                        delay(0.5.seconds)
                        if (downloads.contains(task)) downloads.remove(task)
                    }
                }

                val transitionState = remember { MutableTransitionState(false) }
                transitionState.targetState = task.isVisible

                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn() + expandVertically(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(400))
                ) {
                    val alpha by animateFloatAsState(targetValue = if (isHovered) 1f else if (task.isCompleted) 0.5f else 0.75f)

                    BevelContainer(modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .hoverable(interactionSource = interactionSource) // lo hace hoverable
                        .alpha(alpha) // le aplica la transparencia dependiente de si se le pone el mouse encima o no
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.title,
                                    color = AppTheme.Font1,
                                    fontSize = Sizes.TooltipFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                                )

                                val closeInteraction = remember { MutableInteractionSource() }
                                val isCloseHovered by closeInteraction.collectIsHoveredAsState()

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(Sizes.Round4)
                                        .background(if (isCloseHovered) AppTheme.SurfaceHigh else Color.Transparent)
                                        .clickable(
                                            interactionSource = closeInteraction,
                                            indication = null
                                        ) {
                                            if (!task.isCompleted && !task.hasError)
                                                task.cancel()

                                            task.isVisible = false
                                            scope.launch {
                                                delay(400.milliseconds)
                                                if (downloads.contains(task)) downloads.remove(task)
                                            }
                                        }
                                ) {
                                    Icon(
                                        painter = painterResource("cross".toSVG()),
                                        contentDescription = if (task.isCompleted || task.hasError) "Close" else "Cancel",
                                        tint = if (isCloseHovered) {
                                            if (task.isCompleted || task.hasError) AppTheme.Font1 else AppTheme.Red
                                        } else AppTheme.Font2.copy(0.6f),
                                        modifier = Modifier.size(Sizes.TooltipIconSize)
                                    )
                                }
                            }

                            val progressColor = when {
                                task.hasError -> AppTheme.Red
                                task.isCompleted -> AppTheme.Green
                                else -> lerpColor(AppTheme.Yellow, AppTheme.Green, task.progress.coerceIn(0f, 1f))
                            }
                            val progressAnimation by animateColorAsState(
                                targetValue = progressColor,
                                animationSpec = tween(250)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when {
                                    task.isCompleted && task.sizeText.isNotEmpty() -> {
                                        val finalSize = task.sizeText.substringAfter("/ ").ifEmpty { task.sizeText }

                                        CardState(
                                            task,
                                            "check", "completedCheck", AppTheme.Green,
                                            "Completed · $finalSize",
                                            null,
                                            "folder", "showInFolder",
                                            "Show in folder",
                                        ) { openTaskInExplorer(task) }
                                    }

                                    task.hasError -> {
                                        val displayError = task.errorMessage.ifEmpty { "Unknown error" }
                                        CardState(
                                            task,
                                            "cross", "errorCross", AppTheme.Red,
                                            "Error",
                                            {
                                                TooltipArea(
                                                    delayMillis = 300,
                                                    tooltip = {
                                                        Box(
                                                            modifier = Modifier.fieldStyle()
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                .widthIn(max = 280.dp)
                                                        ) {
                                                            Text(
                                                                text = displayError,
                                                                color = AppTheme.Font1,
                                                                fontSize = Sizes.TooltipFont
                                                            )
                                                        }
                                                    },
                                                    tooltipPlacement = TooltipPlacement.ComponentRect(
                                                        anchor = Alignment.TopCenter,
                                                        alignment = Alignment.BottomCenter,
                                                        offset = DpOffset(0.dp, (-8).dp)
                                                    )
                                                ) {
                                                    Text(
                                                        text = "Error · $displayError",
                                                        color = AppTheme.Font2,
                                                        fontSize = Sizes.TooltipFont,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            "reload", "errorRetry",
                                            "Retry",
                                        ) { task.retry() }
                                    }

                                    else -> {
                                        val startDetails = task.statusPhase.ifEmpty {
                                            buildList {
                                                if (task.sizeText.isNotEmpty()) add(task.sizeText)
                                                if (task.speed.isNotEmpty()) add(task.speed)
                                                if (task.eta.isNotEmpty()) add("ETA: ${task.eta}")
                                            }.joinToString(" · ")
                                        }

                                        val endStatus = "${(task.progress * 100).toInt()}%"

                                        CardState(
                                            task = task,
                                            startText = startDetails,
                                            endText = endStatus
                                        )
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = if (task.isCompleted) 1f else task.progress,
                                modifier = Modifier.fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (task.progress == 0f) AppTheme.Border else progressAnimation,
                                backgroundColor = if (isHovered) AppTheme.Sunken.copy(alpha = 0.60f) else AppTheme.Sunken,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = queuedCount > 0,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(AppTheme.Surface.copy(0.85f), Sizes.Round4)
                        .border(1.dp, AppTheme.Border, Sizes.Round4)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "+$queuedCount more",
                        color = AppTheme.Font2,
                        fontSize = Sizes.TooltipFont,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.CardState( // al hacerlo RowScope puedo usar `Modifier.weight`
    task: DownloadItem,
    startIcon: String? = null, startIconId: String? = null, startIconTint: Color = AppTheme.Font1,
    startText: String? = null,
    startContent: (@Composable () -> Unit)? = null,
    endIcon: String? = null, endIconId: String? = null,
    endText: String? = null,
    endClickable: ((DownloadItem) -> Unit)? = null
) {
    Row(
        modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (startIcon != null)
            Icon(
                painter = painterResource(startIcon.toSVG()),
                contentDescription = startIconId ?: startIcon,
                tint = startIconTint,
                modifier = Modifier.size(Sizes.FieldFontSize.toDP())
            )

        if (startContent != null) {
            startContent()
        } else if (startText != null) {
            Text(
                text = startText,
                color = AppTheme.Font2,
                fontSize = Sizes.TooltipFont
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (endText != null)
            Text(
                text = endText,
                color = if (endClickable != null) AppTheme.Font1 else AppTheme.Font2,
                fontSize = Sizes.TooltipFont,
                modifier = if (endClickable != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { endClickable.invoke(task) }
                } else Modifier
            )

        if (endIcon != null)
        ActionIcon(
            iconName = endIcon,
            description = endIconId ?: endIcon,
            iconSize = Sizes.TooltipIconSize,
            padding = 2.dp,
            shape = Sizes.Round4
        ) { endClickable?.invoke(task) }
    }
}


fun openTaskInExplorer(task: DownloadItem) {
    try {
        val defaultFolder = when {
            task.title.contains("yt-dlp", ignoreCase = true) -> File(Prefs.ytDlpFolder)
            task.title.contains("ffmpeg", ignoreCase = true) -> File(Prefs.ffmpegFolder)
            task.title.contains("deno", ignoreCase = true) -> File(Prefs.denoFolder)
            else -> File(Prefs.downloadFolder)
        }
        val cleanTitle = task.title.replace("\"", "'").replace(Regex("[\\\\/:*?<>|]"), "_").trim()
        val targetFile = if (defaultFolder.exists() && defaultFolder.isDirectory && cleanTitle.length >= 3) {
            defaultFolder.listFiles()?.firstOrNull { file ->
                file.isFile &&
                        file.name.startsWith(cleanTitle, ignoreCase = true) &&
                        !file.name.endsWith(".part") &&
                        !file.name.endsWith(".ytdl")
            }
        } else null
        if (targetFile != null && targetFile.exists()) {
            ProcessBuilder("explorer.exe", "/select,", targetFile.absolutePath).start()
        } else if (defaultFolder.exists()) {
            ProcessBuilder("explorer.exe", defaultFolder.absolutePath).start()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}