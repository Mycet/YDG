package dev.mycet.ydg

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.objects.*
import dev.mycet.ydg.tabs.AppTab
import dev.mycet.ydg.tabs.MediaTab
import dev.mycet.ydg.tabs.SetupTab
import dev.mycet.ydg.tabs.TabBar
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.utils.ui.BevelContainer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// 'recomposición' se le llama al llamado de una función
// La UI se dibuja llamando a funciones, si algo cambia en la UI, Compose vuelve a llamar a la función

@Composable
fun App() {
    // 'remember' le dice a Compose que recuerde este valor entre recomposiciones
    // 'mutableStateOf' crea un estado reactivo — cuando cambia, la UI se redibuja
    var selectedTab by remember { mutableStateOf (
        AppTab.match(if (Prefs.startupTab == "Last Used") Prefs.lastActiveTab else Prefs.startupTab)
    )}
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
                onTabSelected = { selectedTab = it },
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
                        AppTab.VIDEO -> MediaTab(false, scope, onNewDownload = { title ->
                            val task = DownloadTask(title)
                            downloads.add(task)
                            task
                        })

                        AppTab.AUDIO -> MediaTab(isAudio = true, scope, onNewDownload = { title ->
                            val task = DownloadTask(title)
                            downloads.add(task)
                            task
                        })

                        AppTab.SETUP -> SetupTab(scope, onNewTask = { title ->
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
                            color = AppTheme.ProgressBarError,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        DownloadQueueOverlay(downloads, modifier = Modifier.align(Alignment.TopEnd).padding(bottom = 16.dp, end = 24.dp))
    }
}

@Composable
fun DownloadQueueOverlay(downloads: MutableList<DownloadItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        downloads.takeLast(4).forEach { task ->
            // key(task.id) es para que cada task tenga su propio id, para que no se repitan ni se asigne por el orden
            key(task.id) {
                LaunchedEffect(task.isDone, task.hasError) {
                    if (task.isDone || task.hasError) {
                        delay(7.seconds)
                        task.isVisible = false
                        delay(0.5.seconds)
                        downloads.remove(task)
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
                    val alpha by animateFloatAsState(targetValue = if (isHovered) 1f else if (task.isDone) 0.5f else 0.75f)

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
                            Text(
                                text = task.title,
                                color = AppTheme.TextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val statusStr = when {
                                    task.hasError -> "Error"
                                    task.isDone -> "Done"
                                    else -> "${(task.progress * 100).toInt()}%"
                                }
                                Text(text = statusStr, color = AppTheme.TextSecondary, fontSize = 10.sp)

                                if (task.sizeText.isNotEmpty()) {
                                    if (task.isDone)
                                        Text(
                                            text = "Final: ~${task.sizeText}",
                                            color = AppTheme.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    else
                                        Text(
                                            text = "${task.sizeText} - ${task.speed}",
                                            color = AppTheme.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                }
                            }

                            LinearProgressIndicator(
                                progress = if (task.isDone) 1f else task.progress,
                                modifier = Modifier.fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (task.hasError) AppTheme.ProgressBarError else AppTheme.Accent,
                                backgroundColor = if (isHovered) AppTheme.Contrast.copy(alpha = 0.60f) else AppTheme.Contrast,
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
           .drawBehind {
                        val s = 0.5.dp.toPx() // offset necesario para trazar la linea
                        val color = AppTheme.Border1
                        val w = s * 2 // grosor de la linea
                        val tabPx = tabWidth.toPx()
                        val selectedIndex = selectedTab.ordinal
                        val gapStart = tabPx * selectedIndex
                        val gapEnd = gapStart + tabPx

                        // Izquierdo, derecho, inferior
                        drawLine(color, Offset(s, 0f), Offset(s, size.height), w)
                        drawLine(color, Offset(size.width - s, 0f), Offset(size.width - s, size.height), w)
                        // Superior izquierdo
                        if (gapStart > 0f) // Solo si el tab seleccionado no es el primero, que está en el borde izquierdo
                            drawLine(color, Offset(s, s), Offset(gapStart, s), w)
                        // Superior derecho
                        drawLine(color, Offset(gapEnd, s), Offset(size.width - s, s), w)
                    }
* */