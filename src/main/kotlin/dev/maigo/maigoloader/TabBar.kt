package dev.maigo.maigoloader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Unit = void

@Composable
fun TabBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit  // Función que recibe un AppTab y devuelve void
) {
// El Row que contiene los tabs tiene el color del BORDE/MARCO
    // para que los tabs inactivos (que se hunden con paddingTop) muestren ese color atrás
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(AppTheme.Border)
            .padding(start = 4.dp, top = 0.dp)
    ) {
        AppTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(90.dp)
                    // El tab activo ocupa toda la altura (36dp)
                    // El inactivo tiene 4dp de padding arriba, se "hunde"
                    .padding(top = if (isSelected) 0.dp else 4.dp)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) AppTheme.Surface else AppTheme.TabInactive
                    )
                    // El tab activo NO tiene borde inferior (se funde con el contenido)
                    // El inactivo SÍ tiene borde inferior
                    .then(
                        if (!isSelected) Modifier.border(
                            width = 1.dp,
                            color = AppTheme.Border,
                        ) else Modifier
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = tab.name,
                    color = if (isSelected) AppTheme.TextPrimary else AppTheme.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}