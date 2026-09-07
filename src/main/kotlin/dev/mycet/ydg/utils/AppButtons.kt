package dev.mycet.ydg.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BevelButton(
    text: String,
    icon: ImageVector? = null, // null por defecto
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 26.dp,
    fontSize: TextUnit = 13.sp,
    textColor: Color = AppTheme.TextPrimary,
    dark: Color = AppTheme.Accent,
    light: Color = AppTheme.Background,
    fill: Color = AppTheme.Contrast,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(height)
            .wrapContentWidth()
            .background(dark, RoundedCornerShape(8.dp))
            .padding(1.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .background(light, RoundedCornerShape(8.dp))
                .padding(1.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .background(fill, RoundedCornerShape(8.dp))
                    .clickable(
                        onClick = onClick,
                        interactionSource = interactionSource,
                        indication = null
                    )
                    .padding(horizontal = if (icon != null) 6.dp else 12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                } else
                    Text(text, color = textColor, fontSize = fontSize)
            }
        }
    }
}

@Composable
fun BevelContainer(
    modifier: Modifier = Modifier,
    dark: Color = AppTheme.Accent,
    light: Color = AppTheme.Background,
    fill: Color = AppTheme.Contrast,
    cornerRadius: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .background(dark, shape)
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(light, shape)
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fill, shape),
                contentAlignment = Alignment.CenterStart,
                content = content
            )
        }
    }
}