package dev.mycet.ydg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.utils.ui.ActionIcon

// Yt-Dlp Gui
fun main() {
    System.setProperty("kotlinx.coroutines.swing", "true")
    System.setProperty("skiko.renderApi", "OPENGL")

    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 720.dp)

        Window(
            onCloseRequest = ::exitApplication,
            title = "YDG v0.1.0",
            state = windowState,
            undecorated = true
        ) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {

                WindowDraggableArea {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(AppTheme.Background),
                        verticalAlignment = CenterVertically
                    ) {
                        ActionIcon(
                            iconName = "top_expand",
                            description = "TopExpand",
                        ) { }

                        Spacer(modifier = Modifier.weight(1f))

                        // Botón Minimizar
                        ActionIcon(
                            iconName = "top_minimize",
                            description = "Minimize",
                        ) {
                            windowState.isMinimized = true
                        }
                        // Botón Pantalla Completa
                        ActionIcon(
                            iconName = if (windowState.placement == WindowPlacement.Floating) "top_maximize" else "top_restore",
                            description = if (windowState.placement == WindowPlacement.Floating) "Maximize" else "Restore",
                        ) {
                            windowState.placement = if (windowState.placement == WindowPlacement.Floating) {
                                WindowPlacement.Maximized
                            } else {
                                WindowPlacement.Floating
                            }
                        }
                        // Botón Cerrar
                        ActionIcon(
                            hoveredColor = Color.Red,
                            iconName = "top_close",
                            description = "Close",
                        ) {
                            exitApplication()
                        }
                    }
                }

                App()
            }
        }
    }
}