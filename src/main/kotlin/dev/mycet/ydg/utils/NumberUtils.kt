package dev.mycet.ydg.utils

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

fun Number.toMB(unit: String = "B"): Double = convert(unit, ByteUnit.MIB)
fun Number.toGB(unit: String = "B"): Double = convert(unit, ByteUnit.GIB)
fun Number.toKB(unit: String = "B"): Double = convert(unit, ByteUnit.KIB)