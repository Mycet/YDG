package dev.mycet.ydg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes

object Modifiers {

    fun Modifier.inputFieldStyle(): Modifier = this
        .border(1.dp, AppTheme.Border, RoundedCornerShape(4.dp))
        .background(AppTheme.Sunken, RoundedCornerShape(4.dp))

    fun Modifier.inputFieldSize(height: Dp = Sizes.TextFieldHeight, width: Dp = Sizes.TextFieldWidth): Modifier = this
        .height(height)
        .width(width)
}