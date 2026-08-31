package dev.mycet.ydg.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.AppTab
import dev.mycet.ydg.utils.AppTheme

// Unit = void

@Composable
fun TabBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,  // Función que recibe un AppTab y devuelve void
    borderColor: Color = AppTheme.Border1,
    tabWidth: Dp = 90.dp
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(AppTheme.Background)
        ) {
            AppTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(tabWidth)
                        // El tab activo ocupa toda la altura (36dp)
                        // El inactivo tiene 4dp de padding arriba, se "hunde"
                        .padding(top = if (isSelected) 0.dp else 4.dp)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) AppTheme.TabActive else AppTheme.TabInactive
                        )
                        .drawBehind() {
                            val s = 0.5.dp.toPx() // Offset necesario
                            val c = borderColor
                            if (isSelected) { // Si está seleccionado, no tiene borde inferior, para fusionarse con el cuerpo
                                // Borde izquierdo, derecho y superior
                                drawLine(c, Offset(s, 0f), Offset(s, size.height), s * 2) // desde (s; 0) hasta (s; size.height)
                                drawLine(c, Offset(size.width - s, 0f), Offset(size.width - s, size.height), s * 2)
                                drawLine(c, Offset(0f, s), Offset(size.width, s), s * 2)
                            } else {
                                // Borde izquierdo, derecho, superior e inferior
                                drawLine(c, Offset(s, 0f), Offset(s, size.height), s * 2)
                                drawLine(c, Offset(size.width - s, 0f), Offset(size.width - s, size.height), s * 2)
                                drawLine(c, Offset(0f, s), Offset(size.width, s), s * 2)
                                drawLine(c, Offset(0f, size.height - s), Offset(size.width, size.height - s), s * 2)
                            }
                        }
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
}