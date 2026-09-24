package dev.keyset

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.bluetooth.HidStatus
import dev.keyset.core.data.LayoutStore
import dev.keyset.core.designsystem.component.TopBar
import dev.keyset.core.model.KeySetSettings
import dev.keyset.core.model.Mod
import dev.keyset.core.model.PadButton
import dev.keyset.core.model.PadLayout
import dev.keyset.core.model.PadState
import dev.keyset.feature.connect.StatusBanner
import dev.keyset.feature.connect.StatusChip
import dev.keyset.feature.editor.ButtonEditor
import dev.keyset.feature.pad.DisconnectedStrip
import dev.keyset.feature.pad.FullscreenHint
import dev.keyset.feature.pad.Pad

/** The Pad tab: top bar, connection status, the button grid, and the button editor. */
@Composable
fun PadScreen(
    state: PadState,
    store: LayoutStore,
    settings: KeySetSettings,
    status: HidStatus,
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    onOpenConnect: () -> Unit,
    onFixStatus: (HidStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val heldIds by HidKeyboard.heldIds.collectAsState()
    // The layout as it was when editing started, so Cancel can put it back; null when not editing.
    var editStart by remember { mutableStateOf<PadLayout?>(null) }
    val editing = editStart != null
    var editTarget by remember { mutableStateOf<PadButton?>(null) }
    val padding = if (fullscreen) 8.dp else 12.dp

    // Never leave keys held down while the buttons are being edited.
    LaunchedEffect(editing) { if (editing) HidKeyboard.releaseAll() }

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            if (!fullscreen) {
                PadTopBar(
                    status = status,
                    editing = editing,
                    onOpenConnect = onOpenConnect,
                    onFullscreen = { onFullscreenChange(true) },
                    onEdit = { editStart = state.selected },
                    onDone = { editStart = null },
                    onCancel = {
                        editStart?.let { before -> store.updateLayout(before.id) { before } }
                        editStart = null
                    },
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = padding)) {
                if (fullscreen) {
                    // The top bar is hidden, so a dropped connection needs its own notice.
                    if (status !is HidStatus.Connected) DisconnectedStrip(onExit = { onFullscreenChange(false) })
                } else {
                    StatusBanner(status, onFixStatus, onOpenConnect)
                }
                Pad(
                    layout = state.selected,
                    editing = editing,
                    heldIds = heldIds,
                    settings = settings,
                    onEdit = { editTarget = it },
                    onMove = { button, to ->
                        store.moveButton(button.id, to - state.selected.buttons.indexOfFirst { it.id == button.id })
                    },
                    onAdd = { editTarget = PadButton(label = "", modifiers = Mod.CTRL, key = 0) },
                    modifier = Modifier.weight(1f).padding(vertical = padding),
                )
            }
        }
        if (fullscreen) FullscreenHint()
    }

    editTarget?.let { target -> EditorDialog(target, state, store, onClose = { editTarget = null }) }
}

/** Opens [ButtonEditor] for [target]; a target not yet in the layout is a new button. */
@Composable
private fun EditorDialog(target: PadButton, state: PadState, store: LayoutStore, onClose: () -> Unit) {
    ButtonEditor(
        initial = target,
        isNew = state.selected.buttons.none { it.id == target.id },
        onSave = {
            store.upsertButton(it)
            onClose()
        },
        onDelete = {
            store.deleteButton(target.id)
            onClose()
        },
        onMove = { delta -> store.moveButton(target.id, delta) },
        onDismiss = onClose,
    )
}

@Composable
private fun PadTopBar(
    status: HidStatus,
    editing: Boolean,
    onOpenConnect: () -> Unit,
    onFullscreen: () -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    // The status takes all the room the icons leave, so it can say who it's connected to.
    TopBar(leading = { StatusChip(status, onOpenConnect) }) {
        if (editing) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel editing", tint = Color.White)
            }
            IconButton(onClick = onDone) {
                Icon(Icons.Default.Check, contentDescription = "Done editing", tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            IconButton(onClick = onFullscreen) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit layout", tint = Color.White)
            }
        }
    }
}
