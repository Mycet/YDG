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
    val formats: List<VideoFormat> = emptyList(),
) {
    val audioExtensions: List<String> get() = listOf("mp3", "m4a", "wav", "flac") // para audio no es necesario ver los disponibles, yt-dlp lo convierte
    val videoExtensions: List<String> get() = formats
        .filter { it.isVideoFormat }
        .map { it.extension }
        .distinct()
        .sorted()

    fun audioFormatsForExt(ext: String): List<VideoFormat> = formats
        .filter { it.isAudioFormat && (it.effectiveSize ?: 0L) > 0L }
        .groupBy { it.audioBitrate ?: 0.0 }
        .mapValues { (_,group) -> group.maxByOrNull { it.effectiveSize ?: 0L }!! }
        .values
        .sortedByDescending { it.audioBitrate ?: 0.0 }

    fun videoFormatsForExt(ext: String): List<VideoFormat> = formats
        .filter { it.isVideoFormat
                && it.extension == ext
                && (it.effectiveSize ?: 0L) > 0L // Filtra los formatos sin tamaño (0 KB)
                && !(it.quality?.contains("Premium", ignoreCase = true) ?: false) // Filtra los formatos premium
        }
        .groupBy { it.quality }
        .mapValues { (_,group) -> group.maxByOrNull { it.effectiveSize ?: 0L }!! }
        .values
        .sortedByDescending { it.height }
}