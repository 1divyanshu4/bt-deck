package dev.btdeck.core.model

/** Max simultaneous non-modifier keys in a boot keyboard report. */
const val MAX_KEYS = 6

/**
 * Builds the 8-byte keyboard report for everything currently held: modifiers OR'd into
 * byte 0, byte 1 reserved, then up to [MAX_KEYS] distinct key usages in press order.
 */
fun buildReport(held: Collection<Pair<Int, Int>>): ByteArray {
    var mods = 0
    val keys = LinkedHashSet<Int>()
    for ((m, k) in held) {
        mods = mods or m
        if (k != 0) keys += k
    }
    val report = ByteArray(8)
    report[0] = mods.toByte()
    keys.take(MAX_KEYS).forEachIndexed { i, k -> report[2 + i] = k.toByte() }
    return report
}
