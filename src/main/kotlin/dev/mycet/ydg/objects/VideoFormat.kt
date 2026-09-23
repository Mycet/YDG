package dev.mycet.ydg.objects

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
    @SerialName("vcodec") val videoCodec: String? = "none",
    @SerialName("acodec") val audioCodec: String? = "none",
    val filesize: Long? = null,
    @SerialName("filesize_approx") val filesizeApprox: Long? = null,
) {
    // Tamaño real aproximado en bytes
    val effectiveSize: Long? get() = filesize ?: filesizeApprox

    val isAudioFormat: Boolean get() = audioCodec != null && audioCodec != "none" && (videoCodec == null || videoCodec == "none")
    val isVideoFormat: Boolean get() = height != null && videoCodec != null && videoCodec != "none" && quality != null

    val displaySize: String get() {
        val bytes = effectiveSize ?: 0
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1fGB".format(bytes / 1_000_000_000.0)
            bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / 1_000_000.0)
            else -> "%.0fKB".format(bytes / 1_000.0)
        }
    }
}