package dev.mycet.ydg.core

import dev.mycet.ydg.data.models.VideoDetails
import dev.mycet.ydg.data.models.VideoInfo
import dev.mycet.ydg.data.prefs.Prefs
import dev.mycet.ydg.ui.navigation.screens.MediaOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipFile
import kotlin.coroutines.cancellation.CancellationException

private val json = Json { ignoreUnknownKeys = true }

// object en vez de class porque no necesita instanciarse, se llama directamente
object CommandManager {
    private fun ytDlpPath(): String {
        return "${Prefs.ytDlpFolder}${File.separator}yt-dlp.exe"
    }

    fun killProcessTree(process: Process?) {
        if (process == null) return
        try {
            val descendants = process.descendants()
            val pid = process.pid()
            ProcessBuilder("taskkill", "/F", "/T", "/PID", pid.toString())
                .redirectErrorStream(true)
                .start()
                .waitFor()

            descendants.forEach { runCatching { it.destroyForcibly() } }
            process.destroyForcibly()
        } catch (_: Exception) {
            process.descendants().forEach { runCatching { it.destroyForcibly() } }
            process.destroyForcibly()
        }
    }

    // 'suspend' es parecido a 'new Thread', es para que no congele el programa
    private suspend fun execute(
        comando: List<String>,
        onProgress: (String) -> Unit,
        onProcess: ((Process) -> Unit)? = null
    ) {
        // withContext hace que ejecute en otro thread en lugar del Main
        // IO para operaciones bloqueantes (leer archivos, procesos, red)
        // Swing para cambios de estado de Compose, cosas que modifiquen la UI
        withContext(Dispatchers.IO) {
            ensureActive()
            var process: Process? = null
            try {
                process = ProcessBuilder(comando)
                    .redirectErrorStream(true)
                    .start()

                onProcess?.invoke(process)
                ensureActive()

                val reader = process.inputStream.bufferedReader()
                var linea = reader.readLine()
                while (linea != null) {
                    ensureActive()
                    println("[yt-dlp]: $linea")
                    withContext(Dispatchers.Swing) {
                        onProgress(linea)
                    }
                    linea = reader.readLine()
                }

                process.waitFor()
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                ex.printStackTrace()
                withContext(Dispatchers.Swing) {
                    onProgress("Error: ${ex.message}")
                }
            } finally {
                killProcessTree(process)
            }
        }
    }

    suspend fun downloadVideo(
        url: String,
        options: MediaOptions,
        formatId: String, audioFormatId: String?,
        extension: String,
        onProgress: (String) -> Unit,
        onProcess: ((Process) -> Unit)? = null
    ) {
        val audio = audioFormatId ?: "bestaudio"
        val ytDlp = ytDlpPath()
        val comando = mutableListOf(ytDlp).apply { // .apply permite modificar tras crear
            addAll(listOf("--format", "$formatId+$audio/best"))
            addAll(listOf("--merge-output-format", extension.lowercase().trim()))
            addCommonMediaArgs(url, options, this)
        }

        execute(comando, onProgress, onProcess);
    }

    suspend fun downloadAudio(
        url: String,
        options: MediaOptions,
        format: String,
        formatId: String,
        bitrate: Int,
        onProgress: (String) -> Unit,
        onProcess: ((Process) -> Unit)? = null
    ) {
        val ytDlp = ytDlpPath()
        val comando = mutableListOf(ytDlp).apply { // .apply permite modificar directamente al crearlo
            addAll(listOf("--format", formatId))
            addAll(listOf("--extract-audio", "--audio-format", format.lowercase()))
            addAll(listOf("--audio-quality", "${bitrate}K"))
            addCommonMediaArgs(url, options, this)
        }

        execute(comando, onProgress, onProcess);
    }

    suspend fun updateYtDlp(onProgress: (String) -> Unit) {
        val comando = mutableListOf(ytDlpPath(), "-U")
        execute(comando, onProgress)
    }

