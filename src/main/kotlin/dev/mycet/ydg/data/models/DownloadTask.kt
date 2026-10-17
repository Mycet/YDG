package dev.mycet.ydg.data.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mycet.ydg.utils.toMB
import java.util.*

interface DownloadItem {
    val id: String
    val title: String
    var progress: Float
    var sizeText: String
    var speed: String
    var eta: String
    var statusPhase: String
    var isDone: Boolean
    var hasError: Boolean
    var isVisible: Boolean
}

class SimpleTask(override var title: String): DownloadItem {
    override val id: String = UUID.randomUUID().toString()
    override var progress by mutableStateOf(0f)
    override var sizeText by mutableStateOf("")
    override var speed by mutableStateOf("")
    override var eta by mutableStateOf("")
    override var statusPhase by mutableStateOf("")
    override var isDone by mutableStateOf(false)
    override var hasError by mutableStateOf(false)
    override var isVisible by mutableStateOf(true)
}

class DownloadTask(override val title: String): DownloadItem {
    override val id: String = UUID.randomUUID().toString()
    override var progress by mutableStateOf(0f)
    override var sizeText by mutableStateOf("")
    override var speed by mutableStateOf("")
    override var eta by mutableStateOf("")
    override var statusPhase by mutableStateOf("")
    override var isDone by mutableStateOf(false)
    override var hasError by mutableStateOf(false)
    override var isVisible by mutableStateOf(true)

    private var filesDownloadingCount = 0
    private var isAudioPhase = false
    private var phase1TotalMB = 0f
    private var currentPhaseTotalMB = 0f
    private var lastRawProgress = 0f

    fun updateFromLog(line: String) {
        // Empieza una fase de descarga (video/audio)
        if (line.contains("[download] Destination:") && currentPhaseTotalMB > 0f && !isAudioPhase) {
            isAudioPhase = true
            phase1TotalMB = currentPhaseTotalMB
        }

        // Buscar progreso % y verificar cambio de fase por caída de progreso
        val pctMatch = Regex("""(\d+(?:\.\d+)?)%""").find(line)
        val p = pctMatch?.groupValues?.get(1)?.toFloatOrNull()?.let { (it / 100f).coerceIn(0f, 1f) }
        if (p != null) {
            if (!isAudioPhase && lastRawProgress > 0.85f && p < 0.15f) {
                isAudioPhase = true
                phase1TotalMB = currentPhaseTotalMB
            }
            lastRawProgress = p
        }

        // Extraer peso total y pasar a MB
        val sizeMatch = Regex("""of\s+~?([0-9.]+)([a-zA-Z]+)""").find(line)
        if (sizeMatch != null) {
            val num = sizeMatch.groupValues[1].toFloatOrNull() ?: 0f
            val unit = sizeMatch.groupValues[2].lowercase()
            currentPhaseTotalMB = num.toMB(unit).toFloat()
        }

        // Cálculo de progreso y texto "descargado / total"
        if (p != null) {
            progress = if (isAudioPhase) (0.85f + (p * 0.15f)) else (p * 0.85f)
            val currentDownloadedMB = currentPhaseTotalMB * p
            val totalDownloadedMB = phase1TotalMB + currentDownloadedMB
            val grandTotalMB = phase1TotalMB + currentPhaseTotalMB
            if (grandTotalMB > 0f)
                sizeText = String.format(Locale.US, "%.1f / %.1f MB", totalDownloadedMB, grandTotalMB)
        }

        // Extraer velocidad
        val speedMatch = Regex("""at\s+~?([0-9.]+)([a-zA-Z/]+)""").find(line)
        if (speedMatch != null) {
            val num = speedMatch.groupValues[1].toFloatOrNull()
            val unit = speedMatch.groupValues[2].lowercase()
            if (num != null)
                speed = String.format(Locale.US, "%.1f MB/s", num.toMB(unit))
        }

        // Extraer ETA (tiempo restante)
        val etaMatch = Regex("""ETA\s+([0-9:]+)""").find(line)
        etaMatch?.groupValues?.get(1)?.let { eta = it }

        // Detectar fases post-descarga
        if (line.contains("[Merger]")) {
            statusPhase = "Merging..."
            speed = ""; eta = ""
            progress = 1f
        } else if (line.contains("[ThumbnailsConvertor]") || line.contains("[EmbedThumbnail]")) {
            statusPhase = "Metadata..."
            speed = ""; eta = ""
        }

        if (line.startsWith("ERROR:") || line.startsWith("Error:") || line.startsWith("[error]"))
            hasError = true
    }
}