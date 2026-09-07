package dev.mycet.ydg.objects

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

// Es mejor práctica usar un archivo properties que usar el registro
object Prefs {
    // Archivo local, se guarda en la carpeta donde se ejecute el programa
    private val configFile = File("config.properties")
    private val properties = Properties()

    init {
        // Al arrancar, si el archivo existe, se cargan las preferencias
        if (configFile.exists()) {
            try {
                FileInputStream(configFile).use { properties.load(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Función interna para guardar el archivo cada vez que algo cambia
    private fun save() {
        try {
            FileOutputStream(configFile).use { properties.store(it, "YDG Configuration") }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var downloadFolder: String
        get() = properties.getProperty("downloadFolder", "")
        set(value) {
            properties.setProperty("downloadFolder", value)
            save()
        }

    var ytDlpFolder: String
        get() = properties.getProperty("ytDlpFolder", "")
        set(value) {
            properties.setProperty("ytDlpFolder", value)
            save()
        }

    var ffmpegFolder: String
        get() = properties.getProperty("ffmpegFolder", "")
        set(value) {
            properties.setProperty("ffmpegFolder", value)
            save()
        }

    var startupTab: String
        get() = properties.getProperty("startupTab", "Last Used")
        set(value) {
            properties.setProperty("startupTab", value)
            save()
        }
    var lastActiveTab: String
        get() = properties.getProperty("lastActiveTab", "VIDEO")
        set(value) {
            properties.setProperty("lastActiveTab", value)
            save()
        }
}