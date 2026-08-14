package dev.maigo.maigoloader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.lightColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Surface


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTab() {
    var expanded by remember { mutableStateOf(false) }
    var videoURL by remember { mutableStateOf("") }
    val options = listOf("MP4", "MP3", "MPG", "MKV")
    var opcionInicial by remember { mutableStateOf("MP4") }

    Surface(
        modifier = Modifier.width(800.dp),
        color = Color(0xFFF02213) // esto modifica los colores de fondo donde esta cada row

    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), //esto de aca modifica el COLOR de fondo tambien excepto los bordes?/????/
        ) {


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(Color(0xFFae55cf)), //esto modifica el color la linea nomas (dios sabe si llego a usar esta mireda)

            ) {
                Text(
                    text = "Video URL: ",
                    color = Color(0XFF000000),
                    modifier = Modifier.padding(end = 8.dp)
                )

                TextField(
                    value = videoURL,
                    onValueChange = {videoURL = it},
                    label = { Text("Video URL") },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.width(220.dp).height(50.dp)
                )

            Spacer(modifier = Modifier.width(12.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Video Extension: ",
                    style = TextStyle(),
                    modifier = Modifier.padding(end = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.width(220.dp).height(60.dp)

                ) {
                    TextField(
                        value = opcionInicial,
                        onValueChange = { opcionInicial = it },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedIndicatorColor = Color(0xFF9335b5),
                            unfocusedIndicatorColor = Color(0xFFFFFFF),
                            focusedLabelColor = Color.Blue,
                            unfocusedLabelColor = Color.Blue
                        ),
                        modifier = Modifier
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = Color(0xFF9335b5)
                    ) {
                        options.forEach {option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.Black) },
                                onClick = {
                                    opcionInicial = option
                                    expanded = false
                                },
                                contentPadding = PaddingValues(0.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
            }

        }
    }

    }
}