package dev.mycet.ydg.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoFormat(
    // @SerialName es para hacerle un override y renombrar el campo,
    // ya que el json con la data tiene nombres poco amigables ('abr' para audioBitrate por ejemplo)
    @SerialName("format_id") val formatId: String = "",
    @SerialName("format_note") val quality: String? = "", // 1080p, 720p, etc, nullable
    @SerialName("abr") val audioBitrate: Double? = null,
    @SerialName("ext") val extension: String = "",
    val height: Int? = null,
    @SerialName("vcodec") val videoCodec: String? = null,
    @SerialName("acodec") val audioCodec: String? = null,
    val filesize: Long? = null,
    @SerialName("filesize_approx") val filesizeApprox: Long? = null,
) {
    // Tamaño real aproximado en bytes
    val effectiveSize: Long? get() = filesize ?: filesizeApprox

    val isVideoFormat: Boolean get() = height != null && videoCodec != "none"
    val isAudioFormat: Boolean get() = audioCodec != "none" && (videoCodec == "none" || height == null)

    // Para saber si el video ya trae el audio
    val isPreMerged: Boolean get() = isVideoFormat && audioCodec != null && audioCodec != "none"

    val displayQuality: String get() {
        if (!quality.isNullOrBlank()) return quality
        if (height != null) return "${height}p"
        return "Unknown"
    }

    val displaySize: String get() {
        val bytes = effectiveSize ?: 0
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1fGB".format(bytes / 1_000_000_000.0)
            bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / 1_000_000.0)
            else -> "%.0fKB".format(bytes / 1_000.0)
        }
    }
}