    fun addCommonMediaArgs(url: String, options: MediaOptions, cmd: MutableList<String>) {
        val safeTitle = options.title.replace("\"", "'").replace(Regex("[\\\\/:*?<>|]"), "_")
        val outputTitle = safeTitle.ifEmpty { "%(title)s" }
        cmd.addAll(listOf("--output", "${Prefs.downloadFolder}${File.separator}$outputTitle.%(ext)s"))

        cmd.addAll(listOf("--ffmpeg-location", Prefs.ffmpegFolder))

        if (Prefs.browserForCookies.isNotBlank())
            cmd.addAll(listOf("--cookies-from-browser", Prefs.browserForCookies.trim().lowercase()))

        if (File("${Prefs.ffmpegFolder}${File.separator}deno.exe").exists())
            cmd.addAll(listOf("--js-runtimes", "deno"))

        if (options.addMetadata) {
            cmd.add("--add-metadata")
            if (options.title.isNotBlank()) {
                val cleanTitle = options.title.replace("\"", "'")
                cmd.addAll(listOf("--parse-metadata", "$cleanTitle:%(title)s"))
            }
            if (options.author.isNotBlank()) {
                val cleanAuthor = options.author.replace("\"", "'")
                cmd.addAll(listOf("--parse-metadata", "$cleanAuthor:%(artist)s"))
                cmd.addAll(listOf("--parse-metadata", "$cleanAuthor:%(uploader)s"))
            }
            if (options.album.isNotBlank()) {
                val cleanAlbum = options.album.replace("\"", "'")
                cmd.addAll(listOf("--parse-metadata", "$cleanAlbum:%(album)s"))
            }
        }

        if (options.embedThumbnail) cmd.add("--embed-thumbnail")
        if (options.embedSubs) cmd.add("--embed-subs")
        if (options.sponsorBlock) cmd.addAll(listOf("--sponsorblock-remove", "all"))

        val targetUrl = if (url.contains("reddit.com", ignoreCase = true)) {
            url.replace(Regex("""https?://(www\.)?reddit\.com"""), "https://old.reddit.com")
        } else url
        if (isReddit(url)) cmd.addAll(listOf("--impersonate", "chrome"))
        cmd.add(targetUrl)
    }

    suspend fun fetchVideoInfo(
        url: String,
        playlistStart: Int = 1,
        playlistEnd: Int = 50,
    ): List<VideoInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val comando = mutableListOf(
                    ytDlpPath(),
                    "--dump-json",
                    "--flat-playlist",
                    "--playlist-start", playlistStart.toString(),
                    "--playlist-end", playlistEnd.toString()
                ).apply {
                    if (Prefs.browserForCookies.isNotBlank())
                        addAll(listOf("--cookies-from-browser", Prefs.browserForCookies.trim().lowercase()))

                    val denoExe = "${Prefs.ffmpegFolder}${File.separator}deno.exe"
                    if (File(denoExe).exists())
                        addAll(listOf("--js-runtimes", denoExe))

                    add(url)
                }

                val proceso = ProcessBuilder(comando)
                    .redirectErrorStream(true)
                    .start()

                val results = mutableListOf<VideoInfo>()
                val reader = proceso.inputStream.bufferedReader()
                var linea = reader.readLine()

                while (linea != null) {
                    if (linea.startsWith("{")) { // JSON
                        try {
                            val info = json.decodeFromString<VideoInfo>(linea)
                            results.add(info)
                        } catch (ex: Exception) {}
                    }
                    linea = reader.readLine()
                }

