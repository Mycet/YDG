package dev.mycet.ydg

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// Yt-Dlp Gui
fun main() {
    System.setProperty("kotlinx.coroutines.swing", "true")
    System.setProperty("skiko.renderApi", "OPENGL")

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "YDG v1.0",
            state = rememberWindowState(width = 1280.dp, height = 720.dp)
        ) {
            App()
        }
    }
}