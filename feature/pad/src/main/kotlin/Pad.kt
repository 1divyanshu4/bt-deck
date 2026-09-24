package dev.btdeck.feature.pad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.btdeck.core.bluetooth.HidKeyboard
import dev.btdeck.core.designsystem.component.ButtonIcon
import dev.btdeck.core.designsystem.component.CornerBadge
import dev.btdeck.core.designsystem.component.hasIcon
import dev.btdeck.core.designsystem.component.softCircles
import dev.btdeck.core.designsystem.theme.BTDeckShapes
import dev.btdeck.core.designsystem.theme.Palette
import dev.btdeck.core.model.BTDeckSettings
import dev.btdeck.core.model.ButtonStyle
import dev.btdeck.core.model.PadButton
import dev.btdeck.core.model.PadLayout
import dev.btdeck.core.model.PressMode
import dev.btdeck.core.model.comboLabel

private val KeyShape = BTDeckShapes.Medium

/**
 * The grid of shortcut keys for [layout], drawn according to [settings]. In [editing] mode
 * keys open the editor instead.
 */
@Composable
fun Pad(
    layout: PadLayout,
    editing: Boolean,
    heldIds: Set<String>,
    settings: BTDeckSettings,
    onEdit: (PadButton) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: (button: PadButton, toIndex: Int) -> Unit = { _, _ -> },
) {
    // Arranging in edit mode: the key being dragged by its handle, where the finger is now
    // (on screen), and where every key sits, to find the one under the finger.
    var dragged by remember { mutableStateOf<PadButton?>(null) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    val keyBounds = remember { mutableMapOf<String, Rect>() }
    fun indexUnder(point: Offset?): Int? =
        point?.let { p -> layout.buttons.indexOfFirst { keyBounds[it.id]?.contains(p) == true }.takeIf { it >= 0 } }
    val dropIndex = if (dragged != null) indexUnder(finger) else null

    // The grid ends in an Add tile while editing (or while there's nothing to press yet), and
    // is always filled out with empty slots to at least [MIN_ROWS] rows, so a layout with few
    // buttons still reads as a grid and its keys keep a normal size.
    val cells: List<Cell> = buildList {
        layout.buttons.forEach { add(Cell.Key(it)) }
        if (editing || layout.buttons.isEmpty()) add(Cell.Add)
        val rows = maxOf(MIN_ROWS, (size + layout.columns - 1) / layout.columns)
        while (size < rows * layout.columns) add(Cell.Empty)
    }
    val gap = settings.spacing.dp.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        for (row in cells.chunked(layout.columns)) {
            val rowModifier = Modifier.fillMaxWidth().weight(1f).heightIn(max = 180.dp)
            Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (slot in row) {
                    val cell = Modifier.weight(1f).fillMaxSize()
                    if (slot == Cell.Add) AddTile(onAdd, cell)
                    if (slot == Cell.Empty) EmptySlot(cell)
                    if (slot !is Cell.Key) continue
                    val button = slot.button
                    val isDragged = button.id == dragged?.id
                    PadKey(
                        button = button,
                        editing = editing,
                        held = button.id in heldIds,
                        settings = settings,
                        onEdit = { onEdit(button) },
                        dropTarget = dropIndex != null && !isDragged && layout.buttons.getOrNull(dropIndex) == button,
                        arrange = DragActions(
                            onStart = { dragged = button },
                            onDrag = { finger = it },
                            onEnd = {
                                // Read now: the last move and the lift can arrive in the same frame.
                                indexUnder(finger)?.let { onMove(button, it) }
                                dragged = null
                                finger = null
                            },
                        ),
                        modifier = cell
                            .onGloballyPositioned { keyBounds[button.id] = it.boundsInRoot() }
                            // The key stays put, faded, while the outline shows where it will go.
                            .alpha(if (isDragged) 0.4f else 1f),
                    )
                }
                // Keep a short last row aligned with the others.
                repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A spot in the grid: a key, the Add tile, or an empty slot that fills out the grid. */
private sealed interface Cell {
    data class Key(val button: PadButton) : Cell
    data object Add : Cell
    data object Empty : Cell
}

/** Rows the grid is always filled out to. */
private const val MIN_ROWS = 4

/** One key. Lit while pressed, held or toggled on; outlined if it's a Hold or Toggle key. */
@Composable
private fun PadKey(
    button: PadButton,
    editing: Boolean,
    held: Boolean,
    settings: BTDeckSettings,
    onEdit: () -> Unit,
    dropTarget: Boolean,
    arrange: DragActions,
    modifier: Modifier,
) {
    var touching by remember { mutableStateOf(false) }
    val color = Palette.getOrElse(button.color) { Palette[0] }
    val background = if (held || touching) color else color.copy(alpha = 0.62f)
    // Hold and Toggle keys get a bright outline; Tap keys a faint edge in their own colour.
    val outline = if (button.mode != PressMode.TAP) {
        BorderStroke(2.dp, lighter(color))
    } else {
        BorderStroke(1.dp, lighter(color).copy(alpha = 0.25f))
    }

    Box(
        modifier
            .clip(KeyShape)
            .background(background)
            .softCircles()
            .border(outline, KeyShape)
            .then(if (dropTarget) Modifier.border(2.dp, Color.White, KeyShape) else Modifier)
            .keyAccessibility(button, editing, held, onEdit)
            .keyTouch(button, editing, settings.vibrate, onEdit, onTouching = { touching = it })
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        KeyFace(button, settings, color)
        // Floats over the face; the edit pencil takes the corner while editing.
        if (!editing && settings.showShortcut) ShortcutBadge(button, settings)
        if (editing) {
            EditBadge()
            DragHandle(color, arrange)
        }
    }
}

/**
 * Screen readers and switch access can't press and hold, so their click taps a Tap key and
 * latches Hold/Toggle keys on and off.
 */
private fun Modifier.keyAccessibility(button: PadButton, editing: Boolean, held: Boolean, onEdit: () -> Unit) =
    semantics(mergeDescendants = true) {
        contentDescription = button.label.ifBlank { comboLabel(button.modifiers, button.key) }
        role = Role.Button
        if (!editing && button.mode != PressMode.TAP) stateDescription = if (held) "On" else "Off"
        onClick(label = if (editing) "Edit" else null) {
            if (editing) onEdit() else clickKey(button)
            true
        }
    }

/** Finger input: presses the key (or opens the editor in edit mode). */
@Composable
private fun Modifier.keyTouch(
    button: PadButton,
    editing: Boolean,
    vibrate: Boolean,
    onEdit: () -> Unit,
    onTouching: (Boolean) -> Unit,
): Modifier {
    val view = LocalView.current
    // The gesture loop outlives recompositions; always act on the latest values.
    val currentButton by rememberUpdatedState(button)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentVibrate by rememberUpdatedState(vibrate)
    return pointerInput(editing) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            onTouching(true)
            if (editing) {
                val lifted = waitForUpOrCancellation() != null
                if (lifted) currentOnEdit()
            } else {
                if (currentVibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                pressKey(currentButton)
            }
            onTouching(false)
        }
    }
}

/** What a finger does, by mode. Returns when the finger lifts. */
private suspend fun AwaitPointerEventScope.pressKey(button: PadButton) {
    when (button.mode) {
        PressMode.TAP -> HidKeyboard.tap(button.id, button.modifiers, button.key)
        PressMode.HOLD -> HidKeyboard.press(button.id, button.modifiers, button.key)
        PressMode.TOGGLE -> toggleKey(button)
    }
    waitForUpOrCancellation()
    if (button.mode == PressMode.HOLD) HidKeyboard.release(button.id)
}

/** What an accessibility click does, by mode. */
private fun clickKey(button: PadButton) {
    if (button.mode == PressMode.TAP) {
        HidKeyboard.tap(button.id, button.modifiers, button.key)
    } else {
        toggleKey(button)
    }
}

private fun toggleKey(button: PadButton) {
    if (HidKeyboard.isHeld(button.id)) {
        HidKeyboard.release(button.id)
    } else {
        HidKeyboard.press(button.id, button.modifiers, button.key)
    }
}

/**
 * What the key shows, following [settings]: its icon (with or without the label) or its
 * text. Keys without an icon always show text.
 */
@Composable
private fun KeyFace(button: PadButton, settings: BTDeckSettings, color: Color) {
    val showIcon = hasIcon(button.icon) && settings.buttonStyle != ButtonStyle.TEXT_ONLY
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // On short keys the label comes first, then the mode tag; the icon gets what's left.
        val labelHeight = 26.dp
        val tagHeight = 19.dp
        val faceHeight = if (showIcon) 18.dp else 24.dp
        val showLabel = showIcon &&
            settings.buttonStyle == ButtonStyle.ICON_AND_LABEL &&
            button.label.isNotBlank() &&
            maxHeight >= faceHeight + labelHeight
        val textHeight = if (showLabel) labelHeight else 0.dp
        val showModeTag = button.mode != PressMode.TAP && maxHeight >= faceHeight + textHeight + tagHeight
        val iconSize = minOf(maxWidth * 0.42f, (maxHeight - textHeight - (if (showModeTag) tagHeight else 0.dp)) * 0.8f)
            .coerceIn(18.dp, 64.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showIcon) IconFace(button, iconSize, showLabel) else TextFace(button)
            if (showModeTag) ModeTag(button.mode, color)
        }
    }
}

