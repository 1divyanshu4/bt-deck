package dev.btdeck.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.KeyboardControlKey
import androidx.compose.material.icons.filled.KeyboardOptionKey
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.Pentagon
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.ui.graphics.vector.ImageVector
import dev.btdeck.core.designsystem.theme.Palette

data class PadIcon(val key: String, val name: String, val vector: ImageVector)

/**
 * Built-in icons a pad button can show instead of its text. Keys are persisted in layouts,
 * so never rename one. User images use keys starting with [CustomIcons.PREFIX] instead.
 */
object PadIcons {
    val groups: List<Pair<String, List<PadIcon>>> = listOf(
        "Edit" to listOf(
            PadIcon("undo", "Undo", Icons.AutoMirrored.Filled.Undo),
            PadIcon("redo", "Redo", Icons.AutoMirrored.Filled.Redo),
            PadIcon("history", "History", Icons.Filled.History),
            PadIcon("save", "Save", Icons.Filled.Save),
            PadIcon("save_as", "Save as", Icons.Filled.SaveAs),
            PadIcon("copy", "Copy", Icons.Filled.ContentCopy),
            PadIcon("cut", "Cut", Icons.Filled.ContentCut),
            PadIcon("paste", "Paste", Icons.Filled.ContentPaste),
            PadIcon("delete", "Delete", Icons.Filled.Delete),
            PadIcon("share", "Export", Icons.Filled.IosShare),
            PadIcon("screenshot", "Screenshot", Icons.Filled.Screenshot),
        ),
        "Selection" to listOf(
            PadIcon("lasso", "Lasso", DrawnIcons.Lasso),
            PadIcon("lasso_fill", "Lasso fill", DrawnIcons.LassoFill),
            PadIcon("lasso_erase", "Lasso erase", DrawnIcons.LassoErase),
            PadIcon("rect_select", "Rectangle select", DrawnIcons.RectSelect),
            PadIcon("ellipse_select", "Ellipse select", DrawnIcons.EllipseSelect),
            PadIcon("marquee", "Marquee", Icons.Filled.HighlightAlt),
            PadIcon("magic", "Magic wand", Icons.Filled.AutoFixHigh),
            PadIcon("select_all", "Select all", Icons.Filled.SelectAll),
            PadIcon("deselect", "Deselect", Icons.Filled.Deselect),
            PadIcon("invert_selection", "Invert selection", DrawnIcons.InvertSelection),
            PadIcon("transform", "Transform", Icons.Filled.OpenInFull),
            PadIcon("move", "Move", Icons.Filled.OpenWith),
            PadIcon("perspective", "Perspective", DrawnIcons.Perspective),
            PadIcon("crop", "Crop", Icons.Filled.Crop),
        ),
        "Brushes & tools" to listOf(
            PadIcon("brush", "Brush", Icons.Filled.Brush),
            PadIcon("pen", "Pen", Icons.Filled.Draw),
            PadIcon("pencil", "Pencil", Icons.Filled.ModeEdit),
            PadIcon("marker", "Marker", Icons.Filled.BorderColor),
            PadIcon("highlighter", "Highlighter", Icons.Filled.Highlight),
            PadIcon("airbrush", "Airbrush", DrawnIcons.Airbrush),
            PadIcon("eraser", "Eraser", DrawnIcons.Eraser),
            PadIcon("fill", "Fill bucket", Icons.Filled.FormatColorFill),
            PadIcon("gradient", "Gradient", Icons.Filled.Gradient),
            PadIcon("picker", "Eyedropper", Icons.Filled.Colorize),
            PadIcon("smudge", "Smudge", DrawnIcons.Smudge),
            PadIcon("blur", "Blur", Icons.Filled.BlurOn),
            PadIcon("liquify", "Liquify", Icons.Filled.Waves),
            PadIcon("clone", "Clone stamp", DrawnIcons.CloneStamp),
            PadIcon("texture", "Texture", Icons.Filled.Texture),
            PadIcon("grain", "Grain", Icons.Filled.Grain),
            PadIcon("text", "Text", Icons.Filled.TextFields),
        ),
        "Shapes & guides" to listOf(
            PadIcon("shapes", "Shapes", Icons.Filled.Interests),
            PadIcon("line", "Line", Icons.Filled.ShapeLine),
            PadIcon("curve", "Curve", Icons.Filled.Gesture),
            PadIcon("rectangle", "Rectangle", Icons.Filled.Rectangle),
            PadIcon("ellipse", "Ellipse", Icons.Filled.PanoramaFishEye),
            PadIcon("polygon", "Polygon", Icons.Filled.Pentagon),
            PadIcon("ruler", "Ruler", Icons.Filled.Straighten),
            PadIcon("symmetry", "Symmetry", DrawnIcons.Symmetry),
            PadIcon("grid", "Grid", Icons.Filled.GridOn),
        ),
        "Canvas" to listOf(
            PadIcon("pan", "Pan", Icons.Filled.PanTool),
            PadIcon("hand", "Hand", Icons.Filled.BackHand),
            PadIcon("zoom_in", "Zoom in", Icons.Filled.ZoomIn),
            PadIcon("zoom_out", "Zoom out", Icons.Filled.ZoomOut),
            PadIcon("fit", "Fit to screen", Icons.Filled.ZoomOutMap),
            PadIcon("reset_view", "Reset view", Icons.Filled.CenterFocusStrong),
            PadIcon("rotate_left", "Rotate left", Icons.AutoMirrored.Filled.RotateLeft),
            PadIcon("rotate_right", "Rotate right", Icons.AutoMirrored.Filled.RotateRight),
            PadIcon("rotate", "Rotate", Icons.Filled.ScreenRotation),
            PadIcon("flip", "Flip horizontal", Icons.Filled.Flip),
            PadIcon("flip_vertical", "Flip vertical", DrawnIcons.FlipVertical),
            PadIcon("resize", "Canvas size", Icons.Filled.PhotoSizeSelectLarge),
            PadIcon("aspect", "Aspect ratio", Icons.Filled.AspectRatio),
            PadIcon("reference", "Reference", Icons.Filled.Image),
            PadIcon("hide_ui", "Hide UI", Icons.Filled.VisibilityOff),
        ),
        "Layers" to listOf(
            PadIcon("layers", "Layers", Icons.Filled.Layers),
            PadIcon("new_layer", "New layer", Icons.Filled.LibraryAdd),
            PadIcon("duplicate_layer", "Duplicate layer", Icons.Filled.FilterNone),
            PadIcon("merge", "Merge down", Icons.AutoMirrored.Filled.MergeType),
            PadIcon("layer_up", "Layer up", DrawnIcons.LayerUp),
            PadIcon("layer_down", "Layer down", DrawnIcons.LayerDown),
            PadIcon("clear_layer", "Clear layer", Icons.Filled.LayersClear),
            PadIcon("visibility", "Visibility", Icons.Filled.Visibility),
            PadIcon("opacity", "Opacity", Icons.Filled.Opacity),
            PadIcon("alpha_lock", "Alpha lock", DrawnIcons.AlphaLock),
            PadIcon("clipping_mask", "Clipping mask", DrawnIcons.ClippingMask),
            PadIcon("lock", "Lock", Icons.Filled.Lock),
        ),
        "Color & adjust" to listOf(
            PadIcon("palette", "Palette", Icons.Filled.Palette),
            PadIcon("color_wheel", "Color wheel", Icons.Filled.ColorLens),
            PadIcon("swap_colors", "Swap colors", Icons.Filled.SwapHoriz),
            PadIcon("invert", "Invert", Icons.Filled.InvertColors),
            PadIcon("adjust", "Adjust", Icons.Filled.Tune),
            PadIcon("hsb", "Hue / saturation", Icons.Filled.Tonality),
            PadIcon("brightness", "Brightness", Icons.Filled.Brightness6),
            PadIcon("contrast", "Contrast", Icons.Filled.Contrast),
            PadIcon("curves", "Curves", Icons.AutoMirrored.Filled.ShowChart),
            PadIcon("filter", "Filter", Icons.Filled.Filter),
            PadIcon("grayscale", "Grayscale", Icons.Filled.FilterBAndW),
        ),
        "Brush size" to listOf(
            PadIcon("size_down", "Size down", Icons.Filled.RemoveCircle),
            PadIcon("size_up", "Size up", Icons.Filled.AddCircle),
            PadIcon("line_weight", "Line weight", Icons.Filled.LineWeight),
        ),
        "Animation" to listOf(
            PadIcon("animation", "Animation", Icons.Filled.Animation),
            PadIcon("timeline", "Timeline", Icons.Filled.Movie),
            PadIcon("play", "Play", Icons.Filled.PlayArrow),
        ),
        "Keys & misc" to listOf(
            PadIcon("ctrl", "Ctrl", Icons.Filled.KeyboardControlKey),
            PadIcon("shift", "Shift", Icons.Filled.KeyboardCapslock),
            PadIcon("alt", "Alt", Icons.Filled.KeyboardOptionKey),
            PadIcon("meta", "Meta", Icons.Filled.KeyboardCommandKey),
            PadIcon("space", "Space", Icons.Filled.SpaceBar),
            PadIcon("backspace", "Backspace", Icons.AutoMirrored.Filled.Backspace),
            PadIcon("menu", "Menu", Icons.Filled.Menu),
            PadIcon("quick_menu", "Quick menu", Icons.Filled.Apps),
            PadIcon("settings", "Settings", Icons.Filled.Settings),
            PadIcon("star", "Star", Icons.Filled.Star),
            PadIcon("bolt", "Bolt", Icons.Filled.Bolt),
            PadIcon("dot", "Dot", Icons.Filled.Circle),
        ),
    )

    private val byKey = groups.flatMap { it.second }.associateBy { it.key }

    fun get(key: String?): PadIcon? = key?.let { byKey[it] }
}
