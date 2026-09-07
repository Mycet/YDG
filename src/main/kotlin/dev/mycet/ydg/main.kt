package dev.mycet.ydg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import dev.mycet.ydg.utils.TopIcon

// Yt-Dlp Gui
fun main() {
    System.setProperty("kotlinx.coroutines.swing", "true")
    System.setProperty("skiko.renderApi", "OPENGL")

    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 720.dp)

        Window(
            onCloseRequest = ::exitApplication,
            title = "YDG v0.1",
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
                        TopIcon(
                            iconPath = "icons/top_expand.svg",
                            description = "TopExpand",
                        ) {
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Botón Minimizar
                        TopIcon(
                            iconPath = "icons/top_minimize.svg",
                            description = "Minimize",
                        ) {
                            windowState.isMinimized = true
                        }
                        // Botón Pantalla Completa
                        TopIcon(
                            iconPath = if (windowState.placement == WindowPlacement.Floating) "icons/top_maximize.svg" else "icons/top_restore.svg",
                            description = if (windowState.placement == WindowPlacement.Floating) "Maximize" else "Restore",
                        ) {
                            windowState.placement = if (windowState.placement == WindowPlacement.Floating) {
                                WindowPlacement.Maximized
                            } else {
                                WindowPlacement.Floating
                            }
                        }
                        // Botón Cerrar
                        TopIcon(
                            hoveredColor = Color.Red,
                            iconPath = "icons/top_close.svg",
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