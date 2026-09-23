package dev.mycet.ydg.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.objects.Dependencies
import dev.mycet.ydg.objects.Prefs
import dev.mycet.ydg.objects.SimpleTask
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.utils.Sizes
import dev.mycet.ydg.utils.ui.BevelButton
import dev.mycet.ydg.utils.ui.ActionIcon
import dev.mycet.ydg.utils.ui.CollapsibleSection
import dev.mycet.ydg.utils.ui.SimpleDropdown
import dev.mycet.ydg.utils.ui.TooltipIcon
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
fun SetupTab(scope: CoroutineScope, onNewTask: (String) -> SimpleTask) {
    var downloadFolder by remember { mutableStateOf(Prefs.downloadFolder) }
    var ytDlpFolder by remember { mutableStateOf(Prefs.ytDlpFolder) }
    var ffmpegFolder by remember { mutableStateOf(Prefs.ffmpegFolder) }
    var denoFolder by remember { mutableStateOf(Prefs.denoFolder) }

    var browserCookies by remember { mutableStateOf(Prefs.browserForCookies) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CollapsibleSection(title = "General") {

            FolderRow( // Downloads Folder
                label = "Downloads folder:",
                value = downloadFolder,
                onValueChange = { downloadFolder = it; Prefs.downloadFolder = it },
                onLocate = {
                    NativeFolderPicker.pickFolder(
                        title = "Set downloads destination folder: ",
                        initialDir = downloadFolder
                    )?.let { // .let permite hacer algo con el valor nullable devuelto si no es nulo
                        downloadFolder = it
                        Prefs.downloadFolder = it
                    }
                },
                placeholder = "Select downloads destination folder..."
            )

            SettingText( // Cookies
                label = "Cookies:",
                value = browserCookies,
                placeholder = "chrome, firefox...",
                tooltipText = "Supported browsers:\nedge, chrome, brave, firefox, opera, vivaldi, safari.\n\nLeave empty to disable.",
                onValueChange = { browserCookies = it; Prefs.browserForCookies = it }
            )

            var expandedLastActiveTab by remember { mutableStateOf(false) }
            SimpleDropdown(
                value = Prefs.lastActiveTab,
                expanded = expandedLastActiveTab,
                onExpandedChange = { expandedLastActiveTab = it },
                options = listOf("VIDEO", "AUDIO", "SETUP"),
                onSelect = { Prefs.lastActiveTab = it },
                label = { it.uppercase() }
            )
        }

        CollapsibleSection(title = "Dependencies") {

            FolderRow( // YT-DLP
                label = "YT-DLP:",
                value = ytDlpFolder,
                onValueChange = { ytDlpFolder = it; Prefs.ytDlpFolder = it },
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
                    runDownloadTask(
                        scope, onNewTask, "Downloading yt-dlp...", ytDlpFolder,
                        { ytDlpFolder = it; Prefs.ytDlpFolder = it },
                        { targetFolder, task ->
                            if (Dependencies.ytDlpExists()) { // Ya existe, actualizar
                                task.title = "Updating yt-dlp..."
                                CommandManager.updateYtDlp(onProgress = { task.speed = it })
                            } else {
                                task.title = "Downloading yt-dlp..."
                                val dest = "$targetFolder${File.separator}yt-dlp.exe"
                                CommandManager.downloadFile(
                                    url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe",
                                    destPath = dest,
                                    onProgress = { task.speed = it }
                                )
                            }
                        })
                },
                placeholder = "Select yt-dlp destination folder..."
            )


            FolderRow( // ffmpeg
                label = "ffmpeg:",
                value = ffmpegFolder,
                onValueChange = { ffmpegFolder = it; Prefs.ffmpegFolder = it },
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
                    runDownloadTask(
                        scope, onNewTask, "Downloading ffmpeg...", ffmpegFolder,
                        { ffmpegFolder = it; Prefs.ffmpegFolder = it },
                        { targetFolder, task ->
                            CommandManager.downloadFfmpeg(
                                destFolder = targetFolder,
                                onProgress = { task.speed = it }
                            )
                        })
                },
                placeholder = "Select ffmpeg destination folder..."
            )


            FolderRow( // Deno
                label = "deno:",
                value = denoFolder,
                onValueChange = { denoFolder = it; Prefs.denoFolder = it },
                onLocate = {
                    NativeFolderPicker.pickFolder(
                        title = "Set downloads destination folder: ",
                        initialDir = denoFolder
                    )?.let {
                        denoFolder = it
                        Prefs.denoFolder = it
                    }
                },
                onDownload = {
                    runDownloadTask(
                        scope, onNewTask, "Downloading deno...", denoFolder,
                        { denoFolder = it; Prefs.denoFolder = it },
                        { targetFolder, task ->
                            CommandManager.downloadDeno(
                                destFolder = targetFolder,
                                onProgress = { task.speed = it }
                            )
                        })
                },
                placeholder = "Select Deno destination folder..."
            )
        }