@Composable
private fun IconFace(button: PadButton, iconSize: Dp, showLabel: Boolean) {
    ButtonIcon(button.icon, button.tintImage, Color.White, Modifier.size(iconSize), description = null)
    if (showLabel) {
        Text(
            button.label,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun TextFace(button: PadButton) {
    Text(
        button.label.ifBlank { comboLabel(button.modifiers, button.key, separator = " + ") },
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The key's shortcut (e.g. Ctrl+Z) in its top-right corner. */
@Composable
private fun ShortcutBadge(button: PadButton, settings: BTDeckSettings) {
    // A text key without a label already shows its shortcut as its text.
    val showsShortcutAlready = button.label.isBlank() &&
        (!hasIcon(button.icon) || settings.buttonStyle == ButtonStyle.TEXT_ONLY)
    if (showsShortcutAlready) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        // Nudged into the key's padding so it hugs the corner and covers as little as it can.
        CornerBadge(comboLabel(button.modifiers, button.key, separator = " + "), Modifier.offset(x = 4.dp, y = (-4).dp))
    }
}

/** "HOLD" / "TOGGLE" under the key's text, in a light shade of the key's colour. */
@Composable
private fun ModeTag(mode: PressMode, color: Color) {
    Text(
        mode.label.uppercase(),
        color = lighter(color),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** A light shade of a key colour, for its outline and mode tag. */
private fun lighter(color: Color) = lerp(color, Color.White, 0.35f)

/** What dragging a key's handle does, in edit mode. [onDrag] gets the finger's spot on screen. */
private class DragActions(val onStart: () -> Unit, val onDrag: (Offset) -> Unit, val onEnd: () -> Unit)

/**
 * The 2×3 dots in an editing key's bottom-right corner. Drag them to move the key to another
 * spot; the Earlier/Later buttons in the editor do the same without dragging.
 */
@Composable
private fun DragHandle(color: Color, actions: DragActions) {
    val current by rememberUpdatedState(actions)
    val dotColor = lighter(color)
    // Where the handle sits, to turn touch positions into screen positions.
    val placement = remember { arrayOfNulls<LayoutCoordinates>(1) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(36.dp)
                .semantics { contentDescription = "Drag to move" }
                .onGloballyPositioned { placement[0] = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { current.onStart() },
                        onDragEnd = { current.onEnd() },
                        onDragCancel = { current.onEnd() },
                    ) { change, _ ->
                        change.consume()
                        placement[0]?.let { current.onDrag(it.localToRoot(change.position)) }
                    }
                }
                .drawBehind {
                    val gap = 8.dp.toPx()
                    val inset = 6.dp.toPx()
                    for (column in 0..1) {
                        for (row in 0..2) {
                            val center = Offset(size.width - inset - column * gap, size.height - inset - row * gap)
                            drawCircle(dotColor, radius = 2.dp.toPx(), center = center)
                        }
                    }
                },
        )
    }
}

@Composable
private fun EditBadge() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Icon(
            Icons.Default.Edit,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AddTile(onAdd: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(KeyShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), KeyShape)
            .clickable(onClick = onAdd)
            .semantics(mergeDescendants = true) { contentDescription = "Add button" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Text("Add", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** An unused spot in the grid: just a faint dashed outline. */
@Composable
private fun EmptySlot(modifier: Modifier) {
    Box(
        modifier.drawBehind {
            val radius = 12.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
            )
        },
    )
}
