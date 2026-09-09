package dev.mycet.ydg.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoDetails(
    val id: String = "",
    val title: String = "",
    val duration: Double = 0.0,
    val thumbnail: String = "",
    val uploader: String = "",
    @SerialName("webpage_url") val url: String = "",
    val formats: List<VideoFormat> = emptyList()
) {
    val videoExtensions: List<String> get() = formats
        .filter { it.isVideoFormat }
        .map { it.ext }
        .distinct()
        .sorted()
    fun videoFormatsForExt(ext: String): List<VideoFormat> = formats
        .filter { it.isVideoFormat
                && it.ext == ext
                && (it.effectiveSize ?: 0L) > 0L // Filtra los formatos sin tamaño (0 KB)
                && !(it.note?.contains("Premium", ignoreCase = true) ?: false) // Filtra los formatos premium
        }
        .groupBy { it.note }
        .mapValues { (_,group) -> group.maxByOrNull { it.effectiveSize ?: 0L }!! }
        .values
        .sortedByDescending { it.height }
}