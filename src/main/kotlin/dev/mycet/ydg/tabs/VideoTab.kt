package dev.mycet.ydg.tabs

import androidx.compose.foundation.HorizontalScrollbar
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
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MenuAnchorType
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInput
import dev.mycet.ydg.objects.AppTheme
import dev.mycet.ydg.objects.VideoCard
import dev.mycet.ydg.objects.VideoInfo
import dev.mycet.ydg.utils.BevelButton
import dev.mycet.ydg.ytdownload.CommandManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoTab(scope: CoroutineScope, onProgress: (String) -> Unit) {
    val formatList = listOf("MP4", "MKV", "WEBM", "AVI", "MOV")
    val qualityList = listOf("1080p", "720p", "480p", "360p")
    var selectedFormat by remember { mutableStateOf("MP4") }
    var selectedQuality by remember { mutableStateOf("1080p") }
    var expanded by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }
    var videoURL by remember { mutableStateOf("") }

    var previewItems by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(videoURL) {
        previewItems = emptyList()
        if (videoURL.startsWith("http")) {
            isLoading = true
            delay(800.milliseconds)
            previewItems = CommandManager.fetchVideoInfo(videoURL)
            isLoading = false
        } else {
            isLoading = false
        }
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
            modifier = Modifier.fillMaxWidth().height(28.dp) // ancho fijo para todos los elementos de la fila
        ) {
            Text(
                text = "Video URL:",
                color = AppTheme.TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.width(100.dp)
            )

            // Mayor control que TextField
            BasicTextField(
                value = videoURL,
                onValueChange = { videoURL = it }, // { it -> videoURL = it }, 'it' es el nombre default del input y te ahorra el 'it ->'
                textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(AppTheme.TextPrimary),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)  // ocupa todo el espacio restante de la fila
                    .height(26.dp)
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
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()  // el campo de texto real va acá adentro
                    }
                }
            )

            // Box exterior — color oscuro, es el "borde" exterior
            BevelButton(
                text = "Download",
                onClick = {
                    scope.launch {
                        if (videoURL.isEmpty()) {
                            onProgress("Enter a URL first")
                            return@launch
                        }
                        if (!videoURL.startsWith("http")) {
                            onProgress("Invalid URL")
                            return@launch
                        }

                        CommandManager.downloadVideo(
                            url = videoURL,
                            format = selectedFormat,
                            metadata = false,
                            thumbnail = false,
                            subs = false,
                            noPlayList = false,
                            onProgress = { onProgress(it) }
                        )
                    }
                },
                modifier = Modifier.padding(start = 6.dp),

                )
        }

        // Fila 2: Extensión y calidad de video
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(28.dp)
        ) {
            Text(
                text = "Video Extension:",
                color = AppTheme.TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.width(100.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.width(160.dp).height(26.dp)
            ) {
                BasicTextField(
                    value = selectedFormat,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color.Transparent),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                        .height(26.dp)
                        .border(1.dp, AppTheme.Border2)
                        .background(AppTheme.Background2),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                innerTextField()
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = AppTheme.Background2
                ) {
                    formatList.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = AppTheme.TextPrimary, fontSize = 13.sp) },
                            modifier = Modifier.height(26.dp),
                            onClick = {
                                selectedFormat = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(30.dp))

            Text(
                text = "Video Quality:",
                color = AppTheme.TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.width(100.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded2,
                onExpandedChange = { expanded2 = !expanded2 },
                modifier = Modifier.width(160.dp).height(26.dp)
            ) {
                BasicTextField(
                    value = selectedQuality,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = TextStyle(color = AppTheme.TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color.Transparent),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                        .height(26.dp)
                        .border(1.dp, AppTheme.Border2)
                        .background(AppTheme.Background2),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                innerTextField()
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    }
                )

                ExposedDropdownMenu(
                    expanded = expanded2,
                    onDismissRequest = { expanded2 = false },
                    modifier = Modifier.background(AppTheme.Background2)
                ) {
                    qualityList.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = AppTheme.TextPrimary, fontSize = 13.sp) },
                            modifier = Modifier.height(26.dp),
                            onClick = {
                                selectedQuality = option
                                expanded2 = false
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.Background2)
                .drawBehind {
                    val s = 0.5.dp.toPx() // offset necesario para trazar la linea
                    val color = AppTheme.Border1
                    val w = s * 2 // grosor de la linea

                    // Izquierdo, derecho, superior, inferior
                    drawLine(color, Offset(s, 0f), Offset(s, size.height), w)
                    drawLine(color, Offset(size.width - s, 0f), Offset(size.width - s, size.height), w)
                    drawLine(color, Offset(0f, s), Offset(size.width, s), w)
                    drawLine(color, Offset(0f, size.height - s), Offset(size.width, size.height - s), w)

                }
                .padding(2.dp)
        ) {
            when {
                isLoading -> {
                    Text(
                        text = "Loading...",
                        color = AppTheme.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isEmpty() && videoURL.startsWith("http") -> {
                    Text(
                        text = "No results",
                        color = AppTheme.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                previewItems.isNotEmpty() -> {
                    val scrollState = rememberLazyListState()

                    Column(modifier = Modifier.fillMaxWidth()
                        .height(180.dp)
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
                                            if (delta != 0f) {
                                                scope.launch {
                                                    scrollState.scrollBy(delta * 80f)
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            items(previewItems) { video ->
                                VideoCard(video)
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