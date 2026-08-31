package dev.mycet.ydg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.objects.Dependencies
import dev.mycet.ydg.tabs.AudioTab
import dev.mycet.ydg.tabs.SetupTab
import dev.mycet.ydg.tabs.VideoTab
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.tabs.TabBar

// Los tabs disponibles en la app
enum class AppTab { VIDEO, AUDIO, SETUP }

// 'recomposición' se le llama al llamado de una función
// La UI se dibuja llamando a funciones, si algo cambia en la UI, Compose vuelve a llamar a la función

@Composable
fun App() {
    // 'remember' le dice a Compose que recuerde este valor entre recomposiciones
    // 'mutableStateOf' crea un estado reactivo — cuando cambia, la UI se redibuja
    var selectedTab by remember { mutableStateOf(AppTab.VIDEO) }
    var setupWarning by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") } // log
    var progress by remember { mutableStateOf(0f) }  // 0.0 a 1.0

    // Parsea el porcentaje del output de yt-dlp o downloadFile
    // yt-dlp imprime "[download]  45.3% of ..."
    // downloadFile imprime "Downloading... 45% ..."
    fun parseProgress(line: String): Float { // pide String y devuelve Float
        val match = Regex("""(\d+(?:\.\d+)?)%""").find(line)
        return match?.groupValues?.get(1)?.toFloatOrNull()?.div(100f) ?: progress
    }

    val onProgress: (String) -> Unit = { line ->
        statusText = line
        progress = parseProgress(line)
    }
    val scope = rememberCoroutineScope()
    val tabWidth = 90.dp  // tiene que coincidir con el width de los tabs en TabBar

    // LaunchedEffect ejecuta el código cada vez que la variable pasada como argumento cambie
    // Al pasarle Unit (void), se ejecuta 1 sola vez al arrancar el programa
    LaunchedEffect(Unit) {
        val issues = Dependencies.check()
        if (issues.isNotEmpty())
            setupWarning = "⚠ " + issues.joinToString(" · ")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background)        // El marco exterior
            .padding(12.dp)                        // El "grosor" del marco
    ) {
        // Barra de tabs arriba
        TabBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            borderColor = AppTheme.Border1,
            tabWidth = tabWidth,
        )

        // Contenido según el tab seleccionado
        Box(
            modifier = Modifier
                .weight(1f) // ocupa todo el espacio restante
                .fillMaxSize()
                .background(AppTheme.Surface)
                .drawBehind {
                    val s = 0.5.dp.toPx() // offset necesario para trazar la linea
                    val color = AppTheme.Border1
                    val w = s * 2 // grosor de la linea
                    val tabPx = tabWidth.toPx()
                    val selectedIndex = selectedTab.ordinal
                    val gapStart = tabPx * selectedIndex
                    val gapEnd = gapStart + tabPx

                    // Izquierdo, derecho, inferior
                    drawLine(color, Offset(s, 0f), Offset(s, size.height), w)
                    drawLine(color, Offset(size.width - s, 0f), Offset(size.width - s, size.height), w)
                    // Superior izquierdo
                    if (gapStart > 0f) // Solo si el tab seleccionado no es el primero, que está en el borde izquierdo
                        drawLine(color, Offset(s, s), Offset(gapStart, s), w)
                    // Superior derecho
                    drawLine(color, Offset(gapEnd, s), Offset(size.width - s, s), w)
                }
                .padding(2.dp)
        ) {
            when (selectedTab) {
                AppTab.VIDEO -> VideoTab(scope, onProgress = onProgress)
                AppTab.AUDIO -> AudioTab(scope, onProgress = onProgress)
                AppTab.SETUP -> SetupTab(scope, onProgress = onProgress)
            }
        }

        // Barra de progreso inferior — siempre visible
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(AppTheme.Surface)
                .drawBehind {
                    val s = 0.5.dp.toPx()
                    val color = AppTheme.Border1
                    val w = s * 2

                    drawLine(color, Offset(s, 0f), Offset(s, size.height), w)
                    drawLine(color, Offset(size.width - s, 0f), Offset(size.width - s, size.height), w)
                    drawLine(color, Offset(0f, size.height - s), Offset(size.width, size.height - s), w)
                }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .width(120.dp)
                    .height(8.dp),
                color = AppTheme.ProgressBar,
                backgroundColor = AppTheme.Background2
            )

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = AppTheme.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )

            if (setupWarning.isNotEmpty()) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = setupWarning,
                    color = AppTheme.ProgressBarError,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}