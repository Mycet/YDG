package dev.mycet.ydg.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes

fun Modifier.inputFieldStyle(): Modifier = this
    .border(1.dp, AppTheme.Border, Sizes.Round8)
    .background(AppTheme.Sunken, Sizes.Round8)

fun Modifier.inputFieldSize(height: Dp = Sizes.FieldHeightText, width: Dp = Sizes.FieldWidthText): Modifier = this
    .height(height)
    .width(width)