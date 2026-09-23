package dev.keyset.feature.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import dev.keyset.core.data.LayoutStore
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.designsystem.theme.Palette
import dev.keyset.core.model.PadLayout

/** The dialogs the Layouts screen can open. */
internal sealed interface LayoutDialog {
    data object Create : LayoutDialog

    data object RestoreDefaults : LayoutDialog

    data class Rename(val layout: PadLayout) : LayoutDialog

    data class Duplicate(val layout: PadLayout) : LayoutDialog

    data class Delete(val layout: PadLayout) : LayoutDialog
}

/** Shows [dialog] and applies the result to [store]. */
@Composable
internal fun LayoutDialogHost(dialog: LayoutDialog, store: LayoutStore, onClose: () -> Unit) {
    when (dialog) {
        LayoutDialog.Create -> NamePrompt("New layout", initial = "", onDismiss = onClose) { name ->
            store.createLayout(name)
            onClose()
        }
        LayoutDialog.RestoreDefaults -> AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Restore default layouts?") },
            text = { Text("The built-in layouts are added again. Your own layouts are kept.") },
            confirmButton = {
                Button(onClick = {
                    store.restoreDefaults()
                    onClose()
                }, shape = KeySetShapes.Small) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
        )
        is LayoutDialog.Rename -> NamePrompt("Rename layout", dialog.layout.name, onDismiss = onClose) { name ->
            store.updateLayout(dialog.layout.id) { it.copy(name = name) }
            onClose()
        }
        is LayoutDialog.Duplicate -> NamePrompt(
            "Duplicate layout",
            "${dialog.layout.name} copy",
            onDismiss = onClose,
        ) { name ->
            store.duplicateLayout(dialog.layout.id, name)
            onClose()
        }
        is LayoutDialog.Delete -> ConfirmDelete(dialog.layout.name, onDismiss = onClose) {
            store.deleteLayout(dialog.layout.id)
            onClose()
        }
    }
}

@Composable
private fun NamePrompt(title: String, initial: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") })
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onDone(name.trim()) }, shape = KeySetShapes.Small) {
                Text("OK")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ConfirmDelete(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"$name\"?") },
        text = { Text("Its buttons will be deleted too.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = KeySetShapes.Small,
                colors = ButtonDefaults.buttonColors(containerColor = Palette[4]),
            ) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** "Columns 2 3 4 5 6" with the current count highlighted. */
@Composable
internal fun ColumnsPicker(columns: Int, onChange: (Int) -> Unit) {
    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Columns", Modifier.padding(end = 8.dp))
        for (n in 2..6) {
            val selected = columns == n
            val background = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            Box(
                Modifier
                    .padding(2.dp)
                    .size(36.dp)
                    .clip(KeySetShapes.Small)
                    .background(background)
                    .clickable { onChange(n) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$n", color = if (selected) Color.Black else Color.White)
            }
        }
    }
}
