package dev.mycet.ydg.ui.navigation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.data.models.*
import dev.mycet.ydg.downloader.CommandManager
import dev.mycet.ydg.ui.components.BevelButton
import dev.mycet.ydg.ui.components.BevelButtonDefaults
import dev.mycet.ydg.ui.components.BevelContainer
import dev.mycet.ydg.ui.components.FieldDropdown
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaTab(scope: CoroutineScope, initialIsAudio: Boolean = false, onNewDownload: (String) -> DownloadTask) {
    val focusManager = LocalFocusManager.current

    var isAudio by remember { mutableStateOf(initialIsAudio) }
    var url by remember { mutableStateOf("") }
    var previewItems by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }
    var detailsLoadedForId by remember { mutableStateOf("") }
    val detailsCache = remember { mutableStateMapOf<String, VideoDetails>() }

    // Playlist
    var details by remember { mutableStateOf<VideoDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var selectedVideoExt by remember { mutableStateOf("") }
    var selectedVideoQuality by remember { mutableStateOf<VideoFormat?>(null) }
    var selectedAudioExt by remember { mutableStateOf("") }
    var selectedAudioQuality by remember { mutableStateOf<VideoFormat?>(null) }

    var expandedExt by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }

    // Es como una función dentro del compose, tiene acceso a las variables del compose y permite simplificar código que se repite
    val updateDetailsState = { newDetails: VideoDetails ->
        detailsCache[newDetails.id] = newDetails
        details = newDetails
        detailsLoadedForId = newDetails.id
        editedTitle = newDetails.title
        selectedVideoExt = getSelectedExtension(false, newDetails)
        selectedVideoQuality = getSelectedFormat(false, newDetails, selectedVideoExt)
        selectedAudioExt = getSelectedExtension(true, newDetails)
        selectedAudioQuality = getSelectedFormat(true, newDetails, selectedAudioExt)
    }

    LaunchedEffect(url) {
        previewItems = emptyList()
        details = null
        if (url.startsWith("http")) {
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
                .height(28.dp) // ancho fijo para todos los elementos de la fila
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var isFocused by remember { mutableStateOf(false) }

            BevelContainer(
                border = if (isFocused) AppTheme.Accent else AppTheme.Border,
                modifier = Modifier
                    .weight(1f)  // ocupa todo el espacio restante de la fila
                    .height(Sizes.TextFieldHeight),
            ) {
                // BasicTextField permite mayor control que TextField
                BasicTextField(
                    modifier = Modifier.fillMaxSize().onFocusChanged { isFocused = it.isFocused },
                    value = url,
                    onValueChange = {
                        url = it
                    },
                    textStyle = TextStyle(color = AppTheme.Font1, fontSize = Sizes.TextFieldFont),
                    cursorBrush = SolidColor(AppTheme.Font1),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            if (url.isEmpty()) {
                                // Texto que muestra cuando el campo está vacío
                                Text(
                                    "Insert link here",
                                    color = AppTheme.Font2,
                                    fontSize = Sizes.TextFieldFont
                                )
                            }
                            innerTextField()  // el campo de texto real va acá adentro
                        }
                    }
                )
            }

            VideoAudioToggle(
                isAudio = isAudio,
                onModeChange = { newMode ->
                    expandedExt = false
                    expandedQuality = false
                    isAudio = newMode
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
                .padding(2.dp)
        ) {
            when {
                isLoadingList -> {
                    Text(
                        text = "Loading...",
                        color = AppTheme.Font2,
                        fontSize = Sizes.TextFieldFont,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isEmpty() && details == null && url.startsWith("http") -> {
                    Text(
                        text = "No results",
                        color = AppTheme.Font2,
                        fontSize = Sizes.TextFieldFont,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isNotEmpty() || details != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Details - Preview con animación fade-in
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
                                    editedTitle = editedTitle,
                                    onTitleChange = { editedTitle = it },
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
                                    val task = onNewDownload(editedTitle)
                                    scope.launch {
                                        if (currentIsAudio) {
                                            val fmt = selectedAudioQuality ?: return@launch
                                            CommandManager.downloadAudio(
                                                url = nonNullDetails.url,
                                                title = editedTitle,
                                                formatId = fmt.formatId,
                                                format = selectedAudioExt,
                                                bitrate = fmt.audioBitrate?.toInt() ?: 128,
                                                onProgress = { line -> task.updateFromLog(line) }
                                            )
                                        } else {
                                            val fmt = selectedVideoQuality ?: return@launch
                                            CommandManager.downloadVideo(
                                                url = nonNullDetails.url,
                                                title = editedTitle,
                                                formatId = fmt.formatId,
                                                extension = fmt.extension,
                                                onProgress = { line -> task.updateFromLog(line) }
                                            )
                                        }
                                        task.isDone = true
                                    }
                                }
                            }
                        }

                        // Playlist
                        if (previewItems.size > 1)
                            MediaPlaylist(
                                previewItems = previewItems,
                                selectedVideoId = details?.id ?: detailsLoadedForId,
                                url = url,
                                scope = scope
                            ) { video ->
                                scope.launch {
                                    var cached = detailsCache[video.id]
                                    if (cached == null) {
                                        isLoadingDetails = true
                                        details = null
                                        cached = CommandManager.fetchVideoDetails(video.url.ifEmpty { url })
                                    }
                                    cached?.let { updateDetailsState(it) }
                                    isLoadingDetails = false
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoAudioToggle(
    isAudio: Boolean,
    onModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSourceVideo = remember { MutableInteractionSource() }
    val isHoveredVideo by interactionSourceVideo.collectIsHoveredAsState()

    val interactionSourceAudio = remember { MutableInteractionSource() }
    val isHoveredAudio by interactionSourceAudio.collectIsHoveredAsState()

    val shape = RoundedCornerShape(8.dp)
    val segmentShape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .height(Sizes.TextFieldHeight)
            .background(AppTheme.Border, shape)
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(AppTheme.Sunken, shape)
                .padding(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video option
                val isVideoSelected = !isAudio
                val videoBg by animateColorAsState(
                    if (isVideoSelected) AppTheme.Accent else if (isHoveredVideo) AppTheme.SurfaceHigh.copy(alpha = 0.5f) else Color.Transparent,
                    animationSpec = tween(300)
                )
                val videoContentColor by animateColorAsState(
                    if (isVideoSelected) AppTheme.OnAccent else if (isHoveredVideo) AppTheme.Font1 else AppTheme.Font2,
                    animationSpec = tween(300)
                )

                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(segmentShape)
                        .background(videoBg, segmentShape)
                        .clickable(
                            interactionSource = interactionSourceVideo,
                            indication = null
                        ) { onModeChange(false) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource("icons/tab_play.svg"),
                        contentDescription = "Video",
                        tint = videoContentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Video",
                        color = videoContentColor,
                        fontSize = 12.sp,
                        fontWeight = if (isVideoSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }

                // Audio option
                val audioBg by animateColorAsState(
                    if (isAudio) AppTheme.Accent else if (isHoveredAudio) AppTheme.SurfaceHigh.copy(alpha = 0.5f) else Color.Transparent,
                    animationSpec = tween(300)
                )
                val audioContentColor by animateColorAsState(
                    if (isAudio) AppTheme.OnAccent else if (isHoveredAudio) AppTheme.Font1 else AppTheme.Font2,
                    animationSpec = tween(300)
                )

                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(segmentShape)
                        .background(audioBg, segmentShape)
                        .clickable(
                            interactionSource = interactionSourceAudio,
                            indication = null
                        ) { onModeChange(true) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource("icons/tab_audio.svg"),
                        contentDescription = "Audio",
                        tint = audioContentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Audio",
                        color = audioContentColor,
                        fontSize = 12.sp,
                        fontWeight = if (isAudio) FontWeight.SemiBold else FontWeight.Normal,
                    )
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
    editedTitle: String, onTitleChange: (String) -> Unit,                   // title
    selectedExt: String, onExtChange: (String) -> Unit,                     // extension
    selectedQuality: VideoFormat?, onQualityChange: (VideoFormat) -> Unit,  // quality - format
    expandedExt: Boolean, onExpandedExtChange: (Boolean) -> Unit,           // (dropdown) extension
    expandedQuality: Boolean, onExpandedQualityChange: (Boolean) -> Unit,   // (dropdown) quality - format
    isLoadingDetails: Boolean,
    duration: Int,
    onDownload: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val thumbnail = rememberThumbnail(details.thumbnail ?: "")

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
                    }

                    if (isLoadingDetails) {
                        Text(
                            text = "Loading...",
                            color = AppTheme.Font2,
                            fontSize = Sizes.TextFieldFont,
                            modifier = Modifier.align(Alignment.Center)
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

            if (!isLoadingDetails)
                BevelButton(
                    text = "Download",
//                    icon = "download-2",
                    colors = BevelButtonDefaults.colors(
                        fillHover = AppTheme.SurfaceHigh.copy(alpha = 0.35f)
                    ),
                    font = BevelButtonDefaults.font(fontWeight = FontWeight.Medium),
                    onClick = onDownload
                )
        }

        // Propiedades
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Título editable
            PropRow(label = "Title") {
                var isFocused by remember { mutableStateOf(false) }

                BevelContainer(
                    border = if (isFocused) AppTheme.Accent else AppTheme.Border,
                    modifier = Modifier
                        .width(400.dp)
                        .height(Sizes.TextFieldHeight)
                ) {
                    BasicTextField(
                        value = editedTitle,
                        onValueChange = onTitleChange,
                        textStyle = TextStyle(
                            color = AppTheme.Font1,
                        ),
                        cursorBrush = SolidColor(AppTheme.Font1),
                        singleLine = true,
                        modifier = Modifier.fillMaxSize().onFocusChanged { isFocused = it.isFocused},
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) { innerTextField() }
                        }
                    )
                }
            }

            // Dropdown formato (extensions)
            val extensions = if (isAudio) details.audioExtensions else details.videoExtensions
            PropRow(label = "Format") {
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
                        append(format.displaySize)
                    }
                }
            }
            PropRow(label = "Quality") {
                FieldDropdown(
                    value = selectedQuality?.let { formatLabel(it) } ?: AnnotatedString(""),
                    expanded = expandedQuality,
                    onExpandedChange = onExpandedQualityChange,
                    options = qualityOptions,
                    onSelect = onQualityChange,
                    label = formatLabel
                )
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
            .padding(horizontal = 4.dp, vertical = 2.dp)
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
private fun PropRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = AppTheme.Font2,
            fontSize = 12.sp,
            modifier = Modifier.width(55.dp)
        )
        content()
    }
}

private fun getSelectedExtension(isAudio: Boolean = false, video: VideoDetails): String =
    (if (isAudio) video.audioExtensions else video.videoExtensions).firstOrNull() ?: ""

private fun getSelectedFormat(isAudio: Boolean = false, video: VideoDetails, ext: String): VideoFormat? =
    (if (isAudio) video.audioFormatsForExt(ext) else video.videoFormatsForExt(ext)).firstOrNull()