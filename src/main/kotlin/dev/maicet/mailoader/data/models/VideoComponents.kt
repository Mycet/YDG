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
        get() {
            val raw = artist.ifEmpty { uploader }
            if (raw.contains(",")) {
                val uniqueArtists = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                return uniqueArtists.joinToString(", ")
            }
            return raw
        }

    val bestAudioSize: Long
        get() {
            val audioFormat = formats
                .filter { it.isAudioFormat }
                .maxByOrNull { it.audioBitrate ?: it.tbr ?: 0.0} ?: return 0L

            val size = audioFormat.effectiveSize ?: 0L
            if (size > 0L) return size

            // Si no reportan tamaño, calcular
            val bitrate = audioFormat.audioBitrate ?: audioFormat.tbr ?: 0.0
            if (bitrate > 0.0 && duration > 0.0)
                return ((bitrate * 1000.0 / 8.0) * duration).toLong()
            return 0L
        }

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

    fun videoFormatsForExt(ext: String): List<VideoFormat> {
        val isTwitter = url.contains("x.com", true) || url.contains("twitter.com", true)
        val isReddit = url.contains("reddit.com", true)

        return formats
            .filter { it.isVideoFormat
                    && it.extension == ext
                    && ((it.effectiveSize ?: 0L) > 0L || it.isPreMerged || it.quality.isNullOrBlank())   // Filtra los formatos sin tamaño (0 KB)
                    && !(it.quality?.contains("Premium", ignoreCase = true) ?: false) // Filtra los formatos premium
            }
            .groupBy { it.displayQuality }
            .mapValues { (_,group) ->
                val chosen = group.maxByOrNull { it.effectiveSize ?: 0L }!!

                if (isTwitter && chosen.height != null) {
                    val hlsTwin = formats.firstOrNull {
                        it.height == chosen.height && it.formatId.startsWith("hls-") && (it.tbr ?: 0.0) > 0.0
                    }
                    if (hlsTwin?.tbr != null && duration > 0.0) {
                        val calculatedBytes = ((hlsTwin.tbr * 1000.0 / 8.0) * duration).toLong()
                        chosen.copy(filesizeApprox = calculatedBytes)
                    } else chosen
                }
                // 2. Reddit: el tamaño del video ya contempla el total (o se calcula de tbr si es 0)
                else if (isReddit) {
                    val realSize = if ((chosen.effectiveSize ?: 0L) > 0L) {
                        chosen.effectiveSize
                    } else if ((chosen.tbr ?: 0.0) > 0.0 && duration > 0.0) {
                        ((chosen.tbr!! * 1000.0 / 8.0) * duration).toLong()
                    } else null
                    chosen.copy(filesizeApprox = realSize, audioCodec = "dash_included")
                }
                // 3. Otros sitios sin tamaño directo
                else if ((chosen.effectiveSize ?: 0L) == 0L && (chosen.tbr ?: 0.0) > 0.0 && duration > 0.0) {
                    val calculatedBytes = ((chosen.tbr!! * 1000.0 / 8.0) * duration).toLong()
                    chosen.copy(filesizeApprox = calculatedBytes, audioCodec = "dash_included")
                } else chosen
            }
            .values
            .sortedByDescending { it.height }
    }
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
        get() {
            val raw = artist.ifEmpty { uploader }
            if (raw.contains(",")) {
                val uniqueArtists = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                return uniqueArtists.joinToString(", ")
            }
            return raw
        }
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
    @SerialName("tbr") val tbr: Double? = null, // bitrate
) {
    // Tamaño real aproximado en bytes
    val effectiveSize: Long? get() = filesize ?: filesizeApprox

    val isVideoFormat: Boolean get() = height != null && videoCodec != "none"
    val isAudioFormat: Boolean get() = audioCodec != "none" && (videoCodec == "none" || height == null)

    // Para saber si el video ya trae el audio
    val isPreMerged: Boolean get() = isVideoFormat && audioCodec != null && audioCodec != "none"

    val displayQuality: String get() {
        if (height != null && height > 0) return "${height}p"
        if (!quality.isNullOrBlank() && !quality.contains("DASH", true)) return quality
        return "Unknown"
    }

    fun totalDisplaySize(audioSize: Long = 0L): String {
        val baseSize = effectiveSize ?: 0L
        val totalBytes = if (isVideoFormat && !isPreMerged) baseSize + audioSize else baseSize
        if (totalBytes <= 0L) return ""
        return totalBytes.formatBytes()
    }
}