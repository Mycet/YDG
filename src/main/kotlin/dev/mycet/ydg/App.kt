package dev.mycet.ydg

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.core.Dependencies
import dev.mycet.ydg.data.models.DownloadItem
import dev.mycet.ydg.data.models.DownloadTask
import dev.mycet.ydg.data.models.SimpleTask
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.ui.components.ActionIcon
import dev.mycet.ydg.ui.components.BevelContainer
import dev.mycet.ydg.ui.navigation.AppTab
import dev.mycet.ydg.ui.navigation.TabBar
import dev.mycet.ydg.ui.navigation.screens.MediaTab
import dev.mycet.ydg.ui.navigation.screens.SettingsTab
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
import dev.mycet.ydg.ui.theme.toDP
import dev.mycet.ydg.utils.toSVG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// 'recomposición' se le llama al llamado de una función
// La UI se dibuja llamando a funciones, si algo cambia en la UI, Compose vuelve a llamar a la función

@Composable
fun App(isSidebarExpanded: Boolean = false) {
    // 'remember' le dice a Compose que recuerde este valor entre recomposiciones
    // 'mutableStateOf' crea un estado reactivo — cuando cambia, la UI se redibuja
    val initialTab =
        if (Prefs.openOnStartup.equals("last open", ignoreCase = true)) Prefs.lastTab
        else Prefs.openOnStartup

    var selectedTab by remember { mutableStateOf(AppTab.match(initialTab)) }
    var setupWarning by remember { mutableStateOf("") }

    val downloads = remember { mutableStateListOf<DownloadItem>() }
    val scope = rememberCoroutineScope()

    // LaunchedEffect ejecuta el código cada vez que la variable pasada como argumento cambie
    // Al pasarle Unit (void), se ejecuta 1 sola vez al arrancar el programa
    LaunchedEffect(Unit) {
        val issues = Dependencies.check()
        if (issues.isNotEmpty())
            setupWarning = "⚠ " + issues.joinToString(" · ")
    }

    val customTextSelectionColors = remember {
        TextSelectionColors(
            handleColor = AppTheme.Accent, // los círculos en los extremos del texto seleccionado, que se ve en celular
            backgroundColor = AppTheme.Accent.copy(alpha = 0.35f) // fondo
        )
    }
    val appScrollbarStyle = ScrollbarStyle(
        minimalHeight = 4.dp,
        thickness = 6.dp,
        shape = Sizes.Round8,
        hoverDurationMillis = 200,
        unhoverColor = AppTheme.Border,
        hoverColor = AppTheme.BorderHover
//        unhoverColor = AppTheme.BorderHover.copy(alpha = 0.8f),
//        hoverColor = AppTheme.Accent
    )

    CompositionLocalProvider(
        LocalTextSelectionColors provides customTextSelectionColors,
        LocalScrollbarStyle provides appScrollbarStyle,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.Background)              // El marco exterior
                    .padding(top = 4.dp, bottom = 4.dp, end = 12.dp)      // El "grosor" del marco
            ) {
                // Barra de tabs
                TabBar(
                    selectedTab = selectedTab,
                    isExpanded = isSidebarExpanded,
                    onTabSelected = {
                        selectedTab = it
                        Prefs.lastTab = it.name
                    },
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    // Contenido según el tab seleccionado
                    Box(
                        modifier = Modifier
                            .weight(1f) // ocupa todo el espacio restante
                            .fillMaxSize()
                            .background(AppTheme.Background)
                            .padding(2.dp)
                    ) {
                        when (selectedTab) {
                            AppTab.MEDIA -> MediaTab(scope, onNewDownload = { title ->
                                val task = DownloadTask(title)
                                downloads.add(task)
                                task
                            })

                            AppTab.SETTINGS -> SettingsTab(scope, onNewTask = { title ->
                                val task = SimpleTask(title)
                                downloads.add(task)
                                task
                            })
                        }
                    }

                    // Barra inferior de errores
                    if (setupWarning.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(AppTheme.Background)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {

                            Spacer(Modifier.weight(1f))
                            Text(
                                text = setupWarning,
                                color = Color.Red,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            DownloadQueueOverlay(
                downloads,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, bottom = 16.dp, end = 24.dp)
            )
        }
    }
}

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
                LaunchedEffect(task.isCompleted, task.hasError) {
                    if (task.isCompleted || task.hasError) {
                        delay(7.seconds)
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
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
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
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (task.isCompleted) {
                                    if (task.sizeText.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource("check".toSVG()),
                                                contentDescription = "CompletedCheck",
                                                tint = AppTheme.Green,
                                                modifier = Modifier.size(Sizes.FieldFontSize.toDP())
                                            )

                                            val finalSize = task.sizeText.substringAfter("/ ").ifEmpty { task.sizeText }
                                            Text(
                                                text = "Completed · ~${finalSize}",
                                                color = AppTheme.Font2,
                                                fontSize = Sizes.TooltipFont
                                            )
                                        }
                                    }
                                } else {
                                    val details = buildList {
                                        if (task.sizeText.isNotEmpty()) add(task.sizeText)
                                        if (task.speed.isNotEmpty()) add(task.speed)
                                        if (task.eta.isNotEmpty()) add("ETA: ${task.eta}")
                                    }.joinToString(" · ")
                                    if (details.isNotEmpty()) {
                                        Text(
                                            text = details,
                                            color = AppTheme.Font2,
                                            fontSize = Sizes.TooltipFont,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val statusStr = when {
                                        task.hasError -> "Error"
                                        task.isCompleted -> "Show in folder"
                                        task.statusPhase.isNotEmpty() -> task.statusPhase
                                        else -> "${(task.progress * 100).toInt()}%"
                                    }
                                    Text(
                                        text = statusStr,
                                        color = if (task.hasError) AppTheme.Red else AppTheme.Font1,
                                        fontSize = Sizes.TooltipFont,
                                        modifier = if (task.isCompleted) {
                                            Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                openTaskInExplorer(task)
                                            }
                                        } else Modifier
                                    )
                                    if (task.isCompleted) {
                                        ActionIcon(
                                            iconName = "folder", description = "showInFolder", iconSize = Sizes.TooltipIconSize, padding = 2.dp, shape = Sizes.Round4
                                        ) { openTaskInExplorer(task) }
                                    }
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

                            LinearProgressIndicator(
                                progress = if (task.isCompleted) 1f else task.progress,
                                modifier = Modifier.fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = progressAnimation,
                                backgroundColor = if (isHovered) AppTheme.Sunken.copy(alpha = 0.60f) else AppTheme.Sunken,
                            )
                        }
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
                .fillMaxWidth()
                .padding(end = 4.dp),
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
                    text = "+$queuedCount ${if (queuedCount == 1) "task" else "tasks"}",
                    color = AppTheme.Font2,
                    fontSize = Sizes.TooltipFont,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

private fun openTaskInExplorer(task: DownloadItem) {
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