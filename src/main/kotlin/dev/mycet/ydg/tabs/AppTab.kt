package dev.mycet.ydg.tabs

enum class AppTab(val iconPath: String) {
    VIDEO("icons/tab_play.svg"),
    AUDIO("icons/tab_audio.svg"),
    SETUP("icons/tab_settings.svg"),
    ;

    companion object {
        // runCatching es como usar un try catch pero de forma compacta
        fun match(name: String) = runCatching { valueOf(name) }.getOrDefault(VIDEO)
    }
}