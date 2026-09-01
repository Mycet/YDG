package dev.mycet.ydg.tabs

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MenuAnchorType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.objects.VideoCard
import dev.mycet.ydg.objects.VideoDetails
import dev.mycet.ydg.objects.VideoFormat
import dev.mycet.ydg.objects.VideoInfo
import dev.mycet.ydg.objects.rememberThumbnail
import dev.mycet.ydg.utils.BevelButton
import dev.mycet.ydg.utils.Sizes
import dev.mycet.ydg.ytdownload.CommandManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTab(scope: CoroutineScope, onProgress: (String) -> Unit) {
    var videoURL by remember { mutableStateOf("") }
    var previewItems by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }
    var detailsLoadedForId by remember { mutableStateOf("") }
    val detailsCache = remember { mutableStateMapOf<String, VideoDetails>() }

    // Panel superior
    var details by remember { mutableStateOf<VideoDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var selectedExt by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf<VideoFormat?>(null) }
    var expandedExt by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }

    LaunchedEffect(videoURL) {
        previewItems = emptyList()
        details = null
        if (videoURL.startsWith("http")) {
            isLoadingList = true
            delay(800.milliseconds)
            previewItems = CommandManager.fetchVideoInfo(videoURL)
            isLoadingList = false
        } else {
            isLoadingList = false
        }
    }

    // Cuando llega la lista, cargar los detalles del primero
    LaunchedEffect(previewItems) {
        val first = previewItems.firstOrNull() ?: return@LaunchedEffect
        if (detailsLoadedForId == first.id) return@LaunchedEffect

        isLoadingDetails = true

        val fetched = CommandManager.fetchVideoDetails(first.url)
        if (fetched != null) {
            details = fetched
            editedTitle = fetched.title ?: ""
            selectedExt = fetched.videoExtensions.firstOrNull() ?: ""
            selectedFormat = fetched.videoFormatsForExt(selectedExt).firstOrNull()
            detailsLoadedForId = first.id
        }
        isLoadingDetails = false
    }

    // Cuando llega el primero, resetear calidad seleccionada
    LaunchedEffect(selectedExt) {
        selectedFormat = details?.videoFormatsForExt(selectedExt)?.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize() // que ocupe todo el espacio
            .padding(horizontal = 8.dp, vertical = 6.dp), // separación desde los bordes
        verticalArrangement = Arrangement.spacedBy(4.dp) // espaciado vertical entre elementos
    ) {
        // Fila 1 — URL
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .height(28.dp) // ancho fijo para todos los elementos de la fila
                .padding(horizontal = 20.dp)
        ) {
            // BasicTextField permite mayor control que TextField
            BasicTextField(
                value = videoURL,
                onValueChange = {
                    videoURL = it
                }, // { it -> videoURL = it }, 'it' es el nombre default del input y te ahorra el 'it ->'
                textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = Sizes.Font),
                cursorBrush = SolidColor(AppTheme.TextPrimary),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)  // ocupa todo el espacio restante de la fila
                    .height(Sizes.TextField)
                    .border(1.dp, AppTheme.Border2)
                    .background(AppTheme.Background2),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp)
                    ) {
                        if (videoURL.isEmpty()) {
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

        // Fila 2 — Elementos
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.Background2)
                .drawBehind {
                    val s = 0.5.dp.toPx()
                    val color = AppTheme.Border1
                    val w = s * 2
                    drawLine(color, Offset(s, 0f), Offset(s, size.height), w)
                    drawLine(color, Offset(size.width - s, 0f), Offset(size.width - s, size.height), w)
                    drawLine(color, Offset(0f, s), Offset(size.width, s), w)
                    drawLine(color, Offset(0f, size.height - s), Offset(size.width, size.height - s), w)
                }
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

                previewItems.isEmpty() && videoURL.startsWith("http") -> {
                    Text(
                        text = "No results",
                        color = AppTheme.TextSecondary,
                        fontSize = Sizes.Font,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isNotEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Panel superior — thumbnail + propiedades
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val thumbnail = rememberThumbnail(details?.thumbnail ?: "")
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .fillMaxHeight()
                                        .background(AppTheme.Surface2)
                                ) {
                                    if (thumbnail != null) {
                                        Image(
                                            bitmap = thumbnail,
                                            contentDescription = "",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
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
                                val totalSecs = previewItems.firstOrNull { it.id == details?.id }?.duration?.roundToInt() ?: 0
                                val mins = totalSecs / 60
                                val secs = totalSecs % 60

                                Text(
                                    if (details == null) "" else "${details?.uploader}  ·  %d:%02d".format(mins, secs),
                                    color = AppTheme.TextSecondary,
                                    fontSize = 11.sp
                                )

                                if (!isLoadingDetails && details != null)
                                    BevelButton(
                                        text = "Download",
                                        modifier = Modifier.padding(top = 10.dp),
                                        onClick = {
                                            scope.launch {
                                                val fmt = selectedFormat ?: return@launch
                                                CommandManager.downloadVideo(
                                                    url = details!!.url,
                                                    title = editedTitle,
                                                    formatId = fmt.formatId,
                                                    ext = fmt.ext,
                                                    onProgress = { onProgress(it) }
                                                )
                                            }
                                        }
                                    )
                            }

                            // Propiedades
                            if (details != null) {
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Titulo editable
                                    PropRow(label = "Title") {
                                        BasicTextField(
                                            value = editedTitle,
                                            onValueChange = { editedTitle = it },
                                            textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = Sizes.Font),
                                            cursorBrush = SolidColor(AppTheme.TextPrimary),
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(Sizes.TextField)
                                                .border(1.dp, AppTheme.Border2)
                                                .background(AppTheme.Background2),
                                            decorationBox = { innerTextField ->
                                                Box(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) { innerTextField() }
                                            }
                                        )
                                    }

                                    // Dropdown formato (ext)
                                    PropRow(label = "Format") {
                                        SimpleDropdown(
                                            value = selectedExt.uppercase(),
                                            expanded = expandedExt,
                                            onExpandedChange = { expandedExt = it },
                                            options = details!!.videoExtensions,
                                            onSelect = { selectedExt = it },
                                            label = { it.uppercase() }
                                        )
                                    }

                                    // Dropdown calidad
                                    val qualityOptions = details!!.videoFormatsForExt(selectedExt)
                                    PropRow(label = "Quality") {
                                        SimpleDropdown(
                                            value = selectedFormat?.let {
                                                "${it.note}  ${it.displaySize}"
                                            } ?: "",
                                            expanded = expandedQuality,
                                            onExpandedChange = { expandedQuality = it },
                                            options = qualityOptions,
                                            onSelect = { selectedFormat = it },
                                            label = { "${it.note}  ${it.displaySize}" }
                                        )
                                    }
                                }
                            }
                        }

                        // Playlist
                        if (previewItems.size > 1) {
                            val scrollState = rememberLazyListState()
                            var isLoadingMore by remember { mutableStateOf(false) }

                            LaunchedEffect(scrollState) {
                                // cada vez que 'layoutInfo.visibleItemsInfo' cambia
                                snapshotFlow {
                                    scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                }.collect { lastVisible ->
                                    if (!isLoadingMore && lastVisible != null && lastVisible >= previewItems.size - 8) {
                                        isLoadingMore = true
                                        val newItems = CommandManager.fetchVideoInfo(
                                            videoURL,
                                            playlistStart = previewItems.size + 1,
                                            playlistEnd = previewItems.size + 50
                                        )
                                        if (newItems.isNotEmpty())
                                            previewItems = previewItems + newItems
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                    items(previewItems) { video ->
                                        VideoCard(video, onClick = {
                                            scope.launch {
                                                var cached = detailsCache[video.id]
                                                if (cached == null) {
                                                    isLoadingDetails = true;
                                                    details = null;
                                                    cached = CommandManager.fetchVideoDetails(video.url.ifEmpty { videoURL })
                                                }

                                                if (cached != null) {
                                                    detailsCache[video.id] = cached
                                                    details = cached
                                                    detailsLoadedForId = video.id

                                                    editedTitle = cached.title ?: ""
                                                    selectedExt = cached.videoExtensions.firstOrNull() ?: ""
                                                    selectedFormat = cached.videoFormatsForExt(selectedExt).firstOrNull()
                                                }
                                                isLoadingDetails = false;
                                            }
                                        })
                                    }
                                }

                                HorizontalScrollbar(
                                    adapter = rememberScrollbarAdapter(scrollState),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable // Fila de propiedades
private fun PropRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = label, color = AppTheme.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(55.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable // Dropdown generico
private fun <T> SimpleDropdown(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<T>,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.width(200.dp).height(Sizes.TextField)
    ) {
        BasicTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .height(Sizes.TextField)
                .border(1.dp, AppTheme.Border2)
                .background(AppTheme.Background2),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
                ) {
                    Box(Modifier.weight(1f)) { innerTextField() }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = AppTheme.Background2
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option), color = AppTheme.TextPrimary, fontSize = Sizes.Font) },
                    modifier = Modifier.height(Sizes.TextField),
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}