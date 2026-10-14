package dev.mycet.ydg.data.prefs

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
    var denoFolder: String
        get() = properties.getProperty("denoFolder", "")
        set(value) {
            properties.setProperty("denoFolder", value)
            save()
        }

    var startupTab: String
        get() = properties.getProperty("startupTab", "Last Used")
        set(value) {
            properties.setProperty("startupTab", value)
            save()
        }
    var openOnStartup: String
        get() = properties.getProperty("lastActiveTab", "MEDIA")
        set(value) {
            properties.setProperty("lastActiveTab", value)
            save()
        }

    var browserForCookies: String
        get() = properties.getProperty("browserForCookies", "")
        set(value) {
            properties.setProperty("browserForCookies", value)
            save()
        }

    var rememberMediaSettings: Boolean
        get() = properties.getProperty("rememberMediaSettings", "false").toBoolean()
        set(value) { properties.setProperty("rememberMediaSettings", value.toString()); save() }

    var embedThumbnail: Boolean
        get() = properties.getProperty("embedThumbnail", "true").toBoolean()
        set(value) { properties.setProperty("embedThumbnail", value.toString()); save() }

    var addMetadata: Boolean
        get() = properties.getProperty("addMetadata", "true").toBoolean()
        set(value) { properties.setProperty("addMetadata", value.toString()); save() }

    var embedSubs: Boolean
        get() = properties.getProperty("embedSubs", "false").toBoolean()
        set(value) { properties.setProperty("embedSubs", value.toString()); save() }

    var sponsorBlock: Boolean
        get() = properties.getProperty("sponsorBlock", "false").toBoolean()
        set(value) { properties.setProperty("sponsorBlock", value.toString()); save() }

    var customArgs: String
        get() = properties.getProperty("customArgs", "")
        set(value) { properties.setProperty("customArgs", value); save() }

    var isSidebarExpanded: Boolean
        get() = properties.getProperty("isSidebarExpanded", "false").toBoolean()
        set(value) { properties.setProperty("isSidebarExpanded", value.toString()); save() }
}