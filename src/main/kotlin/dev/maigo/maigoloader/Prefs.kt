package dev.maigo.maigoloader

import java.util.prefs.Preferences

object Prefs {
    // Preferences guarda en el registro de Windows automáticamente
    // El string es un identificador para la app
    private val prefs = Preferences.userRoot().node("dev/maigo/maigoloader")

    var downloadFolder: String
        get() = prefs.get("downloadFolder", "")          // "" es el valor por defecto si no hay nada guardado
        set(value) = prefs.put("downloadFolder", value)
    var ytDlpFolder: String
        get() = prefs.get("ytDlpFolder", "")
        set(value) = prefs.put("ytDlpFolder", value)
    var ffmpegFolder: String
        get() = prefs.get("ffmpegFolder", "")
        set(value) = prefs.put("ffmpegFolder", value)
}