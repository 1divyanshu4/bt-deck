package dev.keyset.core.model

import java.util.UUID

enum class PressMode(val label: String, val hint: String) {
    TAP("Tap", "Sends the shortcut once"),
    HOLD("Hold", "Key stays down while your finger is on the button — e.g. Space to pan"),
    TOGGLE("Toggle", "Tap to lock the key down, tap again to release"),
}

data class PadButton(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val modifiers: Int,
    val key: Int,
    val mode: PressMode = PressMode.TAP,
    val color: Int = 0,
    /** A built-in icon key, or a custom image key ("img:…"); null shows the label/shortcut text. */
    val icon: String? = null,
    /** Custom images only: recolor to white like the built-in icons, instead of original colors. */
    val tintImage: Boolean = false,
)

data class PadLayout(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val columns: Int = 3,
    val buttons: List<PadButton>,
)

data class PadState(val layouts: List<PadLayout>, val selectedId: String) {
    val selected: PadLayout get() = layouts.firstOrNull { it.id == selectedId } ?: layouts.first()
}
