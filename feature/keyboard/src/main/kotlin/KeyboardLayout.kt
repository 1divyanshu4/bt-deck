package dev.keyset.feature.keyboard

import dev.keyset.core.model.Keys
import dev.keyset.core.model.Mod

/**
 * One key on the on-screen keyboard.
 *
 * @property label what the key shows
 * @property shifted what it shows while Shift is on (letters and symbols)
 * @property usage the HID key it sends; 0 for keys that only change the keyboard
 * @property modifier the [Mod] bit for Ctrl / Shift / Alt / Meta, otherwise 0
 * @property width relative width; every row adds up to 15
 */
internal data class KeySpec(
    val label: String,
    val usage: Int = 0,
    val shifted: String? = null,
    val modifier: Int = 0,
    val width: Float = 1f,
    val description: String = label,
) {
    val isModifier get() = modifier != 0

    /** The Fn key, which switches between the two layers. */
    val isLayerSwitch get() = usage == 0 && modifier == 0
}

private fun key(
    label: String,
    name: String = label,
    shifted: String? = null,
    width: Float = 1f,
    description: String = name,
) = KeySpec(label, Keys.usage(name), shifted, width = width, description = description)

private fun letters(chars: String) = chars.map { KeySpec(it.toString(), Keys.usage(it.uppercase()), it.uppercase()) }

private fun modifierKey(label: String, bit: Int, width: Float) = KeySpec(label, modifier = bit, width = width)

/** The bottom row is the same on both layers. */
private fun bottomRow(fnLabel: String) = listOf(
    KeySpec(fnLabel, width = 1f, description = "Switch keys"),
    modifierKey("Ctrl", Mod.CTRL, 1.5f),
    modifierKey("Meta", Mod.META, 1.5f),
    modifierKey("Alt", Mod.ALT, 1.25f),
    key("", "Space", width = 5.75f, description = "Space"),
    key("←", "Left"),
    key("↑", "Up"),
    key("↓", "Down"),
    key("→", "Right"),
)

/** Letters, numbers and symbols, like a laptop keyboard. */
internal val MainLayer: List<List<KeySpec>> = listOf(
    listOf(
        key("`", shifted = "~"), key("1", shifted = "!"), key("2", shifted = "@"), key("3", shifted = "#"),
        key("4", shifted = "$"), key("5", shifted = "%"), key("6", shifted = "^"), key("7", shifted = "&"),
        key("8", shifted = "*"), key("9", shifted = "("), key("0", shifted = ")"), key("-", shifted = "_"),
        key("=", shifted = "+"), key("⌫", "Backspace", width = 2f),
    ),
    listOf(key("Tab", width = 1.5f)) + letters("qwertyuiop") +
        listOf(key("[", shifted = "{"), key("]", shifted = "}"), key("\\", shifted = "|", width = 1.5f)),
    listOf(key("Caps", "Caps Lock", width = 1.75f)) + letters("asdfghjkl") +
        listOf(key(";", shifted = ":"), key("'", shifted = "\""), key("Enter", width = 2.25f)),
    listOf(modifierKey("Shift", Mod.SHIFT, 2.25f)) + letters("zxcvbnm") +
        listOf(
            key(",", shifted = "<"),
            key(".", shifted = ">"),
            key("/", shifted = "?"),
            modifierKey("Shift", Mod.SHIFT, 2.75f),
        ),
    bottomRow(fnLabel = "Fn"),
)

/** Function keys and navigation keys, reached with Fn. */
internal val FnLayer: List<List<KeySpec>> = listOf(
    listOf(key("Esc", width = 1.5f)) + (1..12).map { key("F$it") } + listOf(key("Del", "Delete", width = 1.5f)),
    listOf(
        key("Insert", width = 3.75f),
        key("Home", width = 3.75f),
        key("Page Up", "PgUp", width = 3.75f),
        key("Pause", width = 3.75f),
    ),
    listOf(
        key("Print Screen", width = 3.75f),
        key("End", width = 3.75f),
        key("Page Down", "PgDn", width = 3.75f),
        key("Scroll Lock", width = 3.75f),
    ),
    listOf(
        key("Menu", width = 3.75f),
        key("Tab", width = 3.75f),
        key("Enter", width = 3.75f),
        key("⌫", "Backspace", width = 3.75f),
    ),
    bottomRow(fnLabel = "abc"),
)
