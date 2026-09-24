package dev.btdeck.core.model

/**
 * How to type [c] on a US keyboard layout: the modifiers and key to press, or null if the
 * character has no key (e.g. emoji or accented letters).
 *
 * The host decides what a key means, so this is only correct when the host is set to a US
 * (QWERTY) layout.
 */
fun keystrokeFor(c: Char): Pair<Int, Int>? {
    when (c) {
        in 'a'..'z' -> return 0 to 0x04 + (c - 'a')
        in 'A'..'Z' -> return Mod.SHIFT to 0x04 + (c - 'A')
        in '1'..'9' -> return 0 to 0x1E + (c - '1')
        '0' -> return 0 to 0x27
    }
    UNSHIFTED[c]?.let { return 0 to it }
    SHIFTED[c]?.let { return Mod.SHIFT to it }
    return null
}

private val UNSHIFTED = mapOf(
    '\n' to 0x28, '\t' to 0x2B, ' ' to 0x2C, '-' to 0x2D, '=' to 0x2E, '[' to 0x2F, ']' to 0x30,
    '\\' to 0x31, ';' to 0x33, '\'' to 0x34, '`' to 0x35, ',' to 0x36, '.' to 0x37, '/' to 0x38,
)

private val SHIFTED = mapOf(
    '!' to 0x1E, '@' to 0x1F, '#' to 0x20, '$' to 0x21, '%' to 0x22, '^' to 0x23, '&' to 0x24,
    '*' to 0x25, '(' to 0x26, ')' to 0x27, '_' to 0x2D, '+' to 0x2E, '{' to 0x2F, '}' to 0x30,
    '|' to 0x31, ':' to 0x33, '"' to 0x34, '~' to 0x35, '<' to 0x36, '>' to 0x37, '?' to 0x38,
)
