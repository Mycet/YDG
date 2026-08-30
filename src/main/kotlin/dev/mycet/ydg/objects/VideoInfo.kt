package dev.mycet.ydg.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    val id: String = "",
    val title: String = "",
    val duration: Int =  0,
    @SerialName("thumbnail")
    val thumbnail: String = "",
    @SerialName("webpage_url")
    val url: String = "",
    val uploader: String = ""
)