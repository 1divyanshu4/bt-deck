package dev.keyset.feature.pad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
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
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.designsystem.component.ButtonIcon
import dev.keyset.core.designsystem.component.hasIcon
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.designsystem.theme.Palette
import dev.keyset.core.model.ButtonStyle
import dev.keyset.core.model.KeySetSettings
import dev.keyset.core.model.PadButton
import dev.keyset.core.model.PadLayout
import dev.keyset.core.model.PressMode
import dev.keyset.core.model.comboLabel

private val KeyShape = KeySetShapes.Medium

/**
 * The grid of shortcut keys for [layout], drawn according to [settings]. In [editing] mode
 * keys open the editor instead.
 */
@Composable
fun Pad(
    layout: PadLayout,
    editing: Boolean,
    heldIds: Set<String>,
    settings: KeySetSettings,
    onEdit: (PadButton) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // In edit mode the last cell is an "add button" tile, shown as null.
    val cells: List<PadButton?> = if (editing) layout.buttons + null else layout.buttons
    if (cells.isEmpty()) {
        EmptyPad(modifier)
        return
    }
    val gap = settings.spacing.dp.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        for (row in cells.chunked(layout.columns)) {
            val rowModifier = Modifier.fillMaxWidth().weight(1f).heightIn(max = 180.dp)
            Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (button in row) {
                    val cell = Modifier.weight(1f).fillMaxSize()
                    if (button == null) {
                        AddTile(onAdd, cell)
                    } else {
                        PadKey(button, editing, button.id in heldIds, settings, onEdit = { onEdit(button) }, cell)
                    }
                }
                // Keep a short last row aligned with the others.
                repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun EmptyPad(modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("No buttons yet.\nTap ✎ to add some.", textAlign = TextAlign.Center, color = Color.Gray)
    }
}

/** One key. Lit while pressed, held or toggled on; outlined if it's a Hold or Toggle key. */
@Composable
private fun PadKey(
    button: PadButton,
    editing: Boolean,
    held: Boolean,
    settings: KeySetSettings,
    onEdit: () -> Unit,
    modifier: Modifier,
) {
    var touching by remember { mutableStateOf(false) }
    val color = Palette.getOrElse(button.color) { Palette[0] }
    val background = if (held || touching) color else color.copy(alpha = 0.28f)
    val outline = if (button.mode != PressMode.TAP) color else Color.Transparent

    Box(
        modifier
            .clip(KeyShape)
            .background(background)
            .border(2.dp, outline, KeyShape)
            .keyAccessibility(button, editing, held, onEdit)
            .keyTouch(button, editing, settings.vibrate, onEdit, onTouching = { touching = it })
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        KeyFace(button, settings)
        if (editing) EditBadge()
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
private fun KeyFace(button: PadButton, settings: KeySetSettings) {
    val showIcon = hasIcon(button.icon) && settings.buttonStyle != ButtonStyle.TEXT_ONLY
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val iconSize = (minOf(maxWidth, maxHeight) * 0.42f).coerceIn(22.dp, 64.dp)
        val roomForLabel = maxHeight > 72.dp
        val roomForModeTag = maxHeight > 56.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showIcon) {
                val showLabel = settings.buttonStyle == ButtonStyle.ICON_AND_LABEL && roomForLabel
                IconFace(button, iconSize, showLabel)
            } else {
                TextFace(button, settings.showShortcut)
            }
            if (button.mode != PressMode.TAP && roomForModeTag) ModeTag(button.mode)
        }
    }
}

@Composable
private fun IconFace(button: PadButton, iconSize: Dp, showLabel: Boolean) {
    ButtonIcon(button.icon, button.tintImage, Color.White, Modifier.size(iconSize), description = null)
    if (showLabel && button.label.isNotBlank()) {
        SmallText(button.label, alpha = 0.75f, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun TextFace(button: PadButton, showShortcut: Boolean) {
    val shortcut = comboLabel(button.modifiers, button.key)
    Text(
        button.label.ifBlank { shortcut },
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    if (showShortcut && button.label.isNotBlank()) SmallText(shortcut, alpha = 0.6f)
}

/** "HOLD" / "TOGGLE" under the key's text. */
@Composable
private fun ModeTag(mode: PressMode) {
    Text(mode.label.uppercase(), color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp)
}

@Composable
private fun SmallText(text: String, alpha: Float, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White.copy(alpha = alpha),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
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
    Box(
        modifier
            .clip(KeyShape)
            .border(2.dp, Color.White.copy(alpha = 0.2f), KeyShape)
            .clickable(onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add button", tint = Color.White.copy(alpha = 0.6f))
    }
}
