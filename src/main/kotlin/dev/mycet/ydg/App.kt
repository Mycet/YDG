package dev.mycet.ydg

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.core.Dependencies
import dev.mycet.ydg.data.models.DownloadItem
import dev.mycet.ydg.data.models.DownloadTask
import dev.mycet.ydg.data.models.SimpleTask
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.ui.components.DownloadQueueOverlay
import dev.mycet.ydg.ui.navigation.AppTab
import dev.mycet.ydg.ui.navigation.TabBar
import dev.mycet.ydg.ui.navigation.screens.MediaTab
import dev.mycet.ydg.ui.navigation.screens.SettingsTab
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes

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
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, bottom = 16.dp, end = 38.dp) // 64 16 24
            )
        }
    }
}