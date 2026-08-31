package dev.mycet.ydg.ytdownload

import dev.mycet.ydg.objects.Prefs
import dev.mycet.ydg.objects.VideoDetails
import dev.mycet.ydg.objects.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipFile

private val json = Json { ignoreUnknownKeys = true }

// object en vez de class porque no necesita instanciarse, se llama directamente
object CommandManager {
    private fun ytDlpPath(): String {
        return "${Prefs.ytDlpFolder}${File.separator}yt-dlp.exe"
    }

    // 'suspend' es parecido a 'new Thread', es para que no congele el programa
    private suspend fun ejecutar(comando: List<String>, onProgress: (String) -> Unit) {
        // withContext hace que ejecute en otro thread en lugar del Main
        // IO para operaciones bloqueantes (leer archivos, procesos, red)
        // Swing para cambios de estado de Compose, cosas que modifiquen la UI
        withContext(Dispatchers.IO) {
            try {
                val proceso = ProcessBuilder(comando)
                    .redirectErrorStream(true)
                    .start()

                val reader = proceso.inputStream.bufferedReader()
                var linea = reader.readLine()
                while (linea != null) {
                    println(linea)
                    withContext(Dispatchers.Swing) {
                        onProgress(linea)
                    }
                    linea = reader.readLine()
                }

                proceso.waitFor()
            } catch (ex: Exception) {
                withContext(Dispatchers.Swing) {
                    // ${ } o $ es para insertar variables o resultados de funciones u operaciones
                    // Se usa ${} para lo segundo, o para variables cuando es ambiguo y puede confundirse con el texto
                    // Si kotlin no puede leer y distinguir entre variable y texto, te va a pedir {}
                    // val sep = File.separator
                    // $sepbin no se entiende donde termina la variable, entonces te pide llaves -> ${sep}bin -> /bin
                    onProgress("Error: ${ex.message}")
                }
            }
        }
    }

    suspend fun downloadVideo(
        url: String,
        title: String,
        format: String,
        onProgress: (String) -> Unit
    ) {
        val ytDlp = ytDlpPath()
        val comando = mutableListOf(ytDlp).apply { // .apply permite modificar directamente al crearlo
            val outputTitle = title.ifEmpty { "%(title)s" }
            addAll(listOf("--output", "${Prefs.downloadFolder}${File.separator}$outputTitle.%(ext)s"))
            addAll(listOf("--format", format.lowercase()))

            add("--add-metadata")
            add("--embed-thumbnail")
            add(url)
        }

        ejecutar(comando, onProgress);
    }

    suspend fun downloadAudio(
        url: String,
        format: String,
        metadata: Boolean,
        thumbnail: Boolean,
        noPlayList: Boolean,
        onProgress: (String) -> Unit
    ) {
        val ytDlp = ytDlpPath()
        val comando = mutableListOf(ytDlp).apply { // .apply permite modificar directamente al crearlo
            addAll(listOf("--output", "${Prefs.downloadFolder}${File.separator}%(title)s.%(ext)s"))
            addAll(listOf("--extract-audio", "--audio-format", format.lowercase()))

            if (metadata) add("--add-metadata")
            if (thumbnail) add("--embed-thumbnail")
            if (noPlayList) add("--no-playlist")
            add(url)
        }

        ejecutar(comando, onProgress);
    }

    suspend fun updateYtDlp(onProgress: (String) -> Unit) {
        val comando = mutableListOf(ytDlpPath(), "-U")
        ejecutar(comando, onProgress)
    }

    suspend fun fetchVideoInfo(url: String, playlistStart: Int = 1, playlistEnd: Int = 50): List<VideoInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val comando = mutableListOf(
                    ytDlpPath(),
                    "--dump-json",
                    "--flat-playlist",
                    "--playlist-start", playlistStart.toString(),
                    "--playlist-end", playlistEnd.toString(),
                    url
                )

                val proceso = ProcessBuilder(comando)
                    .redirectErrorStream(true)
                    .start()

                val results = mutableListOf<VideoInfo>()
                val reader = proceso.inputStream.bufferedReader()
                var linea = reader.readLine()

                while (linea != null) {
                    if (linea.startsWith("{")) { // JSON
                        try {
                            if (linea.startsWith("{")) {
                                println("DURATION RAW: ${Json.parseToJsonElement(linea).jsonObject["duration"]}")
                                // ... resto igual
                            }
                            val info = json
                                .decodeFromString<VideoInfo>(linea)
                            results.add(info)
                        } catch (ex: Exception) {}
                    }
                    linea = reader.readLine()
                }

                results
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    suspend fun fetchVideoDetails(url: String): VideoDetails? {
        return withContext(Dispatchers.IO) {
            try {
                val proceso = ProcessBuilder(listOf(ytDlpPath(), "--dump-json", url))
                    .redirectErrorStream(true)
                    .start()

                val output = proceso.inputStream.bufferedReader().readText()
                proceso.waitFor()

                val line = output.lines().firstOrNull { it.startsWith("{") } ?: return@withContext null
                println("DETAILS DURATION RAW: ${Json.parseToJsonElement(line).jsonObject["duration"]}")
                json.decodeFromString<VideoDetails>(line)

            } catch (ex: Exception) {
                null
            }
        }
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

    suspend fun downloadAndExtractFfmpeg(destFolder: String, onProgress: (String) -> Unit) {
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
}