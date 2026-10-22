package dev.mycet.ydg.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Sizes {
    // Font
    val TooltipFont = 12.sp
    val FieldFont = 14.sp
    val TitleFont = 18.sp

    val TitleWeight = FontWeight.SemiBold

    // Icons
    val FieldIconSize = 13.dp
    val TooltipIconSize = 11.dp

    // Fields
    val FieldTextHeight = 26.dp
    val FieldTextWidth = 400.dp
    val FieldDropdownWidth = 200.dp

    // Shapes
    val Round4      = RoundedCornerShape(4.dp)
    val Round6      = RoundedCornerShape(6.dp)      // Icons
    val Round8      = RoundedCornerShape(8.dp)      // Fields
    val Round12     = RoundedCornerShape(12.dp)     // Switch / Big fields
}

fun TextUnit.toDP(): Dp = this.value.dp