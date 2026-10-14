package dev.mycet.ydg.ui.navigation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.core.Dependencies
import dev.mycet.ydg.data.models.SimpleTask
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.downloader.CommandManager
import dev.mycet.ydg.ui.components.ActionIcon
import dev.mycet.ydg.ui.components.BevelContainer
import dev.mycet.ydg.ui.components.Modifiers.inputFieldSize
import dev.mycet.ydg.ui.components.Modifiers.inputFieldStyle
import dev.mycet.ydg.ui.components.SimpleDropdown
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
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
    var rememberMediaSettings by remember { mutableStateOf(Prefs.rememberMediaSettings) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .padding(horizontal = 40.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SetupTitle(title = "General", topSpacer = false)

            SetupRow {
                SettingsFolder( // Downloads Folder
                    title = "Downloads folder:",
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
            }

            SetupRow {
                var expanded by remember { mutableStateOf(false) }
                SettingsDropdown( // Cookies
                    title = "Cookies",
                    description = "Use your browser’s login for age-restricted or members-only videos.",
                    value = Prefs.browserForCookies.lowercase().replaceFirstChar { it.uppercase() },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    options = listOf("none", "chrome", "firefox", "brave", "edge", "opera", "vivaldi"/*, "safari"*/), // safari es solo para macOS
                    onSelect = { Prefs.browserForCookies = it },
                    dropdownLabel = { it -> it.replaceFirstChar { it.uppercase() } },
                )
            }

            SetupRow {
                var expanded by remember { mutableStateOf(false) }
                SettingsDropdown(
                    title = "Open on startup",
                    value = Prefs.openOnStartup.lowercase().replaceFirstChar { it.uppercase() },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    options = listOf("VIDEO", "AUDIO", "SETUP", "LAST USED"),
                    onSelect = { Prefs.openOnStartup = it },
                    dropdownLabel = { it -> it.replaceFirstChar { it.uppercase() } },
                )
            }

            SetupRow(bottomSpacer = false) {
                SettingsSwitch(
                    title = "Remember media settings",
                    checked = rememberMediaSettings,
                    onCheckedChange = { rememberMediaSettings = it; Prefs.rememberMediaSettings = it },
                    description = "Reuse your last Media tab choices for new links."
                )
            }

            SetupTitle(title = "Dependencies")

            SetupRow {
                SettingsFolder( // YT-DLP
                    title = "YT-DLP",
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
            }

            SetupRow {
                SettingsFolder( // ffmpeg
                    title = "ffmpeg",
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
            }

            SetupRow {
                SettingsFolder( // Deno
                    title = "deno",
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
        }
    }
}

// Extra Composables
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
@Composable
fun SetupTitle(
    title: String,
    topSpacer: Boolean = true, bottomSpacer: Boolean = true
) {
    if (topSpacer) Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = title,
        fontSize = 16.sp,
        color = AppTheme.Font1,
        fontWeight = FontWeight.Bold
    )
    if (bottomSpacer) Spacer(modifier = Modifier.height(4.dp))
}
@Composable
fun SetupRow(
    topSpacer: Boolean = true, bottomSpacer: Boolean = true,
    content: @Composable () -> Unit
) {
    if (topSpacer) Spacer(modifier = Modifier.height(4.dp))

    content()

    if (bottomSpacer) {
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(thickness = 1.dp, color = AppTheme.Border)
    }
}

@Composable
fun RowScope.SettingLabel(
    title: String,
    description: String = ""
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = title,
            color = AppTheme.Font1,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                color = AppTheme.Font2,
                fontSize = 11.sp
            )
        }
    }
}

@Composable @OptIn(ExperimentalFoundationApi::class)
fun SettingsFolder(
    title: String, description: String = "",
    value: String,
    onValueChange: (String) -> Unit,
    onLocate: () -> Unit,
    onDownload: (() -> Unit)? = null,  // null = no muestra el botón
    placeholder: String = "Select a folder..."
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Sizes.TextFieldHeight)
    ) {
        SettingLabel(title = title, description = description)

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = AppTheme.Font1, fontSize = 13.sp),
            cursorBrush = SolidColor(AppTheme.Font1),
            singleLine = true,
            modifier = Modifier.inputFieldSize(width = 400.dp).inputFieldStyle(),
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
                            Text(placeholder, color = AppTheme.Font2, fontSize = 13.sp)
                        }
                        innerTextField()
                    }

                    ActionIcon(
                        iconName = "folder",
                        iconSize = 13.dp,
                        shape = CircleShape,
                        modifier = Modifier.size(Sizes.TextFieldHeight)
                    ) { onLocate() }
                }
            }
        )

        if (onDownload != null) {
            Spacer(modifier = Modifier.width(2.dp))
            TooltipArea(
                tooltip = {
                    Box(
                        modifier = Modifier.inputFieldStyle().padding(8.dp)
                    ) {
                        Text("Download or update", color = AppTheme.Font1, fontSize = 13.sp)
                    }
                },
                tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset((8).dp, (-8).dp), alignment = Alignment.TopEnd),
                delayMillis = 200
            ) {
                ActionIcon(
                    iconName = "download",
                    iconSize = 13.dp,
                    shape = CircleShape,
                    modifier = Modifier.size(Sizes.TextFieldHeight)
                ) { onDownload() }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingText(
    id: String = "",
    title: String, description: String = "",
    value: String, placeholder: String = "",
    onValueChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // defaultMinSize permite que la fila crezca si hay 2 líneas de texto
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Sizes.TextFieldHeight)
    ) {
        SettingLabel(title = title, description = description)

        var isFocused by remember { mutableStateOf(false) }

        BevelContainer(
            border = if (isFocused) AppTheme.Accent else AppTheme.Border,
            modifier = Modifier
                .width(400.dp)
                .height(Sizes.TextFieldHeight)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = AppTheme.Font1, fontSize = 13.sp),
                cursorBrush = SolidColor(AppTheme.Font1),
                singleLine = true,
                modifier = Modifier.inputFieldSize().inputFieldStyle(),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                    ) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = AppTheme.Font2, fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsDropdown(
    title: String,
    value: String, description: String = "",
    modifier: Modifier = Modifier.height(Sizes.TextFieldHeight),
    options: List<T>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    dropdownLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Sizes.TextFieldHeight)
    ) {
        SettingLabel(title = title, description = description)

        SimpleDropdown(
            value = value,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            options = options,
            onSelect = onSelect,
            label = dropdownLabel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsSwitch(
    id: String = "",
    title: String, description: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Sizes.TextFieldHeight)
    ) {
        SettingLabel(title = title, description = description)

        Switch(
            modifier = Modifier.height(22.dp).width(40.dp),
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.Accent,
                checkedTrackColor = AppTheme.Sunken,
                checkedBorderColor = AppTheme.Sunken,
                uncheckedThumbColor = AppTheme.Font2,
                uncheckedTrackColor = AppTheme.Sunken,
                uncheckedBorderColor = AppTheme.Sunken
            )
        )
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