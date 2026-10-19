package dev.mycet.ydg.utils

fun String.titleCase(): String =
    // los separa y después los une luego de transformarlos
    split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }

// Completa la ruta para un SVG en caso de mandarle solo el nombre del archivo
fun String.toSVG(): String =
    if (this.startsWith("icons/")) // icons/<name>.?
        if (this.endsWith(".svg")) this // icons/<name>.svg
        else "$this.svg" // icons/<name>
    else // <name>/?
        if (this.endsWith(".svg")) "icons/$this" // <name>.svg
        else "icons/$this.svg" // <name>

fun String.formatPathMiddle(maxLength: Int = 36): String {
    if (length <= maxLength) return this
    val sep = if (contains('\\')) '\\' else '/'
    val segments = split(sep)
    if (segments.size < 3) {
        val keep = (maxLength - 1) / 2
        return take(keep) + "…" + takeLast(keep)
    }

    var headCount = 1
    val tailCount = 1

    fun buildPrefix() = segments.take(headCount).joinToString(sep.toString()) + sep
    fun buildSuffix() = sep + segments.takeLast(tailCount).joinToString(sep.toString())

    while (headCount + tailCount < segments.size) {
        val nextPrefix = segments.take(headCount + 1).joinToString(sep.toString()) + sep
        if (nextPrefix.length + 1 + buildSuffix().length <= maxLength) {
            headCount++
        } else {
            break
        }
    }

    return buildPrefix() + "…" + buildSuffix()
}