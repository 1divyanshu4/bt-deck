package dev.keyset.feature.keyboard

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.keyset.core.model.Keys
import dev.keyset.core.model.keystrokeFor

/**
 * What an on-screen key does to the text box while it's being written in. [text] is what the
 * key shows right now (so Shift and Caps Lock are already applied). Keys that make no sense
 * in a text box, like F5, leave it as it is.
 */
internal fun TextFieldValue.edit(usage: Int, text: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val hasSelection = start != end

    fun replace(from: Int, to: Int, with: String) =
        TextFieldValue(this.text.replaceRange(from, to, with), TextRange(from + with.length))

    fun moveTo(index: Int) = copy(selection = TextRange(index.coerceIn(0, this.text.length)))

    return when (Keys.name(usage)) {
        "Backspace" -> when {
            hasSelection -> replace(start, end, "")
            start > 0 -> replace(start - 1, start, "")
            else -> this
        }
        "Delete" -> when {
            hasSelection -> replace(start, end, "")
            end < this.text.length -> replace(start, start + 1, "")
            else -> this
        }
        "Left" -> moveTo(if (hasSelection) start else start - 1)
        "Right" -> moveTo(if (hasSelection) end else end + 1)
        "Home", "Up" -> moveTo(0)
        "End", "Down" -> moveTo(this.text.length)
        "Space" -> replace(start, end, " ")
        // Letters, digits and symbols; the arrow labels (←…) have no keystroke, so they don't count.
        else -> if (text.length == 1 && keystrokeFor(text[0]) != null) replace(start, end, text) else this
    }
}
