package dev.mycet.ydg.core

import androidx.compose.ui.graphics.Color
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.ui.theme.AppTheme
import java.io.File

enum class DependencyState {
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    NOT_FOUND,
    CHECKING
}

data class DependencyStatus(
    val state: DependencyState = DependencyState.CHECKING,
    val text: String = "Checking..."
) {
    val color: Color?
        get() = when (state) {
            DependencyState.UP_TO_DATE -> AppTheme.Green
            DependencyState.UPDATE_AVAILABLE -> AppTheme.Yellow
            DependencyState.NOT_FOUND -> AppTheme.Red
            DependencyState.CHECKING -> null
        }
    val isInstalled: Boolean
        get() = state == DependencyState.UP_TO_DATE || state == DependencyState.UPDATE_AVAILABLE
}

object Dependencies {
    fun ytDlpExists(): Boolean =
        File("${Prefs.ytDlpFolder}${File.separator}yt-dlp.exe").exists()
    fun ffmpegExists(): Boolean =
        File("${Prefs.ffmpegFolder}${File.separator}ffmpeg.exe").exists()
    fun denoExists(): Boolean =
        File("${Prefs.denoFolder}${File.separator}deno.exe").exists()

    // Chequea una lista de problemas que impidan el funcionamiento del programa
    fun check(): List<String> {
        val issues = mutableListOf<String>()
        if (Prefs.ytDlpFolder.isEmpty()) issues.add("yt-dlp folder not configured")
        else if (!ytDlpExists()) issues.add("yt-dlp.exe not found in configured folder")
        if (Prefs.ffmpegFolder.isEmpty()) issues.add("ffmpeg folder not configured")
        else if (!ffmpegExists()) issues.add("ffmpeg.exe not found in configured folder")
        if (Prefs.denoFolder.isEmpty()) issues.add("deno folder not configured")
        else if (!denoExists()) issues.add("deno.exe not found in configured folder")
        return issues
    }

    fun getYtDlpStatus(): DependencyStatus {
        if (Prefs.ytDlpFolder.isEmpty()) return DependencyStatus(state = DependencyState.NOT_FOUND, text = "Not configured")
        val file = File(Prefs.ytDlpFolder, "yt-dlp.exe")
        if (!file.exists()) return DependencyStatus(state = DependencyState.NOT_FOUND, text = "Not found")

        val version = runCatching {
            var result = ProcessBuilder(file.absolutePath, "--version")
                .start()
                .inputStream.bufferedReader().readLine()?.trim()
            if (result != null)
                result = Regex("""\d+(\.\d+)+""").find(result)?.value
            result
        }.getOrNull() ?: return DependencyStatus(state = DependencyState.UP_TO_DATE, text = "Installed")

        return DependencyStatus(state = DependencyState.UP_TO_DATE, text = version)
    }

    fun getFfmpegStatus(): DependencyStatus {
        if (Prefs.ffmpegFolder.isEmpty()) return DependencyStatus(state = DependencyState.NOT_FOUND, text = "Not configured")
        val file = File(Prefs.ffmpegFolder, "ffmpeg.exe")
        if (!file.exists()) return DependencyStatus(state = DependencyState.NOT_FOUND, text = "Not found")
        val version =  runCatching {
            var line = ProcessBuilder(file.absolutePath, "-version")
                .start()
                .inputStream.bufferedReader().readLine()
            if (line != null)
                line = Regex("""\d+(\.\d+)+""").find(line)?.value
            line
        }.getOrNull() ?: return DependencyStatus(state = DependencyState.UP_TO_DATE, text = "Installed")

        return DependencyStatus(state = DependencyState.UP_TO_DATE, text = version)
    }

    fun getDenoStatus(): DependencyStatus {
        if (Prefs.denoFolder.isEmpty()) return DependencyStatus(state = DependencyState.NOT_FOUND, text = "Not configured")
        val file = File(Prefs.denoFolder, "deno.exe")
        if (!file.exists()) return DependencyStatus(state = DependencyState.NOT_FOUND, text = "Not found")

        val version = runCatching {
            var result = ProcessBuilder(file.absolutePath, "--version")
                .start()
                .inputStream.bufferedReader().readLine()?.trim()
            if (result != null)
                result = Regex("""\d+(\.\d+)+""").find(result)?.value
            result
        }.getOrNull() ?: return DependencyStatus(state = DependencyState.UP_TO_DATE, text = "Installed")

        return DependencyStatus(state = DependencyState.UP_TO_DATE, text = version)
    }
}