package dev.mycet.ydg.utils

import java.util.*

enum class ByteUnit(val bytes: Double) {
    B(1.0),
    KB(1_000.0),
    MB(1_000_000.0),
    GB(1_000_000_000.0),
    KIB(1_024.0),
    MIB(1_048_576.0),
    GIB(1_073_741_824.0);

    companion object {
        fun parse(raw: String): ByteUnit {
            val clean = raw.trim().lowercase().removeSuffix("/s")
            return entries.firstOrNull { it.name.equals(clean, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown unit: '$raw'")
        }
    }
}

private fun Number.convert(from: String, to: ByteUnit): Double =
    toDouble() * ByteUnit.parse(from).bytes / to.bytes

fun Number.toGB(unit: String = "B"): Double = convert(unit, ByteUnit.GIB)
fun Number.toMB(unit: String = "B"): Double = convert(unit, ByteUnit.MIB)
fun Number.toKB(unit: String = "B"): Double = convert(unit, ByteUnit.KIB)

fun Number.formatBytes(): String = when {
    this.toDouble() >= ByteUnit.GIB.bytes -> String.format(Locale.US, "%.1f GB", this.toGB())
    this.toDouble() >= ByteUnit.MIB.bytes -> String.format(Locale.US, "%.1f MB", this.toMB())
    this.toDouble() >= ByteUnit.KIB.bytes -> String.format(Locale.US, "%.1f KB", this.toKB())
    else -> String.format(Locale.US, "%.1f B", this.toDouble())
}

fun formatProgress(downloadedMB: Double, totalMB: Double): String {
    return if (totalMB >= 1024.0) {
        val downloadedGB = downloadedMB / 1024.0
        val totalGB = totalMB / 1024.0
        String.format(Locale.US, "%.2f / %.2f GB", downloadedGB, totalGB)
    } else {
        String.format(Locale.US, "%.1f / %.1f MB", downloadedMB, totalMB)
    }
}
