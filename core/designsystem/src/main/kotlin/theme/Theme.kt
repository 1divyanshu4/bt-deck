package dev.keyset.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Fixed dark palette: the pad sits next to a drawing tablet, often in a dim room. */
object KeySetColors {
    val Background = Color(0xFF121214)
    val Surface = Color(0xFF1C1C20)
    val SurfaceHigh = Color(0xFF26262C)
    val Primary = Color(0xFFB9A6FF)
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
fun KeySetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = KeySetColors.Primary,
            background = KeySetColors.Background,
            surface = KeySetColors.Surface,
            surfaceContainer = KeySetColors.Surface,
            surfaceContainerHigh = KeySetColors.SurfaceHigh,
        ),
        content = content,
    )
}
