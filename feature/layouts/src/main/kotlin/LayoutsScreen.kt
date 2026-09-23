package dev.keyset.feature.layouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.keyset.core.data.LayoutStore
import dev.keyset.core.designsystem.component.ButtonIcon
import dev.keyset.core.designsystem.component.hasIcon
import dev.keyset.core.designsystem.theme.KeySetColors
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.designsystem.theme.Palette
import dev.keyset.core.model.PadLayout
import dev.keyset.core.model.PadState

/**
 * All layouts as cards. Tapping one makes it the current layout and calls [onOpen];
 * each card's menu renames, duplicates, changes columns or deletes it.
 */
@Composable
fun LayoutsScreen(state: PadState, store: LayoutStore, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    var dialog by remember { mutableStateOf<LayoutDialog?>(null) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Layouts", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { dialog = LayoutDialog.RestoreDefaults }) { Text("Restore defaults") }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                // Leaves room below the last row for the "New layout" button.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
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
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { dialog = LayoutDialog.Create },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("New layout") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    dialog?.let { LayoutDialogHost(it, store, onClose = { dialog = null }) }
}

@Composable
private fun LayoutCard(
    layout: PadLayout,
    selected: Boolean,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onAction: (LayoutDialog) -> Unit,
    onColumns: (Int) -> Unit,
) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    Card(
        onClick = onOpen,
        border = border,
        colors = CardDefaults.cardColors(containerColor = KeySetColors.Surface),
    ) {
        LayoutPreview(layout, Modifier.fillMaxWidth().aspectRatio(1.4f).padding(10.dp))
        Row(Modifier.padding(start = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    layout.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(layoutSummary(layout, selected), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            LayoutMenu(layout, canDelete, onAction, onColumns)
        }
    }
}

private fun layoutSummary(layout: PadLayout, selected: Boolean): String {
    val buttons = if (layout.buttons.size == 1) "1 button" else "${layout.buttons.size} buttons"
    return if (selected) "In use · $buttons" else buttons
}

/** A small picture of the layout: its first few rows of buttons in their colours. */
@Composable
private fun LayoutPreview(layout: PadLayout, modifier: Modifier) {
    val rows = layout.buttons.take(layout.columns * 4).chunked(layout.columns)
    if (rows.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Empty", color = Color.Gray) }
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in rows) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (button in row) {
                    val color = Palette.getOrElse(button.color) { Palette[0] }
                    Box(
                        Modifier.weight(
                            1f,
                        ).fillMaxSize().clip(KeySetShapes.Small).background(color.copy(alpha = 0.5f)),
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

/** The ⋮ menu on a layout card. */
@Composable
private fun LayoutMenu(
    layout: PadLayout,
    canDelete: Boolean,
    onAction: (LayoutDialog) -> Unit,
    onColumns: (Int) -> Unit,
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
