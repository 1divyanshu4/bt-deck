package dev.btdeck.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Fixed dark palette: the pad sits next to a drawing tablet, often in a dim room. */
object ZexDeckColors {
    val Background = Color(0xFF121214)
    val Surface = Color(0xFF1C1C20)
    val SurfaceHigh = Color(0xFF26262C)
    val Primary = Color(0xFFB9A6FF)
}

/**
 * Corner radii. Small parts (buttons, chips, tiles, keyboard keys) get [Small], larger
 * surfaces (pad keys, cards, dialogs, sheets) get [Medium], and badges that sit inside a
 * key or card get [XSmall], so they're squarer than what they sit on.
 */
object ZexDeckShapes {
    val XSmall = RoundedCornerShape(6.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
}

/** Button accent colors, stored by index in PadButton.color. */
val Palette = listOf(
    Color(0xFF3A3A42), // neutral
    Color(0xFF7C6CF0), // violet
    Color(0xFF2FA88E), // teal
    Color(0xFFD9962B), // amber
    Color(0xFFE0674F), // coral
    Color(0xFF3D8FE0), // blue
)

@Composable
fun ZexDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ZexDeckColors.Primary,
            background = ZexDeckColors.Background,
            surface = ZexDeckColors.Surface,
            surfaceContainer = ZexDeckColors.Surface,
            surfaceContainerHigh = ZexDeckColors.SurfaceHigh,
        ),
        // Material components pick from these; nothing is rounder than Medium.
        shapes = Shapes(
            extraSmall = ZexDeckShapes.Small,
            small = ZexDeckShapes.Small,
            medium = ZexDeckShapes.Medium,
            large = ZexDeckShapes.Medium,
            extraLarge = ZexDeckShapes.Medium,
        ),
        content = content,
    )
}
