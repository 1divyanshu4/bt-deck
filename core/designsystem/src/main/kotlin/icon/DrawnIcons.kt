package dev.keyset.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Drawing-app icons that Material doesn't have. 24dp grid; "S" parts are 2dp round
 * strokes, "F" parts are fills — same style as the Material filled set.
 */
object DrawnIcons {
    private fun icon(name: String, vararg parts: Pair<Char, String>): ImageVector {
        val b = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        for ((mode, d) in parts) {
            when (mode) {
                'S' -> b.addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
                'E' -> b.addPath(
                    pathData = addPathNodes(d),
                    fill = SolidColor(Color.Black),
                    pathFillType = PathFillType.EvenOdd,
                )
                else -> b.addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
            }
        }
        return b.build()
    }

    val Lasso = icon(
        "Lasso",
        'S' to
            "M7.2,13.3C4.6,12.4 3,10.8 3,9C3,6.2 7,4 12,4C17,4 21,6.2 21,9C21,11.8 17,14 12,14C10.9,14 9.9,13.9 9,13.7",
        'S' to "M8,12.5C6.5,14.5 8.5,16 7.5,18.5",
        'S' to "M7.5,21.3a1.8,1.8 0 1,0 0.01,0",
    )

    val LassoFill = icon(
        "LassoFill",
        'F' to "M12,4C17,4 21,6.2 21,9C21,11.8 17,14 12,14C7,14 3,11.8 3,9C3,6.2 7,4 12,4Z",
        'S' to "M8,12.5C6.5,14.5 8.5,16 7.5,18.5",
        'S' to "M7.5,21.3a1.8,1.8 0 1,0 0.01,0",
    )

    val LassoErase = icon(
        "LassoErase",
        'S' to
            "M7.2,13.3C4.6,12.4 3,10.8 3,9C3,6.2 7,4 12,4C17,4 21,6.2 21,9C21,11.8 17,14 12,14C10.9,14 9.9,13.9 9,13.7",
        'S' to "M8,12.5C6.5,14.5 8.5,16 7.5,18.5",
        'S' to "M14,19H22",
    )

    val EllipseSelect = icon(
        "EllipseSelect",
        'S' to
            "M21.00,12.00A9,7 0 0,1 20.47,14.37M19.28,16.11A9,7 0 0,1 17.06,17.79M14.78,18.66A9,7 0 0,1 11.72,19.00M9.22,18.66A9,7 0 0,1 6.48,17.53M4.72,16.11A9,7 0 0,1 3.36,13.95M3.00,12.00A9,7 0 0,1 3.53,9.63M4.72,7.89A9,7 0 0,1 6.94,6.21M9.22,5.34A9,7 0 0,1 12.28,5.00M14.78,5.34A9,7 0 0,1 17.52,6.47M19.28,7.89A9,7 0 0,1 20.64,10.05",
    )

    val RectSelect = icon(
        "RectSelect",
        'S' to
            "M3.00,5.00L6.00,5.00M8.00,5.00L11.00,5.00M13.00,5.00L16.00,5.00M18.00,5.00L21.00,5.00M21.00,5.00L21.00,8.00M21.00,10.00L21.00,13.00M21.00,15.00L21.00,18.00M21.00,19.00L18.00,19.00M16.00,19.00L13.00,19.00M11.00,19.00L8.00,19.00M6.00,19.00L3.00,19.00M3.00,19.00L3.00,16.00M3.00,14.00L3.00,11.00M3.00,9.00L3.00,6.00",
    )

    val Symmetry = icon(
        "Symmetry",
        'S' to "M12,2V5M12,8V11M12,14V17M12,20V22",
        'S' to "M9,6L3,18H9Z",
        'F' to "M15,6L21,18H15Z",
    )

    val FlipVertical = icon(
        "FlipVertical",
        'S' to "M2,12H5M8,12H11M14,12H17M20,12H22",
        'S' to "M6,9L18,9L12,3Z",
        'F' to "M6,15L18,15L12,21Z",
    )

    val CloneStamp = icon(
        "CloneStamp",
        'S' to "M12,3a3,3 0 1,0 0.01,0",
        'S' to "M10.5,8.8V13M13.5,8.8V13",
        'S' to "M5,13H19L20,17H4Z",
        'S' to "M5,20.5H19",
    )

