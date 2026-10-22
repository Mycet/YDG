package dev.mycet.ydg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.ui.components.TopIcon
import dev.mycet.ydg.ui.theme.AppTheme
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Toolkit

// Yt-Dlp Gui
fun main() {
    System.setProperty("kotlinx.coroutines.swing", "true")
    System.setProperty("skiko.renderApi", "OPENGL")

    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 720.dp)
        var isSidebarExpanded by remember { mutableStateOf(Prefs.isSidebarExpanded) }
        var isMaximized by remember { mutableStateOf(false) }
        var normalBounds by remember { mutableStateOf<Rectangle?>(null) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "YDG v0.1.0",
            state = windowState,
            undecorated = true,
            resizable = !isMaximized,
            icon = painterResource("icons/app_icon.png")
        ) {
            window.minimumSize = Dimension(1280, 720)
            window.background = java.awt.Color(0x1C, 0x1C, 0x1C)

            val focusManager = LocalFocusManager.current

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.Background)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() }
            ) {
                WindowDraggableArea {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(AppTheme.Background),
                        verticalAlignment = CenterVertically
                    ) {
                        TopIcon(
                            iconName = "top_expand",
                            description = "TopExpand"
                        ) {
                            isSidebarExpanded = !isSidebarExpanded
                            Prefs.isSidebarExpanded = isSidebarExpanded
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Botón Minimizar
                        TopIcon(
                            iconName = "top_minimize",
                            description = "Minimize"
                        ) {
                            windowState.isMinimized = true
                        }
                        // Botón Pantalla Completa
                        TopIcon(
                            iconName = if (windowState.placement == WindowPlacement.Floating) "top_maximize" else "top_restore",
                            description = if (windowState.placement == WindowPlacement.Floating) "Maximize" else "Restore"
                        ) {
                            if (isMaximized) {
                                normalBounds?.let { window.bounds = it }
                                isMaximized = false
                            } else {
                                normalBounds = window.bounds
                                val gc = window.graphicsConfiguration
                                val bounds = gc.bounds
                                val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
                                window.setBounds(
                                    bounds.x + insets.left,
                                    bounds.y + insets.top,
                                    bounds.width - insets.left - insets.right,
                                    bounds.height - insets.top - insets.bottom
                                )
                                isMaximized = true
                            }
                        }
                        // Botón Cerrar
                        TopIcon(
                            hoveredColor = Color.Red,
                            iconName = "cross",
                            description = "Close"
                        ) {
                            exitApplication()
                        }
                    }
                }

                App(isSidebarExpanded = isSidebarExpanded)
            }
        }
    }
}