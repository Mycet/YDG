package dev.maigo.maigoloader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTab() {
    var expanded by remember { mutableStateOf(false) }
    var videoURL by remember { mutableStateOf("") }
    val formatList = listOf("MP4", "MKV", "WEBM", "AVI", "MOV")
    var selectedFormat by remember { mutableStateOf("MP4") }

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
                onClick = { },
                modifier = Modifier.padding(start = 6.dp),

            )
        }

        // Fila 2: Extensión
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

        }
    }
}