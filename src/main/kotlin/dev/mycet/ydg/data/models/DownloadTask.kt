package dev.mycet.ydg.data.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.utils.formatProgress
import dev.mycet.ydg.utils.toMB
import java.io.File
import java.util.*

interface DownloadItem {
    val id: String
    val title: String
    var progress: Float
    var sizeText: String
    var speed: String
    var eta: String
    var statusPhase: String
    var errorMessage: String
    var isCompleted: Boolean
    var hasError: Boolean
    var isVisible: Boolean
    var onCancel: (() -> Unit)?
    var onRetry: (() -> Unit)?
    fun cancel() {
        onCancel?.invoke()
    }
    fun retry() {
        onRetry?.invoke()
    }
}

class SimpleTask(override var title: String): DownloadItem {
    override val id: String = UUID.randomUUID().toString()
    override var progress by mutableStateOf(0f)
    override var sizeText by mutableStateOf("")
    override var speed by mutableStateOf("")
    override var eta by mutableStateOf("")
    override var statusPhase by mutableStateOf("")
    override var errorMessage by mutableStateOf("")
    override var isCompleted by mutableStateOf(false)
    override var hasError by mutableStateOf(false)
    override var isVisible by mutableStateOf(true)
    override var onCancel: (() -> Unit)? = null
    override var onRetry: (() -> Unit)? = null
}

class DownloadTask(override val title: String): DownloadItem {
    override val id: String = UUID.randomUUID().toString()
    override var progress by mutableStateOf(0f)
    override var sizeText by mutableStateOf("")
    override var speed by mutableStateOf("")
    override var eta by mutableStateOf("")
    override var statusPhase by mutableStateOf("")
    override var errorMessage by mutableStateOf("")
    override var isCompleted by mutableStateOf(false)
    override var hasError by mutableStateOf(false)
    override var isVisible by mutableStateOf(true)
    override var onCancel: (() -> Unit)? = null
    override var onRetry: (() -> Unit)? = null

    private var isAudioPhase = false
    private var phase1TotalMB = 0f
    private var currentPhaseTotalMB = 0f
    private var lastRawProgress = 0f

    fun resetProgress() {
        progress = 0f
        sizeText = ""
        speed = ""
        eta = ""
        statusPhase = ""
        isCompleted = false
        hasError = false
        isAudioPhase = false
        phase1TotalMB = 0f
        currentPhaseTotalMB = 0f
        lastRawProgress = 0f
        trackedFiles.clear()
    }

    fun updateFromLog(line: String) {
        // Capturar rutas de archivos creados por yt-dlp para limpiarlos si se cancela
        if (line.contains("Destination:")) {
            val destPath = line.substringAfter("Destination:").trim().trim('"', '\'')
            if (destPath.isNotBlank()) trackedFiles.add(destPath)
        } else if (line.contains("Writing video thumbnail") || (line.contains("[info]") && line.contains("to:"))) {
            val thumbPath = line.substringAfter("to:").trim().trim('"', '\'')
            if (thumbPath.isNotBlank()) trackedFiles.add(thumbPath)
        } else if (line.contains("Merging formats into")) {
            val mergePath = line.substringAfter("Merging formats into").trim().trim('"', '\'')
            if (mergePath.isNotBlank()) trackedFiles.add(mergePath)
        }

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
                sizeText = formatProgress(totalDownloadedMB.toDouble(), grandTotalMB.toDouble())
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
            statusPhase = "Merging…"
            speed = ""; eta = ""
            progress = 1f
        } else if (line.contains("[ThumbnailsConvertor]") || line.contains("[EmbedThumbnail]")) {
            statusPhase = "Writing metadata…"
            speed = ""; eta = ""
        }

        if (line.startsWith("ERROR:") || line.startsWith("Error:") || line.startsWith("[error]")) {
            hasError = true
            errorMessage = line.replace(Regex("""^(ERROR|Error|\[error]):?\s*"""), "").trim()
        }
    }


    val trackedFiles = mutableSetOf<String>()

    fun cleanResidualFiles() {
        trackedFiles.forEach { path ->
            deleteFileSafely(File(path))
            deleteFileSafely(File("$path.part"))
            deleteFileSafely(File("$path.ytdl"))
        }

        val folder = File(Prefs.downloadFolder)
        val cleanTitle = title.replace("\"", "'").replace(Regex("[\\\\/:*?<>|]"), "_").trim()
        if (folder.exists() && folder.isDirectory && cleanTitle.length >= 3) {
            val residualExtensions = listOf("part", "ytdl", "temp", "webp", "jpg", "jpeg", "png", "mp4", "m4a", "webm", "mp3")
            folder.listFiles()?.forEach { file ->
                if (
                    file.isFile
                    && file.name.startsWith(cleanTitle, true)
                    && file.extension.lowercase() in residualExtensions
                ) { deleteFileSafely(file) }
            }
        }
    }

    private fun deleteFileSafely(file: File) {
        if (!file.exists()) return
        for (i in 1..5) {
            if (file.delete() || !file.exists()) return
            try { Thread.sleep(80) } catch (_: Exception) {}
        }
    }
}