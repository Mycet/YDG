package dev.mycet.ydg.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.mycet.ydg.ui.components.NavigationIcon
import dev.mycet.ydg.ui.theme.AppTheme

// Unit = void

@Composable
fun TabBar(
    selectedTab: AppTab,
    isExpanded: Boolean = false,
    onTabSelected: (AppTab) -> Unit,  // Función que recibe un AppTab y devuelve void
) {
    val barWidth by animateDpAsState(
        targetValue = if (isExpanded) 140.dp else 40.dp,
        animationSpec = tween(180)
    )

    Column (
        modifier = Modifier.fillMaxHeight()
            .width(barWidth)
            .background(AppTheme.Background)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.Start,
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
                            .width(if (isExpanded) 116.dp else 24.dp)
                            .height(1.dp)
                            .background(color = AppTheme.Surface)
                    )
                }
            }

            val isSelected = tab == selectedTab

            NavigationIcon(
                isSelected = isSelected,
                iconName = tab.iconPath,
                label = tab.displayName,
                isExpanded = isExpanded,
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