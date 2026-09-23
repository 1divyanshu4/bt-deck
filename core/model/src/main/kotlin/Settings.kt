package dev.keyset.core.model

/** What a pad button shows. */
enum class ButtonStyle(val label: String) {
    /** Icon with its label under it; buttons without an icon show their text. */
    ICON_AND_LABEL("Icon and label"),

    /** Just the icon; buttons without an icon still show their text. */
    ICON_ONLY("Icon only"),

    /** Text only, even if an icon is set. */
    TEXT_ONLY("Text only"),
}

/** Gap between pad buttons. */
enum class ButtonSpacing(val label: String, val dp: Int) {
    COMPACT("Compact", 4),
    NORMAL("Normal", 10),
    ROOMY("Roomy", 16),
}

/** User preferences from the Settings screen. */
data class KeySetSettings(
    val buttonStyle: ButtonStyle = ButtonStyle.ICON_AND_LABEL,
    /** Show the key combination (e.g. "Ctrl+Z") on text buttons. */
    val showShortcut: Boolean = true,
    val vibrate: Boolean = true,
    val keepScreenOn: Boolean = true,
    val spacing: ButtonSpacing = ButtonSpacing.NORMAL,
)
