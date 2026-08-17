package dev.maigo.maigoloader.objects

import java.io.File

object Dependencies {
    fun ytDlpExists(): Boolean =
        File("${Prefs.ytDlpFolder}${File.separator}yt-dlp.exe").exists()
    fun ffmpegExists(): Boolean =
        File("${Prefs.ffmpegFolder}${File.separator}ffmpeg.exe").exists()

    // Chequea una lista de problemas que impidan el funcionamiento del programa
    fun check(): List<String> {
        val issues = mutableListOf<String>()
        if (Prefs.ytDlpFolder.isEmpty()) issues.add("yt-dlp folder not configured")
        else if (!ytDlpExists()) issues.add("yt-dlp.exe not found in configured folder")
        if (Prefs.ffmpegFolder.isEmpty()) issues.add("ffmpeg folder not configured")
        else if (!ffmpegExists()) issues.add("ffmpeg.exe not found in configured folder")
        return issues
    }
}