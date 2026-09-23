package dev.keyset.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.keyset.core.designsystem.component.ButtonIcon
import dev.keyset.core.designsystem.component.HelpButton
import dev.keyset.core.designsystem.component.hasIcon
import dev.keyset.core.designsystem.theme.KeySetColors
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.designsystem.theme.Palette
import dev.keyset.core.model.Keys
import dev.keyset.core.model.Mod
import dev.keyset.core.model.PadButton
import dev.keyset.core.model.PressMode
import dev.keyset.core.model.comboLabel

private const val ICON_HELP =
    "Shown on the button instead of the shortcut. None shows the label and shortcut. " +
        "Tap + under Custom to use your own image (PNG with transparency works best)."

/**
 * Dialog for creating or editing one pad button. Changes are kept in a draft and only
 * handed to [onSave] when the user taps Save.
 */
@Composable
fun ButtonEditor(
    initial: PadButton,
    isNew: Boolean,
    onSave: (PadButton) -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = KeySetShapes.Medium,
            color = KeySetColors.Surface,
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 720.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                EditorHeader(draft, isNew)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    LabelField(draft.label) { draft = draft.copy(label = it) }
                    ModifierChips(draft.modifiers) { draft = draft.copy(modifiers = it) }
                    KeyChips(draft.key) { draft = draft.copy(key = it) }
                    BehaviourPicker(draft.mode) { draft = draft.copy(mode = it) }
                    SectionTitle("Icon", help = ICON_HELP)
                    IconPicker(
                        selected = draft.icon,
                        tint = Palette.getOrElse(draft.color) { Palette[0] },
                        tintImage = draft.tintImage,
                        onTintImage = { draft = draft.copy(tintImage = it) },
                        onPick = { draft = draft.copy(icon = it) },
                    )
                    ColorPicker(draft.color) { draft = draft.copy(color = it) }
                    if (!isNew) PositionButtons(onMove)
                }
                EditorFooter(
                    isNew = isNew,
                    canSave = draft.modifiers != 0 || draft.key != 0,
                    onDelete = onDelete,
                    onCancel = onDismiss,
                    onSave = { onSave(draft.copy(label = draft.label.trim())) },
                )
            }
        }
    }
}

/** Title plus a live preview of the icon and shortcut being built. */
@Composable
private fun EditorHeader(draft: PadButton, isNew: Boolean) {
    Text(if (isNew) "New button" else "Edit button", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.size(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (hasIcon(draft.icon)) {
            val iconModifier = Modifier.padding(end = 10.dp).size(28.dp)
            ButtonIcon(draft.icon, draft.tintImage, MaterialTheme.colorScheme.primary, iconModifier)
        }
        Text(
            comboLabel(draft.modifiers, draft.key),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LabelField(label: String, onChange: (String) -> Unit) {
    Spacer(Modifier.size(12.dp))
    OutlinedTextField(
        value = label,
        onValueChange = onChange,
        label = { Text("Label (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Ctrl / Shift / Alt / Meta, any combination. [modifiers] is a bit mask of [Mod] values. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModifierChips(modifiers: Int, onChange: (Int) -> Unit) {
    SectionTitle("Modifiers")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((bit, name) in Mod.all) {
            FilterChip(
                selected = modifiers and bit != 0,
                onClick = { onChange(modifiers xor bit) },
                label = { Text(name) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyChips(key: Int, onChange: (Int) -> Unit) {
    SectionTitle("Key")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (k in Keys.all) {
            FilterChip(
                selected = key == k.usage,
                onClick = { onChange(k.usage) },
                label = { Text(k.name) },
            )
        }
    }
}

@Composable
private fun BehaviourPicker(mode: PressMode, onChange: (PressMode) -> Unit) {
    val help = PressMode.entries.joinToString("\n") { "${it.label}: ${it.hint}" }
    SectionTitle("Behaviour", help = help)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        PressMode.entries.forEachIndexed { i, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = { onChange(m) },
                shape = SegmentedButtonDefaults.itemShape(i, PressMode.entries.size, KeySetShapes.Small),
                icon = {}, // no checkmark; the filled segment already shows the choice
            ) { Text(m.label) }
        }
    }
}

/** One swatch per [Palette] entry; [color] is the selected index. */
@Composable
private fun ColorPicker(color: Int, onChange: (Int) -> Unit) {
    SectionTitle("Color")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Palette.forEachIndexed { i, swatch ->
            val ring = if (color == i) Color.White else Color.Transparent
            Box(
                Modifier
                    .size(36.dp)
                    .clip(KeySetShapes.Small)
                    .background(swatch)
                    .border(3.dp, ring, KeySetShapes.Small)
                    .clickable { onChange(i) },
            )
        }
    }
}

@Composable
private fun PositionButtons(onMove: (Int) -> Unit) {
    SectionTitle("Position")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onMove(-1) }, shape = KeySetShapes.Small) { Text("◀ Earlier") }
        OutlinedButton(onClick = { onMove(1) }, shape = KeySetShapes.Small) { Text("Later ▶") }
    }
}

@Composable
private fun EditorFooter(
    isNew: Boolean,
    canSave: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Spacer(Modifier.size(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isNew) TextButton(onClick = onDelete) { Text("Delete", color = Palette[4]) }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onCancel) { Text("Cancel") }
        Button(enabled = canSave, onClick = onSave, shape = KeySetShapes.Small) { Text("Save") }
    }
}

/** Small uppercase heading for an editor section, with an optional "?" tooltip. */
@Composable
internal fun SectionTitle(text: String, help: String? = null) {
    val top = if (help != null) 8.dp else 18.dp
    val bottom = if (help != null) 0.dp else 6.dp
    Row(Modifier.padding(top = top, bottom = bottom), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), fontSize = 12.sp, letterSpacing = 1.sp, color = Color.Gray)
        if (help != null) HelpButton(help)
    }
}