/*        Row(
            modifier = Modifier.fillMaxWidth().height(Sizes.TextField),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var language by remember { mutableStateOf("ENGLISH") }
            var expanded by remember { mutableStateOf(false) }

            Text(text = "Language:", color = AppTheme.TextPrimary, fontSize = 13.sp, modifier = Modifier.width(110.dp))

            SimpleDropdown(
                value = language,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                options = listOf("ENGLISH", "SPANISH"),
                onSelect = { language = it },
                label = { it.uppercase() }
            )
        }*/
    }
}

// Extra Composables
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
@Composable @OptIn(ExperimentalFoundationApi::class)
fun FolderRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onLocate: () -> Unit,
    onDownload: (() -> Unit)? = null,  // null = no muestra el botón
    placeholder: String = "Select a folder..."
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(Sizes.TextField)
    ) {
        Text(text = label, color = AppTheme.TextPrimary, fontSize = 13.sp, modifier = Modifier.width(110.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = 13.sp),
            cursorBrush = SolidColor(AppTheme.TextPrimary),
            singleLine = true,
            modifier = Modifier
                .width(400.dp)
                .height(26.dp)
                .border(1.dp, AppTheme.Border2, RoundedCornerShape(4.dp))
                .background(AppTheme.Contrast, RoundedCornerShape(4.dp)),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxHeight().weight(1f)
                    ) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = AppTheme.TextSecondary, fontSize = 13.sp)
                        }
                        innerTextField()
                    }

                    ActionIcon(iconName = "folder", iconSize = 13.dp, shape = CircleShape, modifier = Modifier.aspectRatio(1f)) { onLocate() }
                }
            }
        )

        if (onDownload != null) {
            Spacer(modifier = Modifier.width(2.dp))
            TooltipArea(
                tooltip = {
                    Box(
                        modifier = Modifier
                            .background(AppTheme.Contrast, RoundedCornerShape(4.dp))
                            .border(1.dp, AppTheme.Border2, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text("Download or update", color = AppTheme.TextPrimary, fontSize = 13.sp)
                    }
                },
                tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset((8).dp, (-8).dp), alignment = Alignment.TopEnd),
                delayMillis = 200
            ) {
                ActionIcon(iconName = "download", iconSize = 13.dp, shape = CircleShape, modifier = Modifier.aspectRatio(1f)) { onDownload() }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingText(
    id: String = "", label: String, value: String, placeholder: String = "", tooltipText: String = "",
    onValueChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(Sizes.TextField)
    ) {
        Text(text = label, color = AppTheme.TextPrimary, fontSize = 13.sp, modifier = Modifier.width(110.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = 13.sp),
            cursorBrush = SolidColor(AppTheme.TextPrimary),
            singleLine = true,
            modifier = Modifier
                .width(130.dp)
                .height(26.dp)
                .border(1.dp, AppTheme.Border2, RoundedCornerShape(4.dp))
                .background(AppTheme.Contrast, RoundedCornerShape(4.dp)),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = AppTheme.TextSecondary, fontSize = 13.sp)
                    }
                    innerTextField()
                }
            }
        )

        if (tooltipText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            TooltipIcon(
                iconName = "info", iconId = "tooltip-$id", iconSize = 16.dp, iconColor = AppTheme.Border2,
                tooltipText = tooltipText,
                tooltipModifier = Modifier.background(AppTheme.Contrast, RoundedCornerShape(4.dp))
                    .border(1.dp, AppTheme.Border2, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun PropertyDropdown(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit,
    label: (String) -> String,
    modifier: Modifier = Modifier
) {
    Row(

    ) {
/*        SimpleDropdown(
            value = Prefs.lastActiveTab,
            expanded = expandedLastActiveTab,
            onExpandedChange = { expandedLastActiveTab = it },
            options = listOf("VIDEO", "AUDIO", "SETUP"),
            onSelect = { Prefs.lastActiveTab = it },
            label = { it.uppercase() }
        )*/
    }
}


// Functions
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
private val activeDownloads = mutableSetOf<String>()

private fun runDownloadTask(
    scope: CoroutineScope,
    onNewTask: (String) -> SimpleTask,
    taskTitle: String,
    currentFolder: String,
    onFolderUpdate: (String) -> Unit,
    action: suspend (targetFolder: String, task: SimpleTask) -> Unit
) {
    if (activeDownloads.contains(taskTitle)) return
    activeDownloads.add(taskTitle)

    val task = onNewTask(taskTitle)

    scope.launch {
        try {
            val targetFolder = currentFolder.ifEmpty {
                File(System.getProperty("user.dir"), "bin").absolutePath
            }

            if (currentFolder.isEmpty())
                onFolderUpdate(targetFolder)

            File(targetFolder).mkdirs()

            action(targetFolder, task)

        } finally { // finally es un bloque que se ejecuta siempre aunque haya un error
            task.isDone = true
            task.progress = 1f
            activeDownloads.remove(taskTitle)
        }
    }
}