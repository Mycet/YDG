package dev.mycet.ydg.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
import dev.mycet.ydg.utils.toSVG

// Action Icon
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
@Composable
fun ActionIcon(
    iconName: String,
    description: String = "TopIcon",
    hoveredColor: Color = AppTheme.Surface,
    backgroundColor: Color = Color.Transparent,
    shape: RoundedCornerShape = Sizes.Round4,
    iconSize: Dp = Sizes.FieldIconSize,
    padding: Dp = 0.dp,
    tint: Color? = null,
    modifier: Modifier = Modifier.size(Sizes.FieldTextHeight),
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val tint = tint ?: if (isHovered) AppTheme.Font1 else AppTheme.Font2

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(
                color = if (isHovered) hoveredColor else backgroundColor,
                shape = shape,
            )
            .padding(padding)
    ) {
        Icon(
            painter = painterResource(iconName.toSVG()),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
@Composable
fun TopIcon(
    iconName: String, description: String,
    hoveredColor: Color = Color.Transparent,
    onClick: () -> Unit
) = ActionIcon(
    iconName = iconName,
    description = description,
    hoveredColor = hoveredColor,
    modifier = Modifier.fillMaxHeight().width(40.dp),
    padding = 0.dp, shape = RoundedCornerShape(0.dp)
) { onClick() }


// Navigation Icon
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
@Composable
fun NavigationIcon(
    isSelected: Boolean,
    iconName: String,
    label: String? = null,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier,
    backgroundSelected: Color = AppTheme.Accent.copy(alpha = 0.15f),
    background: Color = AppTheme.Background,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val rowBg by animateColorAsState(
        when {
            isSelected -> backgroundSelected
            isHovered && isExpanded -> Color.White.copy(alpha = 0.05f)
            else -> background
        },
        animationSpec = tween(120)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(rowBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        val iconBg by animateColorAsState(
            if (isHovered && !isSelected && !isExpanded) Color.White.copy(alpha = 0.08f) else Color.Transparent,
            animationSpec = tween(120)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
                .padding(2.dp)
                .background(
                    color = iconBg,
                    shape = CircleShape
                )
        ) {
            var path = if (iconName.startsWith("icons/")) iconName else "icons/$iconName"
            if (!path.endsWith(".svg")) path = "$path.svg"

            Icon(
                painter = painterResource(path),
                modifier = Modifier.size(iconSize),
                contentDescription = label ?: "",
                tint = if (isSelected) AppTheme.Accent else AppTheme.Font2,
            )
        }

        if (label != null) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(100))
            ) {
                Text(
                    text = label,
                    color = if (isSelected) AppTheme.Accent else AppTheme.Font2,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = 2.dp, end = 12.dp)
                )
            }
        }
    }
}

// Tooltip Icon
// ———————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
@Composable @OptIn(ExperimentalFoundationApi::class)
fun TooltipIcon(
    iconName: String,
    iconId: String = "TooltipIcon",
    iconColor: Color = AppTheme.Font2,
    iconSize: Dp = 13.dp,
    tooltipText: String = "",
    tooltipTextColor: Color = AppTheme.Font1,
    tooltipTextSize: TextUnit = 12.sp,
    tooltipModifier: Modifier = Modifier.background(AppTheme.Sunken)
        .border(1.dp, AppTheme.Border)
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