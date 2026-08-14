package dev.maigo.maigoloader

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Border)        // El marco exterior
            .padding(12.dp)                        // El "grosor" del marco
            .background(AppTheme.Background)    // El contenido adentro
    ) {
        // Barra de tabs arriba
        TabBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // Contenido según el tab seleccionado
        when (selectedTab) {
            AppTab.VIDEO -> VideoTab()
            AppTab.AUDIO -> AudioTab()
        }
    }
}