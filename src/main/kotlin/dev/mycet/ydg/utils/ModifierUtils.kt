package dev.mycet.ydg.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes

fun Modifier.fieldStyle(): Modifier = this
    .border(1.dp, AppTheme.Border, Sizes.Round8)
    .background(AppTheme.Sunken, Sizes.Round8)

fun Modifier.fieldPadding(): Modifier = this
    .padding(horizontal = 8.dp, vertical = 2.dp)

fun Modifier.fieldTextSize(height: Dp = Sizes.TextFieldHeight, width: Dp = Sizes.TextFieldWidth): Modifier = this
    .height(height)
    .width(width)

fun Modifier.fieldDropdownSize(height: Dp = Sizes.TextFieldHeight, width: Dp = Sizes.FieldWidthDropdown): Modifier = this
    .height(height)
    .width(width)