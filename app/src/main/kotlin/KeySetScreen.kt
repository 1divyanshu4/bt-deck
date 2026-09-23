package dev.keyset

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.bluetooth.HidStatus
import dev.keyset.core.data.LayoutStore
import dev.keyset.core.designsystem.theme.KeySetColors
import dev.keyset.core.model.Mod
import dev.keyset.core.model.PadButton
import dev.keyset.core.model.PadState
import dev.keyset.feature.connect.ConnectSheet
import dev.keyset.feature.connect.StatusBanner
import dev.keyset.feature.connect.StatusChip
import dev.keyset.feature.editor.ButtonEditor
import dev.keyset.feature.editor.LayoutMenu
import dev.keyset.feature.pad.DisconnectedStrip
import dev.keyset.feature.pad.FullscreenHint
import dev.keyset.feature.pad.Pad
import dev.keyset.feature.pad.SystemBarsHidden

/** The whole app: top bar, status, pad, and the editor / connect overlays. */
@Composable
fun KeySetScreen(
    store: LayoutStore,
    onFixStatus: (HidStatus) -> Unit,
    onMakeDiscoverable: () -> Unit,
    onScan: () -> Unit,
) {
    val state by store.state.collectAsState()
    val status by HidKeyboard.status.collectAsState()
    val heldIds by HidKeyboard.heldIds.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<PadButton?>(null) }
    var showConnect by remember { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editing) { if (editing) HidKeyboard.releaseAll() }
    SystemBarsHidden(fullscreen)
    BackHandler(enabled = fullscreen) { fullscreen = false }

    val openConnect = { showConnect = true }
    val enterFullscreen = {
        editing = false
        fullscreen = true
    }
    val padding = if (fullscreen) 8.dp else 12.dp

    Surface(color = KeySetColors.Background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(Modifier.fillMaxSize().padding(horizontal = padding)) {
                if (fullscreen) {
                    // The top bar is hidden, so a dropped connection needs its own notice.
                    if (status !is HidStatus.Connected) DisconnectedStrip(onExit = { fullscreen = false })
                } else {
                    TopBar(state, store, status, editing, { editing = !editing }, openConnect, enterFullscreen)
                    StatusBanner(status, onFixStatus, openConnect)
                }
                Pad(
                    layout = state.selected,
                    editing = editing,
                    heldIds = heldIds,
                    onEdit = { editTarget = it },
                    onAdd = { editTarget = PadButton(label = "", modifiers = Mod.CTRL, key = 0) },
                    modifier = Modifier.weight(1f).padding(vertical = padding),
                )
            }
            if (fullscreen) FullscreenHint()
        }
    }

    editTarget?.let { target -> EditorDialog(target, state, store, onClose = { editTarget = null }) }
    if (showConnect) ConnectSheet(status, onMakeDiscoverable, onScan, onDismiss = { showConnect = false })
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
private fun TopBar(
    state: PadState,
    store: LayoutStore,
    status: HidStatus,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onOpenConnect: () -> Unit,
    onFullscreen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LayoutMenu(state, store, Modifier.weight(1f))
        StatusChip(status, onOpenConnect)
        IconButton(onClick = onFullscreen) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
        }
        IconButton(onClick = onToggleEdit) {
            Icon(
                if (editing) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = if (editing) "Done editing" else "Edit layout",
                tint = if (editing) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
    }
}
