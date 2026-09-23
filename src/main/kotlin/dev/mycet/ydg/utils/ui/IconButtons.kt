package dev.mycet.ydg.utils.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.utils.AppTheme

// Action Icon
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
@Composable
fun ActionIcon(
    hoveredColor: Color = AppTheme.SecondaryBackground,
    backgroundColor: Color = Color.Transparent,
    iconName: String,
    description: String = "TopIcon",
    shape: RoundedCornerShape = RoundedCornerShape(0.dp),
    iconSize: Dp = 13.dp,
    modifier: Modifier = Modifier.width(40.dp),
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(
                color = if (isHovered) hoveredColor else backgroundColor,
                shape = shape,
            )
    ) {
        var path = if (iconName.startsWith("icons/")) iconName else "icons/$iconName"
        if (!path.endsWith(".svg")) path = "$path.svg"
        Icon(
            painter = painterResource(path),
            contentDescription = description,
            tint = if (isHovered) AppTheme.TextPrimary else AppTheme.TextSecondary,
            modifier = Modifier.size(iconSize)
        )
    }
}

// Navigation Icon
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
@Composable
fun NavigationIcon(
    isSelected: Boolean,
    iconName: String,
    modifier: Modifier = Modifier,
    backgroundSelected: Color = AppTheme.Accent.copy(alpha = 0.15f),
    background: Color = AppTheme.Background,
    colorIfHovered: Color = AppTheme.Accent.copy(alpha = 0.15f),
    color: Color = Color.Transparent,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = if (isSelected) backgroundSelected else background

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        val iconBackground = if (isHovered && !isSelected) colorIfHovered else color

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = iconBackground,
                    shape = CircleShape
                )
        ) {
            var path = if (iconName.startsWith("icons/")) iconName else "icons/$iconName"
            if (!path.endsWith(".svg")) path = "$path.svg"

            Icon(
                painter = painterResource(path),
                modifier = Modifier.size(iconSize),
                contentDescription = "",
                tint = if (isSelected) AppTheme.TextPrimary else AppTheme.TextSecondary,
            )
        }
    }
}

// Tooltip Icon
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
@Composable @OptIn(ExperimentalFoundationApi::class)
fun TooltipIcon(
    iconName: String,
    iconId: String = "TooltipIcon",
    iconColor: Color = AppTheme.TextSecondary,
    iconSize: Dp = 13.dp,
    tooltipText: String = "",
    tooltipTextColor: Color = AppTheme.TextPrimary,
    tooltipTextSize: TextUnit = 12.sp,
    tooltipModifier: Modifier = Modifier.background(AppTheme.Contrast)
        .border(1.dp, AppTheme.Border2)
        .padding(8.dp)
) {
    var path = if (iconName.startsWith("icons/")) iconName else "icons/$iconName"
    if (!path.endsWith(".svg")) path = "$path.svg"

    TooltipArea(
        tooltip = {
            Box(tooltipModifier) {
                Text(tooltipText, color = tooltipTextColor, fontSize = tooltipTextSize)
            }
        },
    ) {
        Icon(
            painter = painterResource(path),
            contentDescription = iconId,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}