package dev.mycet.ydg.ui.navigation.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.core.CommandManager
import dev.mycet.ydg.core.Dependencies
import dev.mycet.ydg.core.DependencyState
import dev.mycet.ydg.core.DependencyStatus
import dev.mycet.ydg.data.models.SimpleTask
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.ui.components.*
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
import dev.mycet.ydg.utils.fieldStyle
import dev.mycet.ydg.utils.fieldTextSize
import dev.mycet.ydg.utils.titleCase
import javafx.application.Platform
import javafx.stage.DirectoryChooser
import kotlinx.coroutines.*
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

@Composable
fun SettingsTab(scope: CoroutineScope, onNewTask: (String) -> SimpleTask) {
    val focusManager = LocalFocusManager.current

    var downloadFolder by remember { mutableStateOf(Prefs.downloadFolder) }
    var ytDlpFolder by remember { mutableStateOf(Prefs.ytDlpFolder) }
    var ffmpegFolder by remember { mutableStateOf(Prefs.ffmpegFolder) }
    var denoFolder by remember { mutableStateOf(Prefs.denoFolder) }

    var ytDlpStatus by remember { mutableStateOf(DependencyStatus(DependencyState.CHECKING, "Checking...")) }
    var ffmpegStatus by remember { mutableStateOf(DependencyStatus(DependencyState.CHECKING, "Checking...")) }
    var denoStatus by remember { mutableStateOf(DependencyStatus(DependencyState.CHECKING, "Checking...")) }

    LaunchedEffect(ytDlpFolder) {
        withContext(Dispatchers.IO) { ytDlpStatus = Dependencies.getYtDlpStatus() }
    }
    LaunchedEffect(ffmpegFolder) {
        withContext(Dispatchers.IO) { ffmpegStatus = Dependencies.getFfmpegStatus() }
    }
    LaunchedEffect(denoFolder) {
        withContext(Dispatchers.IO) { denoStatus = Dependencies.getDenoStatus() }
    }

    var rememberMediaSettings by remember { mutableStateOf(Prefs.rememberMediaSettings) }
    var rememberLastLink by remember { mutableStateOf(Prefs.rememberLastLink) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            },
        horizontalAlignment = Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .padding(horizontal = 40.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SetupTitle(title = "General", topSpacer = false)

            SettingsRow {
                SettingsFolder( // Downloads Folder
                    title = "Downloads folder",
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

            SettingsRow {
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

            SettingsRow {
                var expanded by remember { mutableStateOf(false) }
                SettingsDropdown(
                    title = "Open on startup",
                    value = Prefs.openOnStartup.titleCase(),
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    options = listOf("media", "settings", "last open"),
                    onSelect = { Prefs.openOnStartup = it },
                    dropdownLabel = { it.titleCase() },
                )
            }

            SettingsRow() {
                SettingsSwitch(
                    title = "Remember media settings",
                    checked = rememberMediaSettings,
                    onCheckedChange = { rememberMediaSettings = it; Prefs.rememberMediaSettings = it },
                    description = "Reuse your last Media tab choices for new links."
                )
            }

            SettingsRow(bottomSpacer = false) {
                SettingsSwitch(
                    title = "Remember last link",
                    checked = rememberLastLink,
                    onCheckedChange = { rememberLastLink = it; Prefs.rememberLastLink = it },
                    description = "Keep the last link when restarting the app."
                )
            }

            SetupTitle(title = "Dependencies")

            SettingsRow {
                SettingsFolder( // YT-DLP
                    title = "YT-DLP", description = ytDlpStatus.text,
                    statusColor = ytDlpStatus.color,
                    value = ytDlpFolder,
                    onValueChange = { ytDlpFolder = it; Prefs.ytDlpFolder = it },
                    onLocate = {
                        NativeFolderPicker.pickFolder(
                            title = "Set the YT-DLP destination folder: ",
                            initialDir = ytDlpFolder
                        )?.let {
                            ytDlpFolder = it
                            Prefs.ytDlpFolder = it
                        }
                    },
                    downloadState = ytDlpStatus.state,
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

            SettingsRow {
                SettingsFolder( // ffmpeg
                    title = "ffmpeg", description = ffmpegStatus.text,
                    statusColor = ffmpegStatus.color,
                    value = ffmpegFolder,
                    onValueChange = { ffmpegFolder = it; Prefs.ffmpegFolder = it },
                    onLocate = {
                        NativeFolderPicker.pickFolder(
                            title = "Set the ffmpeg destination folder: ",
                            initialDir = ffmpegFolder
                        )?.let {
                            ffmpegFolder = it
                            Prefs.ffmpegFolder = it
                        }
                    },
                    downloadState = ffmpegStatus.state,
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

            SettingsRow(bottomSpacer = false) {
                SettingsFolder( // Deno
                    title = "deno", description = denoStatus.text,
                    statusColor = denoStatus.color,
                    value = denoFolder,
                    onValueChange = { denoFolder = it; Prefs.denoFolder = it },
                    onLocate = {
                        NativeFolderPicker.pickFolder(
                            title = "Set the deno destination folder: ",
                            initialDir = denoFolder
                        )?.let {
                            denoFolder = it
                            Prefs.denoFolder = it
                        }
                    },
                    downloadState = denoStatus.state,
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
        fontSize = Sizes.TitleFont,
        color = AppTheme.Font1,
        fontWeight = Sizes.TitleWeight
    )
    if (bottomSpacer) Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun RowScope.SettingLabel(
    title: String,
    description: String = "",
    statusColor: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = title,
            color = AppTheme.Font1,
            fontSize = Sizes.FieldFontSize
        )
        if (description.isNotBlank()) {
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (statusColor != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, shape = CircleShape)
                    )
                }

                Text(
                    text = description,
                    color = AppTheme.Font2,
                    fontSize = Sizes.FieldFontSize
                )
            }
        }
    }
}

@Composable
fun SettingsRow(
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

@Composable @OptIn(ExperimentalFoundationApi::class)
fun SettingsFolder(
    title: String, description: String = "", placeholder: String = "Select a folder...",
    statusColor: Color? = null,
    value: String,
    onValueChange: (String) -> Unit,
    onLocate: () -> Unit,
    downloadState: DependencyState? = null,
    onDownload: (() -> Unit)? = null,  // null = no muestra el botón
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Sizes.FieldTextHeight)
    ) {
        SettingLabel(title = title, description = description, statusColor = statusColor)

        FieldText(
            value = value,
            onValueChange = onValueChange,
            color = AppTheme.Font2,
            maxLength = 36,
            outerContent = {
                if (onDownload != null) {
                    val tooltip = downloadState?.let {
                        when (it) {
                            DependencyState.UP_TO_DATE -> "Up to date"
                            DependencyState.UPDATE_AVAILABLE -> "Update"
                            DependencyState.NOT_FOUND -> "Install"
                            DependencyState.CHECKING -> "Checking..."
                        }
                    } ?: "Install"

                    val icon = downloadState?.let {
                        when (it) {
                            DependencyState.UP_TO_DATE -> "reload"
                            DependencyState.UPDATE_AVAILABLE -> "reload"
                            DependencyState.NOT_FOUND -> "download"
                            DependencyState.CHECKING -> "download"
                        }
                    } ?: "download"

                    TooltipArea(
                        tooltip = {
                            Box(
                                modifier = Modifier.fieldStyle().padding(8.dp)
                            ) {
                                Text(tooltip, color = AppTheme.Font1, fontSize = 13.sp)
                            }
                        },
                        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset((8).dp, (-8).dp), alignment = Alignment.TopEnd),
                        delayMillis = 200
                    ) {
                        ActionIcon(
                            iconName = icon,
                            iconSize = 13.dp,
                            shape = Sizes.Round6, padding = 0.dp,
                            modifier = Modifier.size(Sizes.FieldTextHeight)
                        ) { onDownload() }
                    }
                }
                ActionIcon(
                    iconName = "folder",
                    iconSize = 13.dp,
                    shape = Sizes.Round6, padding = 0.dp,
                    modifier = Modifier.size(Sizes.FieldTextHeight)
                ) { onLocate() }
            }
        ) {
            if (value.isEmpty()) {
                Text(placeholder, color = AppTheme.Font2, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SettingText(
    title: String, description: String = "",
    value: String, placeholder: String = "",
    onValueChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // defaultMinSize permite que la fila crezca si hay 2 líneas de texto
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Sizes.FieldTextHeight)
    ) {
        SettingLabel(title = title, description = description)

        var isFocused by remember { mutableStateOf(false) }

        BevelContainer(
            border = if (isFocused) AppTheme.Accent else AppTheme.Border,
            modifier = Modifier.fieldTextSize()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = AppTheme.Font1, fontSize = 13.sp),
                cursorBrush = SolidColor(AppTheme.Font1),
                singleLine = true,
                modifier = Modifier.fieldTextSize().fieldStyle(),
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

@Composable
fun <T> SettingsDropdown(
    title: String,
    value: String, description: String = "",
    modifier: Modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Sizes.FieldTextHeight),
    options: List<T>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    dropdownLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        SettingLabel(title = title, description = description)

        FieldDropdown(
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
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Sizes.FieldTextHeight)
    ) {
        SettingLabel(title = title, description = description)

        FieldSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
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
    val job = scope.launch {
        try {
            val targetFolder = currentFolder.ifEmpty {
                File(System.getProperty("user.dir"), "bin").absolutePath
            }

            if (currentFolder.isEmpty())
                onFolderUpdate(targetFolder)

            action(targetFolder, task)
            task.isCompleted = true
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            task.hasError = true
        } finally { // finally es un bloque que se ejecuta siempre aunque haya un error
            activeDownloads.remove(taskTitle)
        }
    }
    task.onCancel = { job.cancel() }
}