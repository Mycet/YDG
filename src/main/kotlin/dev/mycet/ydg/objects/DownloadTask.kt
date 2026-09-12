package dev.mycet.ydg.objects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

interface DownloadItem {
    val id: String
    val title: String
    var progress: Float
    var sizeText: String
    var speed: String
    var isDone: Boolean
    var hasError: Boolean
    var isVisible: Boolean
}

class SimpleTask(override var title: String): DownloadItem {
    override val id: String = UUID.randomUUID().toString()
    override var progress by mutableStateOf(0f)
    override var sizeText by mutableStateOf("")
    override var speed by mutableStateOf("")
    override var isDone by mutableStateOf(false)
    override var hasError by mutableStateOf(false)
    override var isVisible by mutableStateOf(true)
}

class DownloadTask(override val title: String): DownloadItem {
    override val id: String = UUID.randomUUID().toString()
    override var progress by mutableStateOf(0f)
    override var sizeText by mutableStateOf("")
    override var speed by mutableStateOf("")
    override var isDone by mutableStateOf(false)
    override var hasError by mutableStateOf(false)
    override var isVisible by mutableStateOf(true)

    private var filesDownloadingCount = 0
    private var phase1SizeMiB = 0f
    private var currentSizeMiB = 0f

    fun updateFromLog(line: String) {
        // Empieza una fase de descarga (video/audio)
        if (line.contains("[download] Destination:")) {
            filesDownloadingCount++

            if (filesDownloadingCount == 2)
                phase1SizeMiB = currentSizeMiB // guardar el peso de la fase anterior para sumarlos después
        }

        // Buscar progreso %
        val pctMatch = Regex("""(\d+(?:\.\d+)?)%""").find(line)
        pctMatch?.groupValues?.get(1)?.toFloatOrNull()?.let { newPct ->
            val p = newPct / 100f

            // Video: 75% de la barra es del video y el otro 25% del audio
            // Audio: 100% de la barra es del audio
            progress = when (filesDownloadingCount) {
                0, 1 -> p * 0.75f
                2 -> 0.75f + (p * 0.25f)
                else -> 1f
            }
        }

        // Extraer MiB y sumar
        val sizeMatch = Regex("""of\s+~?([0-9.]+)([a-zA-Z]+)""").find(line)
        if (sizeMatch != null) {
            val num = sizeMatch.groupValues[1].toFloatOrNull() ?: 0f
            val unit = sizeMatch.groupValues[2].lowercase()

            // Pasar a MiB
            val sizeInMiB = when {
                unit.contains("gib") -> num * 1024f
                unit.contains("gb") -> num * 1000f * 1000f / 1024f / 1024f
                unit.contains("kib") -> num / 1024f
                unit.contains("kb") -> num * 1000f / 1024f / 1024f
                unit.contains("mib") -> num
                unit.contains("mb") -> num * 1000f * 1000f / 1024f / 1024f
                else -> num
            }
            currentSizeMiB = sizeInMiB

            //                         sumar                 pasar a MB
            val totalMB = (phase1SizeMiB + currentSizeMiB) * 1.048576
            sizeText = String.format("%.1f MB", totalMB)
        }

        // Extraer velocidad
        val speedMatch = Regex("""at\s+([a-zA-Z0-9./]+)""").find(line)
        speedMatch?.groupValues?.get(1)?.let { speed = it }

        // Detectar fases post-descarga
        if (line.contains("[Merger]")) {
            speed = "Uniendo video y audio..."
            progress = 1f
        } else if (line.contains("[ThumbnailsConvertor]") || line.contains("[EmbedThumbnail]")) {
            speed = "Procesando metadatos..."
        }

        if (line.lowercase().contains("error"))
            hasError = true
    }
}