                results
            } catch (ex: Exception) {
                ex.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun fetchVideoDetails(url: String): VideoDetails? = withContext(Dispatchers.IO) {
        val reddit = isReddit(url)
        val cleanUrl =
            if (reddit) url.replace(Regex("""https?://(www\.)?reddit\.com"""), "https://old.reddit.com")
            else url

        // null = sin impersonation (YouTube y otros); Reddit prueba varios
        val targets: List<String?> = if (reddit) listOf("chrome", "safari", "firefox") else listOf(null)

        for (target in targets) {
            try {
                val comando = mutableListOf(ytDlpPath(), "--dump-json", "--no-playlist").apply {
                    if (Prefs.browserForCookies.isNotBlank())
                        addAll(listOf("--cookies-from-browser", Prefs.browserForCookies.trim().lowercase()))

                    val denoExe = "${Prefs.ffmpegFolder}${File.separator}deno.exe"
                    if (File(denoExe).exists())
                        addAll(listOf("--js-runtimes", denoExe))

                    if (target != null) addAll(listOf("--impersonate", target))
                    add(cleanUrl)
                }

                val proceso = ProcessBuilder(comando).redirectErrorStream(true).start()
                val output = proceso.inputStream.bufferedReader().readText()
                proceso.waitFor()

                val line = output.lines().firstOrNull { it.startsWith("{") }
                if (line != null) return@withContext json.decodeFromString<VideoDetails>(line)
                println("DEBUG_YTDLP target=$target output:\n$output")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        null
    }

    suspend fun downloadFile(url: String, destPath: String, onProgress: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.connect()

                val totalBytes = connection.contentLengthLong
                val inputStream = connection.inputStream
                val outputFile = File(destPath)
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(8192) // lee de a 8kb, para mostrar progreso y no consumir mucha memoria
                var bytesRead: Int
                var totalRead = 0L

                // inputStream.read devuelve cantidad de bytes leídos o -1 si no hay más
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    val percent = if (totalBytes > 0) (totalRead * 100 / totalBytes).toInt() else 0
                    withContext(Dispatchers.Swing) {
                        onProgress("Downloading... $percent% (${totalRead / 1024}KB / ${totalBytes / 1024}KB)")
                    }
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                withContext(Dispatchers.Swing) {
                    onProgress("Done — saved to $destPath")
                }

            } catch (ex: Exception) {
                withContext(Dispatchers.Swing) {
                    onProgress("Error: ${ex.message}")
                }
            }
        }
    }

    suspend fun downloadFfmpeg(destFolder: String, onProgress: (String) -> Unit) {
        val zipPath = "$destFolder${File.separator}ffmpeg.zip"

        // Descarga el zip
        downloadFile(
            url = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip",
            destPath = zipPath,
            onProgress = onProgress
        )

        withContext(Dispatchers.IO) {
            try {
                // Extrae solo ffmpeg.exe del zip
                withContext(Dispatchers.Swing) { onProgress("Extracting ffmpeg.exe...") }
                val zipFile = ZipFile(zipPath)
                val entry = zipFile.entries().asSequence()
                    .firstOrNull { it.name.endsWith("ffmpeg.exe") && !it.isDirectory }

                if (entry != null) {
                    val input = zipFile.getInputStream(entry)
                    val output = FileOutputStream("$destFolder${File.separator}ffmpeg.exe")
                    input.copyTo(output)
                    output.close()
                    input.close()
                }
                zipFile.close()

                // Borra el zip temporal
                File(zipPath).delete()

                withContext(Dispatchers.Swing) { onProgress("Done — ffmpeg.exe saved to $destFolder") }

            } catch (ex: Exception) {
                withContext(Dispatchers.Swing) { onProgress("Error: ${ex.message}") }
            }
        }
    }

    suspend fun downloadDeno(destFolder: String, onProgress: (String) -> Unit) {
        val zipPath = "$destFolder${File.separator}deno.zip"

        downloadFile(
            url = "https://github.com/denoland/deno/releases/latest/download/deno-x86_64-pc-windows-msvc.zip",
            destPath = zipPath,
            onProgress = onProgress
        )
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Swing) { onProgress("Extracting deno.exe...") }
                val zipFile = ZipFile(zipPath)

                val entry = zipFile.entries().asSequence()
                    .firstOrNull { it.name == "deno.exe" }
                if (entry != null) {
                    val input = zipFile.getInputStream(entry)
                    val output = FileOutputStream("$destFolder${File.separator}deno.exe")
                    input.copyTo(output)
                    output.close()
                    input.close()
                }
                zipFile.close()

                File(zipPath).delete()
                withContext(Dispatchers.Swing) { onProgress("Done — deno.exe saved to $destFolder") }
            } catch (ex: Exception) {
                withContext(Dispatchers.Swing) { onProgress("Error: ${ex.message}") }
            }
        }
    }
}

fun isReddit(url: String): Boolean = url.contains("reddit.com", ignoreCase = true)
fun isTwitter(url: String): Boolean = url.contains("x.com", ignoreCase = true) || url.contains("twitter.com", ignoreCase = true)