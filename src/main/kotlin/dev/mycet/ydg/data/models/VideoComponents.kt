package dev.mycet.ydg.data.models

import dev.mycet.ydg.utils.formatBytes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoDetails(
    val id: String = "",
    val title: String = "", val artist: String = "", val album: String = "",
    val uploader: String = "",
    val duration: Double = 0.0,
    val thumbnail: String = "",
    @SerialName("webpage_url") val url: String = "",
    val formats: List<VideoFormat> = emptyList(),
) {
    val displayAuthor: String
        get() = artist.ifEmpty { uploader }

    val bestAudioSize: Long
        get() = formats
            .filter { it.isAudioFormat }
            .maxByOrNull { it.audioBitrate ?: 0.0 }
            ?.effectiveSize ?: 0L

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
                && ((it.effectiveSize ?: 0L) > 0L || it.isPreMerged || it.quality.isNullOrBlank())   // Filtra los formatos sin tamaño (0 KB)
                && !(it.quality?.contains("Premium", ignoreCase = true) ?: false) // Filtra los formatos premium
        }
        .groupBy { it.displayQuality }
        .mapValues { (_,group) -> group.maxByOrNull { it.effectiveSize ?: 0L }!! }
        .values
        .sortedByDescending { it.height }
}



@Serializable
data class VideoInfo(
    val id: String = "",
    val title: String = "", val artist: String = "", val album: String = "",
    val uploader: String = "",
    val duration: Double = 0.0,
    @SerialName("thumbnail")
    val thumbnail: String = "",
    @SerialName("webpage_url")
    val url: String = "",
) {
    val displayAuthor: String
        get() = artist.ifEmpty { uploader }
}

fun VideoInfo.toVideoDetails(fallbackUrl: String = "") = VideoDetails(
    id = id,
    title = title, artist = artist, album = album,
    uploader = uploader,
    duration = duration,
    thumbnail = thumbnail,
    url = url.ifBlank { fallbackUrl }
)



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

    fun totalDisplaySize(audioSize: Long = 0L): String {
        val baseSize = effectiveSize ?: 0L
        val totalBytes = if (isVideoFormat && !isPreMerged) baseSize + audioSize else baseSize
        if (totalBytes <= 0L) return ""
        return totalBytes.formatBytes()
    }
}