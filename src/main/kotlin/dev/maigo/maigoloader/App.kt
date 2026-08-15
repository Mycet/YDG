package dev.maigo.maigoloader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

// Los tabs disponibles en la app
enum class AppTab { VIDEO, AUDIO }

// 'recomposición' se le llama al llamado de una función
// La UI se dibuja llamando a funciones, si algo cambia en la UI, Compose vuelve a llamar a la función

@Composable
fun App() {
    // 'remember' le dice a Compose que recuerde este valor entre recomposiciones
    // 'mutableStateOf' crea un estado reactivo — cuando cambia, la UI se redibuja
    var selectedTab by remember { mutableStateOf(AppTab.VIDEO) }

    val tabWidth = 90.dp  // tiene que coincidir con el width de los tabs en TabBar

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
                    drawLine(color, Offset(0f, size.height - s), Offset(size.width, size.height - s), w)
                    // Superior izquierdo
                    if (gapStart > 0f) // Solo si el tab seleccionado no es el primero, que está en el borde izquierdo
                        drawLine(color, Offset(s, s), Offset(gapStart, s), w)
                    // Superior derecho
                    drawLine(color, Offset(gapEnd, s), Offset(size.width - s, s), w)
                }
                .padding(2.dp)
        ) {
            when (selectedTab) {
                AppTab.VIDEO -> VideoTab()
                AppTab.AUDIO -> AudioTab()
            }
        }
    }
}