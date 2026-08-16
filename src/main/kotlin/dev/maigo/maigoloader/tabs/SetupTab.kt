package dev.maigo.maigoloader.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.maigo.maigoloader.Prefs
import dev.maigo.maigoloader.utils.AppTheme
import dev.maigo.maigoloader.utils.BevelButton
import dev.maigo.maigoloader.ytdownload.CommandManager
import kotlinx.coroutines.launch
import java.io.File
import java.net.SocketImpl
import javax.swing.JFileChooser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupTab(onProgress: (String) -> Unit) {
    var folderPath by remember { mutableStateOf(Prefs.downloadFolder) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Fila 1 —
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(28.dp) // ancho fijo para todos los elementos de la fila
        ) {
            Text(
                text = "Download folder:",
                color = AppTheme.TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.width(105.dp)
            )

            BasicTextField(
                value = folderPath,
                onValueChange = {
                    folderPath = it
                    Prefs.downloadFolder = it
                },
                textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(AppTheme.TextPrimary),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .border(1.dp, AppTheme.Border2)
                    .background(AppTheme.Background2),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
                    ) {
                        if (folderPath.isEmpty()) {
                            Text("Select a folder...", color = AppTheme.TextSecondary, fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                }
            )

            BevelButton(
                text = "Browse",
                modifier = Modifier.padding(start = 6.dp),
                onClick = {
                    // Selector de carpetas de Java Swing
                    val chooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        dialogTitle = "Select download folder..."
                    }

                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        folderPath = chooser.selectedFile.absolutePath
                        Prefs.downloadFolder = folderPath
                        Prefs.ytDlpFolder = folderPath
                        Prefs.ffmpegFolder = folderPath
                    }
                }
            )
        }

        // Fila 2: Descargar dependencias
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(28.dp)
        ) {
            Text(
                text = "Dependencies:",
                color = AppTheme.TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.width(105.dp)
            )

            BevelButton(
                text = "Download yt-dlp",
                onClick = {
                    scope.launch {
                        if (folderPath.isEmpty()) {
                            onProgress("Select a folder first")
                            return@launch // equivalente al "return;"
                        }
                        val dest = "$folderPath${File.separator}yt-dlp.exe"
                        CommandManager.downloadFile(
                            url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe",
                            destPath = dest,
                            onProgress = { onProgress(it) }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            BevelButton(
                text = "Download ffmpeg",
                onClick = {
                    scope.launch {
                        if (folderPath.isEmpty()) {
                            onProgress("Select a folder first")
                            return@launch
                        }
                        CommandManager.downloadAndExtractFfmpeg(
                            destFolder = folderPath,
                            onProgress = { onProgress(it) }
                        )
                    }
                }
            )
        }
    }
}