package dev.keyset.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.keyset.core.data.LayoutStore
import dev.keyset.core.designsystem.theme.Palette
import dev.keyset.core.model.PadState

/** The menu entries that ask for a name, with their menu label and dialog title. */
private enum class NameDialog(val menuLabel: String, val title: String) {
    New("New empty layout", "New layout"),
    Duplicate("Duplicate layout", "Duplicate layout"),
    Rename("Rename layout", "Rename layout"),
}

/**
 * The layout title with its dropdown: switch, create, duplicate, rename and delete layouts,
 * and set the column count.
 */
@Composable
fun LayoutMenu(state: PadState, store: LayoutStore, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf<NameDialog?>(null) }
    val close = { open = false }

    Box(modifier) {
        LayoutTitle(state.selected.name, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = close) {
            LayoutItems(state, onSelect = { id ->
                store.select(id)
                close()
            })
            HorizontalDivider()
            for (kind in NameDialog.entries) {
                DropdownMenuItem(text = { Text(kind.menuLabel) }, onClick = {
                    nameDialog = kind
                    close()
                })
            }
            HorizontalDivider()
            ColumnsPicker(state.selected.columns) { n -> store.updateSelected { it.copy(columns = n) } }
            // Never delete the last layout.
            if (state.layouts.size > 1) {
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Delete layout", color = Palette[4]) }, onClick = {
                    store.deleteSelected()
                    close()
                })
            }
        }
    }

    nameDialog?.let { kind ->
        LayoutNameDialog(kind, state, store, onClose = { nameDialog = null })
    }
}

@Composable
private fun LayoutTitle(name: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = "Layouts", tint = Color.White)
    }
}

/** One entry per layout; the current one is bold. */
@Composable
private fun LayoutItems(state: PadState, onSelect: (String) -> Unit) {
    for (layout in state.layouts) {
        val weight = if (layout.id == state.selectedId) FontWeight.Bold else null
        DropdownMenuItem(text = { Text(layout.name, fontWeight = weight) }, onClick = { onSelect(layout.id) })
    }
}

/** "Columns 2 3 4 5 6" with the current count highlighted. */
@Composable
private fun ColumnsPicker(columns: Int, onChange: (Int) -> Unit) {
    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Columns", Modifier.padding(end = 8.dp))
        for (n in 2..6) {
            val selected = columns == n
            val background = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            Box(
                Modifier
                    .padding(2.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(background)
                    .clickable { onChange(n) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$n", color = if (selected) Color.Black else Color.White)
            }
        }
    }
}

/** Asks for a name, then creates, duplicates or renames a layout depending on [kind]. */
@Composable
private fun LayoutNameDialog(kind: NameDialog, state: PadState, store: LayoutStore, onClose: () -> Unit) {
    val current = state.selected.name
    val initial = when (kind) {
        NameDialog.New -> ""
        NameDialog.Duplicate -> "$current copy"
        NameDialog.Rename -> current
    }
    NamePrompt(kind.title, initial, onDismiss = onClose, onDone = { name ->
        when (kind) {
            NameDialog.New -> store.addLayout(name, copyCurrent = false)
            NameDialog.Duplicate -> store.addLayout(name, copyCurrent = true)
            NameDialog.Rename -> store.updateSelected { it.copy(name = name) }
        }
        onClose()
    })
}

@Composable
private fun NamePrompt(title: String, initial: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") })
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onDone(name.trim()) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
