package dev.maigo.ytdownload

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.function.Consumer

class CommandManager {
    fun downloadMedia(url: String, metadata: Boolean, audio: Boolean, thumbnail: Boolean, subs: Boolean, noPlayList: Boolean, onProgress: Consumer<String>) { //al parecer este es el equivalente a void
        val comando = ArrayList<String>()
        val separador: String = File.separator
        val rutaYtDlp: String = System.getProperty("user.dir") + separador + "bin" + separador + "yt-dlp.exe"
        comando.add(rutaYtDlp)

        if(metadata) comando.add("--add-metadata")
        if(audio) {
            comando.add("--extract-audio")
            comando.add("--audio-format")
            comando.add("mp3")
        }

        if(thumbnail) comando.add("--embed-thumbnail")
        if(subs) comando.add("--embed-subs")
        if(noPlayList) comando.add("--no-playlist")
        comando.add(url)

        try {
            val builder = ProcessBuilder(comando)
            builder.redirectErrorStream(true)
            val process: Process = builder.start()

            val reader = BufferedReader(InputStreamReader(process.getInputStream()))
            var line: String? = null

            while(reader.readLine().also { line = it } != null) {
                System.out.println(line)
                if(onProgress != null) onProgress.accept(line.toString()) // me encanta kotlin

            }
            process.waitFor()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun updateYtDlp(onProgress: Consumer<String>) { // y porq ue corcho esta no me pide ponerle unit
        val separador = File.separator // por que esto no me pide string pero la funcion de arriba si?
        val rutaYtDlp = System.getProperty("user.dir") + separador + "bin" + separador + "yt-dlp.exe"

        val comando = ArrayList<String>()
        comando.add(rutaYtDlp)
        comando.add("-U")

        try {
            val builder: ProcessBuilder = ProcessBuilder(comando)
            builder.redirectErrorStream(true)
            val process: Process = builder.start()

            val reader = BufferedReader(InputStreamReader(process.getInputStream()))
            var line: String? = null
            while(reader.readLine().also { line = it } != null) {
                System.out.println(line)
                if(onProgress != null) onProgress.accept(line.toString())
            }
        } catch(ex: Exception) {
            ex.printStackTrace()
        }

    }
}