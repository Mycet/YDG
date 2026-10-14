package dev.mycet.ydg.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppTheme {
    val Sunken      = Color(0xff161616)  // inputs, barra lateral
    val Background  = Color(0xff1C1C1C)  // fondo general
    val Surface     = Color(0xff252525)  // paneles, tarjetas
    val SurfaceHigh = Color(0xff2E2E2E)  // hover, seleccionado

    val Font1       = Color(0xffE6E6E6)  // texto principal
    val Font2       = Color(0xff8A8A8A)  // labels, secundario
    val Font3       = Color(0xff6F6F6F)  // solo deshabilitado

    val Accent      = Color(0xffad9bf6)
    val AccentHover = Color(0xffc0b2f9)
    val OnAccent    = Color(0xff101010)  // texto sobre el acento

    val Border      = Color(0xff333333)
    val BorderHover = Color(0xff3F3F3F)

    val Round8      = RoundedCornerShape(8.dp)
    val Round12     = RoundedCornerShape(12.dp)

/*    val Contrast        = Color(0xff101010)
    val Contrast2       = Color(0xff161616)
    val Background      = Color(0xff1C1C1C)
    val Background2     = Color(0xff252525)

    val Font1           = Color(0xffE6E6E6)  // Texto principal
    val Font2           = Color(0xff8A8A8A)  // Labels, texto secundario
    val Font3           = Color(0xff6F6F6F)

    val Accent          = Color(0xffad9bf6)

    val Border          = Color(0xff333333)*/

/*    val Contrast      = Color(0xff060f2f)
    val Background    = Color(0xff1b2240)
    val SecondaryBackground   = Color(0xff292f4d)

    val HoverA          = Color(0xff2f3259)
    val HoverA2         = Color(0xff41406f)
    val HoverB          = Color(0xff3b4155)

    val TextPrimary   = Color(0xffefeff1)  // Texto principal
    val TextSecondary = Color(0xff959daa)  // Labels, texto secundario

    val Accent        = Color(0xffad9bf6)

    val Icon            = Color(0xff313852)

    val Border1        = Accent
    val Border2        = Color(0xff303c6a)
    val Border3        = Color(0xff292f4d)

    val ProgressBar    = Color(0xff14aa05)
    val ProgressBarError = Color(0xffaa1405)*/
}