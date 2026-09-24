package dev.btdeck.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * The quiet pattern behind pad keys and layout cards: two soft circles in opposite corners.
 * Draw it on a clipped surface, after its background.
 */
fun Modifier.softCircles() = drawBehind {
    val glow = Color.White.copy(alpha = 0.05f)
    drawCircle(glow, radius = size.minDimension * 0.55f, center = Offset(0f, 0f))
    drawCircle(glow, radius = size.minDimension * 0.4f, center = Offset(size.width, size.height))
}