    val ClippingMask = icon(
        "ClippingMask",
        'S' to "M11,3H19A2,2 0 0,1 21,5V8A2,2 0 0,1 19,10H11A2,2 0 0,1 9,8V5A2,2 0 0,1 11,3Z",
        'F' to "M11,14H19A2,2 0 0,1 21,16V19A2,2 0 0,1 19,21H11A2,2 0 0,1 9,19V16A2,2 0 0,1 11,14Z",
        'S' to "M4,6.5V17.5H6.5M4.5,15L6.5,17.5L4.5,20",
    )

    val Perspective = icon(
        "Perspective",
        'S' to "M3,20L8,4H16L21,20Z",
        'S' to "M12,4V20",
        'S' to "M5.5,12H18.5",
    )

    val Airbrush = icon(
        "Airbrush",
        'S' to "M13,9H19A1,1 0 0,1 20,10V20A1,1 0 0,1 19,21H13A1,1 0 0,1 12,20V10A1,1 0 0,1 13,9Z",
        'S' to "M14.5,5.5H17.5V9H14.5Z",
        'F' to
            "M9,5a1,1 0 1,0 0.01,0M5,4a1,1 0 1,0 0.01,0M7,8a1,1 0 1,0 0.01,0M3,8a1,1 0 1,0 0.01,0M5,11.5a1,1 0 1,0 0.01,0M9,11a1,1 0 1,0 0.01,0",
    )

    val Smudge = icon(
        "Smudge",
        'F' to "M16.5,4a4,4 0 1,1 -0.01,0",
        'S' to "M3,12C7,11 10,9.5 13,8.5M4,16.5C8,15 11,13 14,11M7,21C10,19 13,16 15.5,12.5",
    )

    val AlphaLock = icon(
        "AlphaLock",
        'S' to "M6,11H18A1,1 0 0,1 19,12V20A1,1 0 0,1 18,21H6A1,1 0 0,1 5,20V12A1,1 0 0,1 6,11Z",
        'S' to "M8,11V7A4,4 0 0,1 16,7V11",
        'F' to "M9,14h3v3h-3zM12,17h3v3h-3z",
    )

    val LayerUp = icon(
        "LayerUp",
        'S' to "M9,9L16,13L9,17L2,13Z",
        'S' to "M2,17.5L9,21.5L16,17.5",
        'S' to "M20,11V2M17,5L20,2L23,5",
    )

    val LayerDown = icon(
        "LayerDown",
        'S' to "M9,9L16,13L9,17L2,13Z",
        'S' to "M2,17.5L9,21.5L16,17.5",
        'S' to "M20,2V11M17,8L20,11L23,8",
    )

    val InvertSelection = icon(
        "InvertSelection",
        'S' to
            "M3.00,3.00L5.50,3.00M7.50,3.00L10.00,3.00M12.00,3.00L14.50,3.00M16.50,3.00L19.00,3.00M21.00,3.00L21.00,5.50M21.00,7.50L21.00,10.00M21.00,12.00L21.00,14.50M21.00,16.50L21.00,19.00M21.00,21.00L18.50,21.00M16.50,21.00L14.00,21.00M12.00,21.00L9.50,21.00M7.50,21.00L5.00,21.00M3.00,21.00L3.00,18.50M3.00,16.50L3.00,14.00M3.00,12.00L3.00,9.50M3.00,7.50L3.00,5.00",
        'F' to "M8,8H16V16H8Z",
    )

    val Eraser = icon(
        "Eraser",
        'E' to "M16.24,3.56L21.19,8.5C21.97,9.29 21.97,10.55 21.19,11.34L12,20.53C10.44,22.09 7.91,22.09 " +
            "6.34,20.53L2.81,17C2.03,16.21 2.03,14.95 2.81,14.16L13.41,3.56C14.2,2.78 15.46,2.78 16.24,3.56Z" +
            "M4.22,15.58L7.76,19.11C8.54,19.9 9.8,19.9 10.59,19.11L14.12,15.58L9.17,10.63L4.22,15.58Z",
    )
}
