package dev.mycet.ydg.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.objects.Dependencies
import dev.mycet.ydg.objects.Prefs
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.utils.BevelButton
import dev.mycet.ydg.ytdownload.CommandManager
import javafx.application.Platform
import javafx.stage.DirectoryChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CompletableFuture

object NativeFolderPicker {
    private var started = false

    @Synchronized
    private fun ensureStarted() {
        if (!started) {
            runCatching { Platform.startup {} }
            Platform.setImplicitExit(false)
            started = true
        }
    }

    fun pickFolder(title: String, initialDir: String? = null): String? {
        ensureStarted();
        val future = CompletableFuture<String>() // al parecer es una variable que es independiente de la asincronia de hilos
        // es decir, deja que el resto de la funcion trabaje sin bloquear el hilo esperando por un valor
        // en este caso el valor es la carpeta que llegue a seleccionar

        Platform.runLater {
            val chooser = DirectoryChooser().apply { //otra cosa de manejar orden en hilos, aun que este especificamente espera a que se cumpla
                // algo dentro del hilo para ser ejecutado

                this.title = title
                initialDir?.takeIf { File(it).isDirectory }?.let { path ->
                    initialDirectory = File(path)
                }
            }

            future.complete(chooser.showDialog(null)?.absolutePath) //asignar valor al future (direccion de la carpeta)
        }
        return future.get()
        // osea la secuencia es future ->, platform LLAMADO pero no ejecutado, future.complete, y luego la ejecucion del platform
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupTab(scope: CoroutineScope, onProgress: (String) -> Unit) {
    var downloadFolder by remember { mutableStateOf(Prefs.downloadFolder) }
    var ytDlpFolder by remember { mutableStateOf(Prefs.ytDlpFolder) }
    var ffmpegFolder by remember { mutableStateOf(Prefs.ffmpegFolder) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Fila 1 — Downloads Folder
        FolderRow(
            label = "Downloads folder:",
            value = downloadFolder,
            onValueChange = { downloadFolder = it; Prefs.downloadFolder = it },
            onClear = { downloadFolder = ""; Prefs.downloadFolder = "" },
            onLocate = {
                NativeFolderPicker.pickFolder(
                    title = "Set downloads destination folder: ",
                    initialDir = downloadFolder
                )?.let {
                    downloadFolder = it
                    Prefs.downloadFolder = it
                }
            },
            placeholder = "Select downloads destination folder..."
        )

        // Fila 2 — YT-DLP folder
        FolderRow(
            label = "YT-DLP:",
            value = ytDlpFolder,
            onValueChange = { ytDlpFolder = it; Prefs.ytDlpFolder = it },
            onClear = { ytDlpFolder = ""; Prefs.ytDlpFolder = "" },
            onLocate = {
                NativeFolderPicker.pickFolder(
                    title = "Set downloads destination folder: ",
                    initialDir = ytDlpFolder
                )?.let {
                    ytDlpFolder = it
                    Prefs.ytDlpFolder = it
                }
            },
            onDownload = {
                scope.launch {
                    if (ytDlpFolder.isEmpty()) {
                        onProgress("Select a folder first")
                        return@launch // equivalente al "return;"
                    }

                    if (Dependencies.ytDlpExists()) { // Ya existe, actualizar
                        onProgress("yt-dlp found, updating...")
                        CommandManager.updateYtDlp(onProgress = { onProgress(it) })
                    } else {
                        val dest = "$ytDlpFolder${File.separator}yt-dlp.exe"
                        CommandManager.downloadFile(
                            url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe",
                            destPath = dest,
                            onProgress = { onProgress(it) }
                        )
                    }
                }
            },
            placeholder = "Select yt-dlp destination folder..."
        )

        // Fila 3 — ffmpeg folder
        FolderRow(
            label = "ffmpeg:",
            value = ffmpegFolder,
            onValueChange = { ffmpegFolder = it; Prefs.ffmpegFolder = it },
            onClear = { ffmpegFolder = ""; Prefs.ffmpegFolder = "" },
            onLocate = {
                NativeFolderPicker.pickFolder(
                    title = "Set downloads destination folder: ",
                    initialDir = ffmpegFolder
                )?.let {
                    ffmpegFolder = it
                    Prefs.ffmpegFolder = it
                }
            },
            onDownload = {
                scope.launch {
                    if (ffmpegFolder.isEmpty()) {
                        onProgress("Select a folder first")
                        return@launch
                    }
                    CommandManager.downloadAndExtractFfmpeg(
                        destFolder = ffmpegFolder,
                        onProgress = { onProgress(it) }
                    )
                }
            },
            placeholder = "Select ffmpeg destination folder..."
        )
    }
}

@Composable
fun FolderRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onLocate: () -> Unit,
    onDownload: (() -> Unit)? = null,  // null = no muestra el botón
    placeholder: String = "Select a folder..."
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(28.dp)
    ) {
        Text(text = label, color = AppTheme.TextPrimary, fontSize = 13.sp, modifier = Modifier.width(110.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
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
                    if (value.isEmpty()) {
                        Text(placeholder, color = AppTheme.TextSecondary, fontSize = 13.sp)
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.width(2.dp))
        BevelButton(icon = Icons.Default.Clear, text = "Clear", onClick = onClear)
        Spacer(modifier = Modifier.width(2.dp))
        BevelButton(icon = Icons.Default.FolderOpen, text = "Locate", onClick = onLocate)

        if (onDownload != null) {
            Spacer(modifier = Modifier.width(2.dp))
            BevelButton(icon = Icons.Default.Download, text = "Download", onClick = onDownload)
        }
    }
}