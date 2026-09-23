package dev.keyset.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.keyset.core.data.CustomIcons
import dev.keyset.core.designsystem.icon.PadIcons

/** True if [key] names a built-in icon or a stored custom image. */
fun hasIcon(key: String?): Boolean =
    if (CustomIcons.isCustom(key)) CustomIcons.bitmap(key!!) != null else PadIcons.get(key) != null

/** Draws a built-in vector icon or a user image. Built-ins are always tinted to [color]. */
@Composable
fun ButtonIcon(
    key: String?,
    tintImage: Boolean,
    color: Color,
    modifier: Modifier,
    description: String? = PadIcons.get(key)?.name,
) {
    if (CustomIcons.isCustom(key)) {
        val bmp = remember(key) { CustomIcons.bitmap(key!!)?.asImageBitmap() } ?: return
        Image(
            bmp,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            colorFilter = if (tintImage) ColorFilter.tint(color) else null,
        )
    } else {
        val icon = PadIcons.get(key) ?: return
        Icon(icon.vector, contentDescription = description, tint = color, modifier = modifier)
    }
}
