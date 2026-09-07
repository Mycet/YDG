package dev.mycet.ydg.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun TopIcon(
    hoveredColor: Color = AppTheme.SecondaryBackground,
    backgroundColor: Color = Color.Transparent,
    iconPath: String,
    description: String = "TopIcon",
    shape: RoundedCornerShape = RoundedCornerShape(0.dp),
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .width(40.dp)
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
        Icon(
            painter = painterResource(iconPath),
            contentDescription = description,
            tint = if (isHovered) AppTheme.TextPrimary else AppTheme.TextSecondary,
            modifier = Modifier.size(13.dp)
        )
    }
}