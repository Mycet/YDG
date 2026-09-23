package dev.mycet.ydg.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.utils.ui.NavigationIcon

// Unit = void

@Composable
fun TabBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,  // Función que recibe un AppTab y devuelve void
) {
    Column (
        modifier = Modifier.fillMaxHeight()
            .width(40.dp)
            .background(AppTheme.Background)
            .padding(2.dp),
        horizontalAlignment = CenterHorizontally,
    ) {
        AppTab.entries.forEach { tab ->
            if (tab == AppTab.SETUP) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(1.dp)
                            .background(color = AppTheme.SecondaryBackground)
                    )
                }
            }

            val isSelected = tab == selectedTab

            NavigationIcon(
                isSelected = isSelected,
                iconName = tab.iconPath,
                modifier = Modifier.drawBehind {
                    if (isSelected) {
                        drawLine(
                            color = AppTheme.Accent,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3.dp.toPx() // Grosor de la línea
                        )
                    }
                },
                onClick = { onTabSelected(tab) }
            )
        }
    }
}