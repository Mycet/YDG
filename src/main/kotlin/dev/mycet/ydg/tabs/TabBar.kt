package dev.mycet.ydg.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.mycet.ydg.utils.AppTheme

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
            val isSelected = tab == selectedTab
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

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

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        color = if (isSelected) AppTheme.Accent.copy(alpha = 0.15f) else AppTheme.Background,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onTabSelected(tab) }
                    )
                    .drawBehind {
                        if (isSelected) {
                            drawLine(
                                color = AppTheme.Accent,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 3.dp.toPx() // Grosor de la línea
                            )
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = if (isHovered && !isSelected) AppTheme.Accent.copy(alpha = 0.15f) else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(tab.iconPath),
                        modifier = Modifier.size(20.dp),
                        contentDescription = "",
                        tint = if (isSelected) AppTheme.TextPrimary else AppTheme.TextSecondary,
                    )
                }
            }
        }
    }
}