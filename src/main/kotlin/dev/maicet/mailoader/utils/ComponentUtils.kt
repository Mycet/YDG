package dev.maicet.mailoader.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
fun tooltipPlacementRect(): TooltipPlacement =
    TooltipPlacement.ComponentRect(
        anchor = Alignment.TopCenter,
        alignment = Alignment.TopCenter,
        offset = DpOffset(0.dp, (-2).dp) // Desplaza 2dp hacia arriba del texto
    )
@OptIn(ExperimentalFoundationApi::class)
fun tooltipPlacementCursor(): TooltipPlacement =
    TooltipPlacement.CursorPoint(
        offset = DpOffset((8).dp, (-8).dp),
        alignment = Alignment.TopEnd
    )