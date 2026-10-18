package dev.mycet.ydg.ui.navigation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.core.CommandManager
import dev.mycet.ydg.data.models.*
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.ui.components.*
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
import dev.mycet.ydg.utils.fieldDropdownSize
import dev.mycet.ydg.utils.fieldPadding
import dev.mycet.ydg.utils.fieldStyle
import kotlinx.coroutines.*
import java.util.Objects.toString
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds


data class MediaOptions(
    val title: String = "",
    val author: String = "",
    val album: String = "",
    val addMetadata: Boolean = Prefs.addMetadata,
    val embedThumbnail: Boolean = Prefs.embedThumbnail,
    val embedSubs: Boolean = Prefs.embedSubs,
    val sponsorBlock: Boolean = Prefs.sponsorBlock
)

enum class MediaType {VIDEO, AUDIO};

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaTab(scope: CoroutineScope, initialIsAudio: Boolean = false, onNewDownload: (String) -> DownloadTask) {
    val focusManager = LocalFocusManager.current

    var isAudio by remember { mutableStateOf(initialIsAudio) }
    var url by remember { mutableStateOf(if (Prefs.rememberLastLink) Prefs.lastLink else "") }
    var previewItems by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }
    var loadedVideoId by remember { mutableStateOf("") }
    val detailsCache = remember { mutableStateMapOf<String, VideoDetails>() }

    // Selected Media
    var details by remember { mutableStateOf<VideoDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var loadDetailsJob by remember { mutableStateOf<Job?>(null) }
    var options by remember { mutableStateOf(MediaOptions()) }
    var selectedVideoExt by remember { mutableStateOf("") }
    var selectedVideoQuality by remember { mutableStateOf<VideoFormat?>(null) }
    var selectedAudioExt by remember { mutableStateOf("") }
    var selectedAudioQuality by remember { mutableStateOf<VideoFormat?>(null) }

    var expandedExt by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // Es como una función dentro del compose, tiene acceso a las variables del compose y permite simplificar código que se repite
    val updateDetailsState = { newDetails: VideoDetails ->
        val isSameVideo = newDetails.id == loadedVideoId
        detailsCache[newDetails.id] = newDetails
        details = newDetails
        loadedVideoId = newDetails.id
        options = options.copy(
            title = if (isSameVideo && options.title.isNotEmpty()) options.title else newDetails.title,
            author = if (isSameVideo && options.author.isNotEmpty()) options.author else newDetails.displayAuthor,
            album = if (isSameVideo && options.album.isNotEmpty()) options.album else newDetails.album
        )
        selectedVideoExt = getSelectedExtension(false, newDetails)
        selectedVideoQuality = getSelectedFormat(false, newDetails, selectedVideoExt)
        selectedAudioExt = getSelectedExtension(true, newDetails)
        selectedAudioQuality = getSelectedFormat(true, newDetails, selectedAudioExt)
    }

    LaunchedEffect(url) {
        previewItems = emptyList()
        details = null
        loadedVideoId = ""
        options = options.copy(title = "", author = "", album = "")
        if (url.startsWith("http")) {
            Prefs.lastLink = url

            isLoadingList = true
            delay(800.milliseconds)

            val isPlaylist = url.contains("list=") || url.contains("/playlist")
            val hasVideoId = url.contains("watch?v=") // cuando es una playlist apuntando a un video

            if (isPlaylist && !hasVideoId) {
                previewItems = CommandManager.fetchVideoInfo(url) // Solo playlist
            } else {
                isLoadingDetails = true
                if (isPlaylist) { // Playlist apuntando a un video
                    val listDeferred = async { CommandManager.fetchVideoInfo(url) }
                    val detailsDeferred = async { CommandManager.fetchVideoDetails(url) }

                    // await() espera a que se resuelva el Deferred, y después usa el resultado
                    detailsDeferred.await()
                        ?.let { updateDetailsState(it) } // '?.let' hace que si no es null se use en el lambda
                    previewItems = listDeferred.await()
                } else
                    CommandManager.fetchVideoDetails(url)?.let { updateDetailsState(it) }
                isLoadingDetails = false
            }
            isLoadingList = false
        }
    }

    LaunchedEffect(isAudio) {
        details?.let { nonNullDetails ->
            if (isAudio && selectedAudioExt.isEmpty()) {
                selectedAudioExt = getSelectedExtension(true, nonNullDetails)
                selectedAudioQuality = getSelectedFormat(true, nonNullDetails, selectedAudioExt)
            } else if (!isAudio && selectedVideoExt.isEmpty()) {
                selectedVideoExt = getSelectedExtension(false, nonNullDetails)
                selectedVideoQuality = getSelectedFormat(false, nonNullDetails, selectedVideoExt)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize() // que ocupe todo el espacio
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp), // separación desde los bordes
        verticalArrangement = Arrangement.spacedBy(4.dp) // espaciado vertical entre elementos
    ) {
        // Fila 1 — URL & Botón Video / Audio
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .height(Sizes.FieldTextHeight) // ancho fijo para todos los elementos de la fila
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FieldText(
                modifier = Modifier
                    .weight(1f)
                    .height(Sizes.FieldTextHeight),
                value = url,
                onValueChange = { url = it },
                leadingIcon = {
                    Icon (
                        painter = painterResource("icons/link.svg"),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = AppTheme.Font2
                    )
                },

                trailingIcon = {
                    TooltipArea(
                        tooltip = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = AppTheme.Sunken,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(1.dp, AppTheme.Border, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text (
                                    text = "Copy from clipboard",
                                    color = AppTheme.Font1,
                                    fontSize = Sizes.FieldFontSize
                                )
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.CursorPoint(
                            alignment = Alignment.BottomEnd,
                            offset = DpOffset(0.dp, 8.dp)
                        )
                    ) {
                        Icon(
                            painter = painterResource("icons/clipboard.svg"),
                            contentDescription = "Copy from clipboard",
                            tint = AppTheme.Font2,
                            modifier = Modifier.size(20.dp)
                                .clickable {
                                    val texto = clipboardManager.getText()?.text
                                    if (!clipboardManager.equals(" ")) {
                                        url = toString(texto)
                                    }
                                }
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        painter = painterResource("icons/continue.svg"),
                        contentDescription = null,
                        tint = AppTheme.Font2,
                        modifier = Modifier.size(20.dp).clickable {

                        }

                    )

                },

            ) {
                if (url.isEmpty()) {
                    Text(
                        "Insert link here",
                        color = AppTheme.Font2,
                        fontSize = Sizes.FieldFontSize
                    )
                }
            }


            MultiOptionSwitch(
                options = MediaType.entries,
                selectedOption = if (isAudio) MediaType.AUDIO else MediaType.VIDEO,
                onOptionSelected = { newType ->
                    expandedExt = false
                    expandedQuality = false
                    isAudio = (newType == MediaType.AUDIO)
                },
                optionLabel = { type ->
                    when (type) {
                        MediaType.VIDEO -> "Video"
                        MediaType.AUDIO -> "Audio"
                    }
                },
                optionIcon = { type ->
                    when (type) {
                        MediaType.VIDEO -> painterResource("icons/tab_play.svg")
                        MediaType.AUDIO -> painterResource("icons/tab_audio.svg")
                    }
                }
            )
        }

        // Fila 2 — Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.Background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            when {
                isLoadingList -> {
                    Text(
                        text = "Loading...",
                        color = AppTheme.Font2,
                        fontSize = Sizes.FieldFontSize,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isEmpty() && details == null && url.startsWith("http") -> {
                    Text(
                        text = "No results",
                        color = AppTheme.Font2,
                        fontSize = Sizes.FieldFontSize,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isNotEmpty() || details != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Details - Preview con animación fade-in
                        // ————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
                        details?.let { nonNullDetails ->
                            AnimatedContent(
                                targetState = isAudio,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(durationMillis = 600)) togetherWith
                                    fadeOut(animationSpec = tween(durationMillis = 150))
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                label = "MediaDetailsFade"
                            ) { currentIsAudio ->
                                val currentExt = if (currentIsAudio) selectedAudioExt else selectedVideoExt
                                val currentQuality = if (currentIsAudio) selectedAudioQuality else selectedVideoQuality

                                MediaDetailsPanel(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    details = nonNullDetails,
                                    isAudio = currentIsAudio,
                                    options = options, onOptionsChange = { options = it },
                                    selectedExt = currentExt,
                                    onExtChange = { newExt ->
                                        if (currentIsAudio) {
                                            selectedAudioExt = newExt
                                            selectedAudioQuality = nonNullDetails.audioFormatsForExt(newExt).firstOrNull()
                                        } else {
                                            selectedVideoExt = newExt
                                            selectedVideoQuality = nonNullDetails.videoFormatsForExt(newExt).firstOrNull()
                                        }
                                    },
                                    selectedQuality = currentQuality,
                                    onQualityChange = { newQuality ->
                                        if (currentIsAudio) {
                                            selectedAudioQuality = newQuality
                                        } else {
                                            selectedVideoQuality = newQuality
                                        }
                                    },
                                    expandedExt = expandedExt,
                                    onExpandedExtChange = { expandedExt = it },
                                    expandedQuality = expandedQuality,
                                    onExpandedQualityChange = { expandedQuality = it },
                                    isLoadingDetails = isLoadingDetails,
                                    duration = nonNullDetails.duration.roundToInt(),
                                ) {
                                    val task = onNewDownload(options.title)
                                    var job: Job? = null
                                    var currentProcess: Process? = null

                                    task.onCancel = {
                                        val proc = currentProcess
                                        job?.cancel()
                                        scope.launch(Dispatchers.IO) {
                                            CommandManager.killProcessTree(proc)
                                            task.cleanResidualFiles()
                                        }
                                    }

                                    job = scope.launch {
                                        try {
                                            if (currentIsAudio) {
                                                val fmt = selectedAudioQuality ?: return@launch
                                                CommandManager.downloadAudio(
                                                    url = nonNullDetails.url,
                                                    options = options,
                                                    formatId = fmt.formatId,
                                                    format = selectedAudioExt,
                                                    bitrate = fmt.audioBitrate?.toInt() ?: 128,
                                                    onProgress = { line -> task.updateFromLog(line) },
                                                    onProcess = { proc -> currentProcess = proc }
                                                )
                                            } else {
                                                val fmt = selectedVideoQuality ?: return@launch
                                                CommandManager.downloadVideo(
                                                    url = nonNullDetails.url,
                                                    options = options,
                                                    formatId = fmt.formatId,
                                                    extension = fmt.extension,
                                                    onProgress = { line -> task.updateFromLog(line) },
                                                    onProcess = { proc -> currentProcess = proc }
                                                )
                                            }
                                            task.isCompleted = true
                                        } catch (_: CancellationException) { // Descarga cancelada por el usuario
                                            val proc = currentProcess
                                            scope.launch(Dispatchers.IO) {
                                                CommandManager.killProcessTree(proc)
                                                task.cleanResidualFiles()
                                            }
                                        } finally {
                                            currentProcess = null
                                        }
                                    }
                                }
                            }
                        }

                        // Playlist
                        // ————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
                        if (previewItems.size > 1)
                            MediaPlaylist(
                                previewItems = previewItems,
                                selectedVideoId = details?.id ?: loadedVideoId,
                                url = url,
                                scope = scope
                            ) { video ->
                                loadDetailsJob?.cancel()

                                loadedVideoId = video.id
                                details = video.toVideoDetails(url)
                                options = options.copy(title = video.title, author = video.displayAuthor, album = video.album)
                                selectedVideoExt = ""
                                selectedVideoQuality = null
                                selectedAudioExt = ""
                                selectedAudioQuality = null

                                val cached = detailsCache[video.id]
                                if (cached != null) {
                                    updateDetailsState(cached)
                                    isLoadingDetails = false
                                } else {
                                    isLoadingDetails = true
                                    loadDetailsJob = scope.launch {
                                        val fetched = CommandManager.fetchVideoDetails(video.url.ifEmpty { url })
                                        fetched?.let { updateDetailsState(it) }
                                        isLoadingDetails = false
                                    }
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable // Details
private fun MediaDetailsPanel(
    modifier: Modifier = Modifier,
    details: VideoDetails,
    isAudio: Boolean,
    options: MediaOptions, onOptionsChange: (MediaOptions) -> Unit,
    selectedExt: String, onExtChange: (String) -> Unit,                     // extension
    selectedQuality: VideoFormat?, onQualityChange: (VideoFormat) -> Unit,  // quality - format
    expandedExt: Boolean, onExpandedExtChange: (Boolean) -> Unit,           // (dropdown) extension
    expandedQuality: Boolean, onExpandedQualityChange: (Boolean) -> Unit,   // (dropdown) quality - format
    isLoadingDetails: Boolean,
    duration: Int,
    onDownload: () -> Unit
) {
    var destination by remember { mutableStateOf(Prefs.downloadFolder) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "ThumbnailPulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val thumbnail = rememberThumbnail(details.thumbnail)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(0.dp)
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(240.dp)
                            .blur(
                                radius = 32.dp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                            )
                            .alpha(0.5f)
                            .clip(Sizes.Round12),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(Sizes.Round12)
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(pulseAlpha)
                                .background(AppTheme.SurfaceHigh)
                                .border(1.dp, AppTheme.Border, Sizes.Round12)
                        )
                    }
                }
            }

            // Uploader y Duración
            val mins = duration / 60;
            val secs = duration % 60
            Text(
                "${details.uploader}  ·  %d:%02d".format(mins, secs),
                color = AppTheme.Font2,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            BevelButton(
                text = if (isLoadingDetails) "Loading..." else "Download",
                colors = if (isLoadingDetails) {
                    BevelButtonDefaults.colors(
                        border = AppTheme.Border,
                        borderHover = AppTheme.Border,
                        fill = AppTheme.Sunken,
                        fillHover = AppTheme.Sunken,
                        textColor = AppTheme.Font2,
                        textColorHover = AppTheme.Font2
                    )
                } else {
                    BevelButtonDefaults.colors(
                        fillHover = AppTheme.SurfaceHigh.copy(alpha = 0.35f)
                    )
                },
                font = BevelButtonDefaults.font(fontWeight = FontWeight.Medium),
                onClick = {
                    if (!isLoadingDetails) onDownload()
                }
            )
        }

        // Propiedades
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PropertyRow(label = "Title") {
                FieldText(
                    value = options.title,
                    onValueChange = { onOptionsChange(options.copy(title = it)) },
                ) {}
            }
            PropertyRow(label = "Author") {
                FieldText(
                    value = options.author,
                    onValueChange = { onOptionsChange(options.copy(author = it)) },
                ) {}
            }
            PropertyRow(label = "Album", bottomSpacer = true) {
                if (isLoadingDetails && options.album.isEmpty()) {
                    Box(modifier = Modifier.fieldDropdownSize(width = 400.dp).fieldStyle(),
                        contentAlignment = Alignment.CenterStart) {
                        Box(
                            modifier = Modifier.fieldPadding()
                                .size(120.dp, 10.dp)
                                .alpha(pulseAlpha)
                                .background(AppTheme.SurfaceHigh, Sizes.Round4)
                                .border(1.dp, AppTheme.Border, Sizes.Round4),
                        )
                    }
                } else {
                    FieldText(
                        value = options.album,
                        onValueChange = { onOptionsChange(options.copy(album = it)) },
                    ) {}
                }
            }

            // Dropdown formato (extensions)
            val extensions = if (isAudio) details.audioExtensions else details.videoExtensions
            PropertyRow(label = "Format") {
                if (isLoadingDetails && selectedExt.isEmpty())
                    Box(modifier = Modifier.fieldDropdownSize().fieldStyle().fieldStyle(),
                        contentAlignment = Alignment.CenterStart) {
                        Box(
                            modifier = Modifier.fieldPadding()
                                .size(60.dp, 10.dp)
                                .alpha(pulseAlpha)
                                .background(AppTheme.SurfaceHigh, Sizes.Round4)
                                .border(1.dp, AppTheme.Border, Sizes.Round4),
                        )
                    }
                else
                    FieldDropdown(
                        value = selectedExt.uppercase(),
                        expanded = expandedExt,
                        onExpandedChange = onExpandedExtChange,
                        options = extensions,
                        onSelect = onExtChange,
                        label = { it.uppercase() }
                    )
            }

            // Dropdown quality-format
            val qualityOptions =
                if (isAudio) details.audioFormatsForExt(selectedExt) else details.videoFormatsForExt(selectedExt)
            val formatLabel: (VideoFormat) -> AnnotatedString = { format ->
                buildAnnotatedString {
                    val quality = if (isAudio) "${format.audioBitrate?.toInt() ?: 0} kbps" else format.displayQuality
                    append(quality)
                    append("    ")
                    withStyle(SpanStyle(color = AppTheme.Font2)) {
                        append(format.totalDisplaySize(if (isAudio) 0L else details.bestAudioSize))
                    }
                }
            }
            PropertyRow(label = "Quality", bottomSpacer = true) {
                if (isLoadingDetails && selectedQuality == null)
                    Box(modifier = Modifier.fieldDropdownSize().fieldStyle(),
                        contentAlignment = Alignment.CenterStart) {
                        Box(
                            modifier = Modifier.fieldPadding()
                                .size(90.dp, 10.dp)
                                .alpha(pulseAlpha)
                                .background(AppTheme.SurfaceHigh, Sizes.Round4)
                                .border(1.dp, AppTheme.Border, Sizes.Round4),
                        )
                    }
                else
                    FieldDropdown(
                        value = selectedQuality?.let { formatLabel(it) } ?: AnnotatedString(""),
                        expanded = expandedQuality,
                        onExpandedChange = onExpandedQualityChange,
                        options = qualityOptions,
                        onSelect = onQualityChange,
                        label = formatLabel
                    )
            }

            PropertyRow(label = "Destination") {
                FieldText(
                    value = destination,
                    onValueChange = { destination = it; Prefs.downloadFolder = it },
                    color = AppTheme.Font2,
                    outerContent = {
                        ActionIcon(
                            iconName = "folder",
                            iconSize = Sizes.FieldIconSize,
                            shape = Sizes.Round6,
                            modifier = Modifier.size(Sizes.FieldTextHeight)
                        ) {
                            NativeFolderPicker.pickFolder(
                                title = "Set downloads destination folder: ",
                                initialDir = Prefs.downloadFolder
                            )?.let { // .let permite hacer algo con el valor nullable devuelto si no es nulo
                                destination = it
                                Prefs.downloadFolder = it
                            }
                        }
                    }
                ) {
                    if (Prefs.downloadFolder.isEmpty()) {
                        Text("Select download destination folder...", color = AppTheme.Font2, fontSize = Sizes.FieldFontSize)
                    }
                }
            }
        }
    }
}

@Composable // Playlist
private fun MediaPlaylist(
    previewItems: List<VideoInfo>,
    selectedVideoId: String?,
    url: String,
    scope: CoroutineScope,
    onVideoClick: (VideoInfo) -> Unit
) {
    val scrollState = rememberLazyListState()
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentItems by remember { mutableStateOf(previewItems) }

    // sincronizar por si 'previewItems' cambia desde afuera
    LaunchedEffect(previewItems) { currentItems = previewItems }

    LaunchedEffect(selectedVideoId, currentItems) {
        if (!selectedVideoId.isNullOrEmpty() && currentItems.isNotEmpty()) {
            val targetIndex = currentItems.indexOfFirst { it.id == selectedVideoId }
            if (targetIndex >= 0) {
                val scrollIndex = (targetIndex - 1).coerceAtLeast(0)
                scrollState.animateScrollToItem(scrollIndex)
            }
        }
    }

    LaunchedEffect(scrollState) {
        // cada vez que 'layoutInfo.visibleItemsInfo' cambia
        snapshotFlow {
            scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisible ->
            if (!isLoadingMore && lastVisible != null && lastVisible >= currentItems.size - 8) {
                isLoadingMore = true
                val newItems = CommandManager.fetchVideoInfo(
                    url,
                    playlistStart = currentItems.size + 1,
                    playlistEnd = currentItems.size + 50
                )
                if (newItems.isNotEmpty())
                    currentItems = currentItems + newItems
                isLoadingMore = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyRow(
                state = scrollState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (delta != 0f) scope.launch {
                                    scrollState.scrollBy(delta * 80f)
                                }
                            }
                        }
                    }
            ) {
                items(currentItems) { video ->
                    VideoCard(
                        video,
                        isSelected = video.id == selectedVideoId,
                        onClick = { onVideoClick(video) }
                    )
                }
            }

            if (scrollState.canScrollBackward) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(64.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AppTheme.Background, Color.Transparent)
                            )
                        )
                )
            }

            if (scrollState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(64.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, AppTheme.Background)
                            )
                        )
                )
            }
        }

        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Utils
// ───────────────────────────────────────────────────────
@Composable // Fila de propiedades
private fun PropertyRow(
    label: String,
    bottomSpacer: Boolean = false, topSpacer: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    if (topSpacer) Spacer(modifier = Modifier.height(14.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(Sizes.FieldTextHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = AppTheme.Font2,
            fontSize = Sizes.FieldFontSize,
            modifier = Modifier.width(80.dp)
        )
        content()
    }
    if (bottomSpacer) Spacer(modifier = Modifier.height(14.dp))
}

private fun getSelectedExtension(isAudio: Boolean = false, video: VideoDetails): String =
    (if (isAudio) video.audioExtensions else video.videoExtensions).firstOrNull() ?: ""

private fun getSelectedFormat(isAudio: Boolean = false, video: VideoDetails, ext: String): VideoFormat? =
    (if (isAudio) video.audioFormatsForExt(ext) else video.videoFormatsForExt(ext)).firstOrNull()