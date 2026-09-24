package dev.btdeck.feature.layouts

import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.btdeck.core.data.LayoutArchive
import dev.btdeck.core.data.LayoutTransfer
import dev.btdeck.core.designsystem.theme.BTDeckShapes
import kotlinx.coroutines.launch

/**
 * Reads the `.keyset` file at [uri], shows what's in it, and imports it if the user agrees.
 * Used for files picked with Import and for files opened from other apps.
 */
@Composable
fun ImportLayoutDialog(uri: Uri, transfer: LayoutTransfer, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var contents by remember { mutableStateOf<LayoutArchive.Contents?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        runCatching { transfer.read(uri) }
            .onSuccess { contents = it }
            .onFailure { error = it.message ?: "Couldn't read that file." }
    }

    val found = contents
    when {
        error != null -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Can't import") },
            text = { Text(error!!) },
            confirmButton = { TextButton(onClick = onDone) { Text("OK") } },
        )
        found != null -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Import \"${found.layout.name}\"?") },
            text = { Text(describe(found)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val name = transfer.import(found)
                        Toast.makeText(context, "Imported \"$name\"", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                }, shape = BTDeckShapes.Small) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
        )
    }
}

private fun describe(contents: LayoutArchive.Contents): String {
    val buttons = contents.layout.buttons.size
    val images = contents.images.size
    val buttonText = if (buttons == 1) "1 button" else "$buttons buttons"
    val imageText = when (images) {
        0 -> ""
        1 -> " and 1 custom image"
        else -> " and $images custom images"
    }
    return "It has $buttonText$imageText. It's added as a new layout; your layouts aren't changed."
}
