package dev.btdeck.feature.editor

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import dev.btdeck.core.designsystem.component.ButtonIcon
import dev.btdeck.core.designsystem.component.CompactTextField
import dev.btdeck.core.designsystem.component.HelpButton
import dev.btdeck.core.designsystem.component.hasIcon
import dev.btdeck.core.designsystem.theme.Palette
import dev.btdeck.core.designsystem.theme.ZexDeckColors
import dev.btdeck.core.designsystem.theme.ZexDeckShapes
import dev.btdeck.core.model.Mod
import dev.btdeck.core.model.PadButton
import dev.btdeck.core.model.PressMode
import dev.btdeck.core.model.comboLabel

private const val ICON_HELP =
    "Shown on the button instead of the shortcut. None shows the label and shortcut. " +
        "Tap + under Custom to use your own image (PNG with transparency works best)."

/** The editor's two tabs: how the button looks, and what it sends. */
private enum class EditorTab(val title: String) { Display("Display"), Shortcut("Shortcut") }

/**
 * Dialog for creating or editing one pad button. Changes are kept in a draft and only
 * handed to [onSave] when the user taps Save. A new button opens on Shortcut, since it
 * can't be saved without one; an existing one opens on Display.
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
    var tab by remember { mutableStateOf(if (isNew) EditorTab.Shortcut else EditorTab.Display) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = ZexDeckShapes.Medium,
            color = ZexDeckColors.Surface,
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 720.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                EditorHeader(draft, isNew)
                EditorTabs(tab, onSelect = { tab = it })
                // A fresh scroll position per tab, so each opens at its top.
                key(tab) {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        when (tab) {
                            EditorTab.Display -> {
                                LabelField(draft.label) { draft = draft.copy(label = it) }
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
                            EditorTab.Shortcut -> {
                                BehaviourPicker(draft.mode) { draft = draft.copy(mode = it) }
                                ModifierChips(draft.modifiers) { draft = draft.copy(modifiers = it) }
                                KeyPicker(draft.key) { draft = draft.copy(key = it) }
                            }
                        }
                    }
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

/** One line: the title, and a small live preview of the icon and shortcut being built. */
@Composable
private fun EditorHeader(draft: PadButton, isNew: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (isNew) "New button" else "Edit button",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier
                .clip(ZexDeckShapes.Small)
                .background(primary.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasIcon(draft.icon)) {
                ButtonIcon(draft.icon, draft.tintImage, primary, Modifier.padding(end = 6.dp).size(16.dp))
            }
            Text(
                comboLabel(draft.modifiers, draft.key, separator = " + "),
                color = primary,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTabs(selected: EditorTab, onSelect: (EditorTab) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = Color.Transparent,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        for (t in EditorTab.entries) {
            Tab(selected = t == selected, onClick = { onSelect(t) }, text = { Text(t.title) })
        }
    }
}

@Composable
private fun LabelField(label: String, onChange: (String) -> Unit) {
    SectionTitle("Label")
    CompactTextField(label, onChange, placeholder = "Optional", modifier = Modifier.fillMaxWidth())
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

@Composable
private fun BehaviourPicker(mode: PressMode, onChange: (PressMode) -> Unit) {
    val help = PressMode.entries.joinToString("\n") { "${it.label}: ${it.hint}" }
    SectionTitle("Behaviour", help = help)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        PressMode.entries.forEachIndexed { i, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = { onChange(m) },
                shape = SegmentedButtonDefaults.itemShape(i, PressMode.entries.size, ZexDeckShapes.Small),
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
                    .clip(ZexDeckShapes.Small)
                    .background(swatch)
                    .border(3.dp, ring, ZexDeckShapes.Small)
                    .clickable { onChange(i) },
            )
        }
    }
}

@Composable
private fun PositionButtons(onMove: (Int) -> Unit) {
    SectionTitle("Position")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onMove(-1) }, shape = ZexDeckShapes.Small) { Text("◀ Earlier") }
        OutlinedButton(onClick = { onMove(1) }, shape = ZexDeckShapes.Small) { Text("Later ▶") }
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
        Button(enabled = canSave, onClick = onSave, shape = ZexDeckShapes.Small) { Text("Save") }
    }
}

/** Small uppercase heading for an editor section, with an optional "?" tooltip. */
@Composable
internal fun SectionTitle(text: String, help: String? = null) {
    // With a "?" the row is already its 48dp touch height, so it gets almost no padding of its
    // own; both kinds of heading end up about the same distance from their content.
    val space = if (help != null) Modifier.padding(top = 4.dp) else Modifier.padding(top = 16.dp, bottom = 8.dp)
    Row(space, verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), fontSize = 12.sp, letterSpacing = 1.sp, color = Color.Gray)
        if (help != null) HelpButton(help)
    }
}
