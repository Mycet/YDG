package dev.mycet.ydg.tabs

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.objects.*
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.utils.Sizes
import dev.mycet.ydg.utils.ui.BevelButton
import dev.mycet.ydg.utils.ui.BevelContainer
import dev.mycet.ydg.utils.ui.SimpleDropdown
import dev.mycet.ydg.ytdownload.CommandManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaTab(isAudio: Boolean = false, scope: CoroutineScope, onNewDownload: (String) -> DownloadTask) {
    var url by remember { mutableStateOf("") }
    var previewItems by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }
    var detailsLoadedForId by remember { mutableStateOf("") }
    val detailsCache = remember { mutableStateMapOf<String, VideoDetails>() }

    // Playlist
    var details by remember { mutableStateOf<VideoDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var selectedExt by remember { mutableStateOf("") }
    var selectedQuality by remember { mutableStateOf<VideoFormat?>(null) }
    var expandedExt by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }

    // Es como una función dentro del compose, tiene acceso a las variables del compose y permite simplificar código que se repite
    val updateDetailsState = { newDetails: VideoDetails ->
        detailsCache[newDetails.id] = newDetails
        details = newDetails
        detailsLoadedForId = newDetails.id
        editedTitle = newDetails.title ?: ""
        selectedExt = getSelectedExtension(isAudio, newDetails)
        selectedQuality = getSelectedFormat(isAudio, newDetails, selectedExt)
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
                isLoadingDetails = true;
                if (isPlaylist) { // Playlist apuntando a un video
                    val listDeferred = async { CommandManager.fetchVideoInfo(url) }
                    val detailsDeferred = async { CommandManager.fetchVideoDetails(url) }

                    // await() espera a que se resuelva el Deferred, y después usa el resultado
                    detailsDeferred.await()
                        ?.let { updateDetailsState(it) } // '?.let' hace que si no es null se use en el lambda
                    previewItems = listDeferred.await()
                } else
                    CommandManager.fetchVideoDetails(url)?.let { updateDetailsState(it) }
                isLoadingDetails = false;
            }
            isLoadingList = false
        }
    }

    // Cuando cambia la extensión, buscar los formatos correspondientes
    LaunchedEffect(selectedExt) {
        val formats = if (isAudio) details?.audioFormatsForExt(selectedExt) else details?.videoFormatsForExt(selectedExt)
        selectedQuality = formats?.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize() // que ocupe todo el espacio
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp), // separación desde los bordes
        verticalArrangement = Arrangement.spacedBy(4.dp) // espaciado vertical entre elementos
    ) {
        // Fila 1 — URL
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .height(28.dp) // ancho fijo para todos los elementos de la fila
                .padding(horizontal = 20.dp)
        ) {
            BevelContainer(
                modifier = Modifier
                    .weight(1f)  // ocupa todo el espacio restante de la fila
                    .height(Sizes.TextField),
            ) {
                // BasicTextField permite mayor control que TextField
                BasicTextField(
                    modifier = Modifier.fillMaxSize(),
                    value = url,
                    onValueChange = {
                        url = it
                    }, // { it -> videoURL = it }, 'it' es el nombre default del input y te ahorra el 'it ->'
                    textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = Sizes.Font),
                    cursorBrush = SolidColor(AppTheme.TextPrimary),
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
                                    color = AppTheme.TextSecondary,
                                    fontSize = Sizes.Font
                                )
                            }
                            innerTextField()  // el campo de texto real va acá adentro
                        }
                    }
                )
            }
        }

        // Fila 2 — Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.Background)
                .padding(2.dp)
        ) {
            when {
                isLoadingList -> {
                    Text(
                        text = "Loading...",
                        color = AppTheme.TextSecondary,
                        fontSize = Sizes.Font,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isEmpty() && details == null && url.startsWith("http") -> {
                    Text(
                        text = "No results",
                        color = AppTheme.TextSecondary,
                        fontSize = Sizes.Font,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isNotEmpty() || details != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Details - Preview
                        details?.let { nonNullDetails ->
                            MediaDetailsPanel(
                                Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                                nonNullDetails,
                                isAudio,
                                editedTitle, { editedTitle = it },
                                selectedExt, { selectedExt = it },
                                selectedQuality, { selectedQuality = it },
                                expandedExt, { expandedExt = it },
                                expandedQuality, { expandedQuality = it },
                                isLoadingDetails,
                                nonNullDetails.duration.roundToInt(),
                            ) {
                                val task = onNewDownload(editedTitle)
                                scope.launch {
                                    val fmt = selectedQuality ?: return@launch
                                    if (isAudio) {
                                        CommandManager.downloadAudio(
                                            url = nonNullDetails.url,
                                            title = editedTitle,
                                            formatId = fmt.formatId,
                                            format = selectedExt,
                                            bitrate = fmt.audioBitrate?.toInt() ?: 128,
                                            onProgress = { line -> task.updateFromLog(line) }
                                        )
                                    } else {
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

                        // Playlist
                        if (previewItems.size > 1)
                            MediaPlaylist(
                                previewItems = previewItems,
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
                modifier = Modifier
                    .size(240.dp)
                    .fillMaxHeight()
                    .background(AppTheme.Background)
                    .clip(RoundedCornerShape(12.dp))
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
                        color = AppTheme.TextSecondary,
                        fontSize = Sizes.Font,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Uploader y Duración
            val mins = duration / 60;
            val secs = duration % 60
            Text(
                "${details.uploader}  ·  %d:%02d".format(mins, secs),
                color = AppTheme.TextSecondary,
                fontSize = 11.sp
            )

            if (!isLoadingDetails)
                BevelButton(
                    text = "Download",
                    modifier = Modifier.padding(top = 10.dp),
                    onClick = onDownload
                )
        }

        // Propiedades

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Título editable
            PropRow(label = "Title") {
                BevelContainer(
                    modifier = Modifier
                        .width(400.dp)
                        .height(Sizes.TextField)
                ) {
                    BasicTextField(
                        value = editedTitle,
                        onValueChange = onTitleChange,
                        textStyle = TextStyle(
                            color = AppTheme.TextPrimary,
                            fontSize = Sizes.Font
                        ),
                        cursorBrush = SolidColor(AppTheme.TextPrimary),
                        singleLine = true,
                        modifier = Modifier.fillMaxSize(),
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
                SimpleDropdown(
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
            val formatLabel: (VideoFormat) -> String = { format ->
                if (isAudio) "${format.audioBitrate?.toInt() ?: 0} kbps    ${format.displaySize}"
                else "${format.quality}    ${format.displaySize}"
            }
            PropRow(label = "Quality") {
                SimpleDropdown(
                    value = selectedQuality?.let { formatLabel(it) } ?: "",
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
        LazyRow(
            state = scrollState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
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
                VideoCard(video, onClick = { onVideoClick(video) })
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
            color = AppTheme.TextSecondary,
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