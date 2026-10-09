package dev.btdeck.feature.layouts

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.btdeck.core.data.LayoutStore
import dev.btdeck.core.data.LayoutTransfer
import dev.btdeck.core.designsystem.component.ButtonIcon
import dev.btdeck.core.designsystem.component.CornerBadge
import dev.btdeck.core.designsystem.component.TopBar
import dev.btdeck.core.designsystem.component.hasIcon
import dev.btdeck.core.designsystem.component.softCircles
import dev.btdeck.core.designsystem.theme.Palette
import dev.btdeck.core.designsystem.theme.ZexDeckColors
import dev.btdeck.core.designsystem.theme.ZexDeckShapes
import dev.btdeck.core.model.PadLayout
import dev.btdeck.core.model.PadState
import kotlinx.coroutines.launch

/**
 * All layouts as cards. Tapping one makes it the current layout and calls [onOpen]; each
 * card's menu renames, duplicates, shares, saves, changes columns or deletes it. Layouts can
 * also be imported from `.keyset` files.
 */
@Composable
fun LayoutsScreen(
    state: PadState,
    store: LayoutStore,
    transfer: LayoutTransfer,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    status: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<LayoutDialog?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var exporting by remember { mutableStateOf<PadLayout?>(null) }

    // Any file type: shared .keyset files often arrive without a recognisable type.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importUri = uri
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(KEYSET_MIME)) { uri ->
        val layout = exporting ?: return@rememberLauncherForActivityResult
        exporting = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = runCatching { transfer.export(layout, uri) }.isSuccess
            Toast.makeText(context, if (saved) "Saved" else "Couldn't save the file", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier.fillMaxSize()) {
        TopBar(leading = status) {
            TextButton(onClick = { dialog = LayoutDialog.Create }) { Text("New layout") }
            ScreenMenu(
                onImport = { pickFile.launch(arrayOf("*/*")) },
                onRestoreDefaults = { dialog = LayoutDialog.RestoreDefaults },
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            // Same edge and gap below the bar as the Pad.
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.layouts, key = { it.id }) { layout ->
                LayoutCard(
                    layout = layout,
                    selected = layout.id == state.selectedId,
                    canDelete = state.layouts.size > 1,
                    onOpen = {
                        store.select(layout.id)
                        onOpen()
                    },
                    onAction = { dialog = it },
                    onColumns = { n -> store.updateLayout(layout.id) { it.copy(columns = n) } },
                    onShare = { scope.launch { shareLayout(context, transfer, layout) } },
                    onSave = {
                        exporting = layout
                        saveFile.launch(LayoutTransfer.fileName(layout))
                    },
                )
            }
        }
    }

    dialog?.let { LayoutDialogHost(it, store, onClose = { dialog = null }) }
    importUri?.let { ImportLayoutDialog(it, transfer, onDone = { importUri = null }) }
}

/** The ⋮ menu at the end of the top bar. */
@Composable
private fun ScreenMenu(onImport: () -> Unit, onRestoreDefaults: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Import") }, onClick = {
                open = false
                onImport()
            })
            DropdownMenuItem(text = { Text("Restore defaults") }, onClick = {
                open = false
                onRestoreDefaults()
            })
        }
    }
}

@Composable
private fun LayoutCard(
    layout: PadLayout,
    selected: Boolean,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onAction: (LayoutDialog) -> Unit,
    onColumns: (Int) -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    // Same surface as the pad keys; the layout in use is tinted with the accent colour.
    val surface = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f).compositeOver(ZexDeckColors.Surface)
    } else {
        ZexDeckColors.Surface
    }
    Card(
        onClick = onOpen,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        colors = CardDefaults.cardColors(containerColor = surface),
        // The tint shows the layout in use; this says it to screen readers.
        modifier = Modifier.semantics { this.selected = selected },
    ) {
        Column(Modifier.softCircles()) {
            LayoutPreview(layout, Modifier.fillMaxWidth().aspectRatio(1.4f).padding(10.dp))
            Row(Modifier.padding(start = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Takes the room the count and menu leave, and ends in "…" if it's too long.
                Text(
                    layout.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ButtonCount(layout.buttons.size, Modifier.padding(start = 8.dp))
                LayoutMenu(layout, canDelete, onAction, onColumns, onShare, onSave)
            }
        }
    }
}

/**
 * How many buttons the layout has, next to its name. Accent-coloured, so it isn't mistaken
 * for a button's shortcut badge.
 */
@Composable
private fun ButtonCount(count: Int, modifier: Modifier) {
    CornerBadge(
        "$count",
        modifier.semantics { contentDescription = if (count == 1) "1 button" else "$count buttons" },
        background = MaterialTheme.colorScheme.primary,
        color = MaterialTheme.colorScheme.onPrimary,
    )
}

/** A small picture of the layout: its first [PREVIEW_ROWS] rows of buttons in their colours. */
@Composable
private fun LayoutPreview(layout: PadLayout, modifier: Modifier) {
    val rows = layout.buttons.take(layout.columns * PREVIEW_ROWS).chunked(layout.columns)
    if (rows.isEmpty()) {
        // A dark panel where the buttons would be, so an empty card still has a picture.
        Box(
            modifier.clip(ZexDeckShapes.Small).background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) { Text("Empty", color = Color.Gray) }
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in rows) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (button in row) {
                    val color = Palette.getOrElse(button.color) { Palette[0] }
                    // Like the pad keys: a fuller colour and a lighter edge, so even neutral
                    // grey tiles stand out from the card behind them, tinted or not.
                    val edge = lerp(color, Color.White, 0.35f).copy(alpha = 0.35f)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(ZexDeckShapes.Small)
                            .background(color.copy(alpha = 0.62f))
                            .border(1.dp, edge, ZexDeckShapes.Small),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hasIcon(button.icon)) {
                            ButtonIcon(
                                button.icon,
                                button.tintImage,
                                Color.White,
                                Modifier.size(14.dp),
                                description = null,
                            )
                        }
                    }
                }
                repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val PREVIEW_ROWS = 2

/** The ⋮ menu on a layout card. */
@Composable
private fun LayoutMenu(
    layout: PadLayout,
    canDelete: Boolean,
    onAction: (LayoutDialog) -> Unit,
    onColumns: (Int) -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options for ${layout.name}")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = {
                open = false
                onAction(LayoutDialog.Rename(layout))
            })
            DropdownMenuItem(text = { Text("Duplicate") }, onClick = {
                open = false
                onAction(LayoutDialog.Duplicate(layout))
            })
            DropdownMenuItem(text = { Text("Share") }, onClick = {
                open = false
                onShare()
            })
            DropdownMenuItem(text = { Text("Save to file") }, onClick = {
                open = false
                onSave()
            })
            ColumnsPicker(layout.columns, onColumns)
            if (canDelete) {
                DropdownMenuItem(text = { Text("Delete", color = Palette[4]) }, onClick = {
                    open = false
                    onAction(LayoutDialog.Delete(layout))
                })
            }
        }
    }
}
