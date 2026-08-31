package dev.mycet.ydg.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class VideoFormat(
    @SerialName("format_id") val formatId: String = "",
    @SerialName("format_note") val note: String? = "", // 1080p, 720p, etc, nullable
    val ext: String = "",
    val height: Int? = null,
    val vcodec: String? = "none",
    val acodec: String? = "none",
    val filesize: Long? = null,
    @SerialName("filesize_approx") val filesizeApprox: Long? = null,
) {
    // Tamaño real aproximado en bytes
    val effectiveSize: Long? get() = filesize ?: filesizeApprox
    // Solo formato de video
    val isVideoFormat: Boolean get() = height != null && vcodec != null && vcodec != "none" && note != null

    val displaySize: String get() {
        val bytes = effectiveSize ?: 0
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1fGB".format(bytes / 1_000_000_000.0)
            bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / 1_000_000.0)
            else -> "%.0fKB".format(bytes / 1_000.0)
        }
    }
}