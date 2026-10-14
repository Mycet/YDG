package dev.mycet.ydg.ui.navigation

enum class AppTab(val iconPath: String, val displayName: String) {
    MEDIA("icons/tab_play.svg", "Media"),
    SETUP("icons/tab_settings.svg", "Settings"),
    ;

    companion object {
        fun match(name: String): AppTab = when (name.uppercase()) {
            "SETUP" -> SETUP
            else -> MEDIA
        }
    }
}