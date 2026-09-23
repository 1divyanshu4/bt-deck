package dev.keyset.core.model

/** Modifier bits in byte 0 of a HID boot keyboard report. */
object Mod {
    const val CTRL = 0x01
    const val SHIFT = 0x02
    const val ALT = 0x04
    const val META = 0x08

    val all = listOf(CTRL to "Ctrl", SHIFT to "Shift", ALT to "Alt", META to "Meta")
}

data class Key(val usage: Int, val name: String)

/** HID usage IDs (Keyboard/Keypad page 0x07) the pad can send. Usage 0 means "modifiers only". */
object Keys {
    val NONE = Key(0, "None")

    val all: List<Key> = buildList {
        add(NONE)
        ('A'..'Z').forEachIndexed { i, c -> add(Key(0x04 + i, c.toString())) }
        ('1'..'9').forEachIndexed { i, c -> add(Key(0x1E + i, c.toString())) }
        add(Key(0x27, "0"))
        add(Key(0x2C, "Space"))
        add(Key(0x28, "Enter"))
        add(Key(0x29, "Esc"))
        add(Key(0x2B, "Tab"))
        add(Key(0x2A, "Backspace"))
        add(Key(0x4C, "Delete"))
        add(Key(0x2D, "-"))
        add(Key(0x2E, "="))
        add(Key(0x2F, "["))
        add(Key(0x30, "]"))
        add(Key(0x31, "\\"))
        add(Key(0x33, ";"))
        add(Key(0x34, "'"))
        add(Key(0x35, "`"))
        add(Key(0x36, ","))
        add(Key(0x37, "."))
        add(Key(0x38, "/"))
        add(Key(0x52, "Up"))
        add(Key(0x51, "Down"))
        add(Key(0x50, "Left"))
        add(Key(0x4F, "Right"))
        add(Key(0x4A, "Home"))
        add(Key(0x4D, "End"))
        add(Key(0x4B, "PgUp"))
        add(Key(0x4E, "PgDn"))
        add(Key(0x49, "Insert"))
        (1..12).forEach { add(Key(0x39 + it, "F$it")) }
    }

    private val byUsage = all.associateBy { it.usage }

    fun name(usage: Int): String = byUsage[usage]?.name ?: "0x%02X".format(usage)
}

/** Human-readable combo, e.g. "Ctrl+Shift+Z". */
fun comboLabel(modifiers: Int, key: Int): String {
    val parts = Mod.all.filter { modifiers and it.first != 0 }.map { it.second }.toMutableList()
    if (key != 0) parts += Keys.name(key)
    return parts.joinToString("+").ifEmpty { "—" }
}
