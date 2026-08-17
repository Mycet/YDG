package dev.maigo.maigoloader.objects

import androidx.compose.ui.graphics.Color

object AppTheme {
    val Background     = Color(0xFF2b2b2b)  // Bordes y marco exterior 0xFF3c3c3c 0xFF2b2b2b
    val Background2    = Color(0xff1a1818)  // Fondo principal
    val Border1        = Color(0xff1e1e1e)
    val Border2        = Color(0xff646464)
    val Surface        = Color(0xff505050)  // Cuerpo, Tab activo
    val Surface2       = Color(0xff646464)  // Hover, dropdown, seleccionado
    val Surface3       = Color(0xff828282)
    val TabInactive    = Color(0xff464646)  // Tab no activo
    val TabActive      = Color(0xff505050)  // Tab activo (igual que Surface, se funde)

    val TextPrimary    = Color(0xFFe8e8e8)  // Texto principal
    val TextSecondary  = Color(0xFF9b9b9b)  // Labels, texto secundario

    val Accent         = Color(0xFF7c4fa0)
    val AccentHover    = Color(0xFF9b68bf)
    val AccentText     = Color(0xFFf0eaf8)

    val ProgressBar    = Color(0xff14aa05)
    val ProgressBarError = Color(0xffaa1405)
}