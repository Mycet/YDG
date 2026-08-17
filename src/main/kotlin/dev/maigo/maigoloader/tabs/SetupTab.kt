package dev.maigo.maigoloader.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.maigo.maigoloader.objects.Prefs
import dev.maigo.maigoloader.objects.AppTheme
import dev.maigo.maigoloader.objects.Dependencies
import dev.maigo.maigoloader.utils.BevelButton
import dev.maigo.maigoloader.ytdownload.CommandManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser

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
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Select downloads destination folder..."
                }

                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    downloadFolder = chooser.selectedFile.absolutePath
                    Prefs.downloadFolder = downloadFolder
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
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Select yt-dlp destination folder..."
                }

                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    ytDlpFolder = chooser.selectedFile.absolutePath
                    Prefs.ytDlpFolder = ytDlpFolder
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
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Select ffmpeg destination folder..."
                }

                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    ffmpegFolder = chooser.selectedFile.absolutePath
                    Prefs.ffmpegFolder = ffmpegFolder
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