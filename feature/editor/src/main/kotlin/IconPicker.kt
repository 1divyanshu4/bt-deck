package dev.keyset.feature.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.keyset.core.data.CustomIcons
import dev.keyset.core.designsystem.component.ButtonIcon
import dev.keyset.core.designsystem.component.CompactTextField
import dev.keyset.core.designsystem.icon.PadIcon
import dev.keyset.core.designsystem.icon.PadIcons
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.designsystem.theme.Palette
import kotlinx.coroutines.launch

private val GroupTitleColor = Color(0xFF9999A4)

/**
 * Pick a built-in icon, one of the user's own images, or none. [selected] is the icon key;
 * [tint] highlights the selected tile. [tintImage] only applies to custom images.
 */
@Composable
internal fun IconPicker(
    selected: String?,
    tint: Color,
    tintImage: Boolean,
    onTintImage: (Boolean) -> Unit,
    onPick: (String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val search = query.trim()
    val groups = filterIcons(search)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CompactTextField(query, { query = it }, placeholder = "Search icons", modifier = Modifier.fillMaxWidth())
        if (search.isEmpty()) {
            // Its text is its label.
            IconTile(description = null, selected = selected == null, tint = tint, onClick = { onPick(null) }) {
                Text("None", fontSize = 11.sp, color = Color.White)
            }
            CustomImages(selected, tint, tintImage, onTintImage, onPick)
        }
        for ((title, icons) in groups) {
            IconGroup(title, icons, selected, tint, onPick)
        }
        if (groups.isEmpty()) Text("No icons match \"$search\"", fontSize = 14.sp, color = Color.Gray)
    }
}

/** Built-in icon groups whose names contain [search] (all groups when it's empty). */
private fun filterIcons(search: String): List<Pair<String, List<PadIcon>>> {
    if (search.isEmpty()) return PadIcons.groups
    return PadIcons.groups
        .map { (title, icons) -> title to icons.filter { it.name.contains(search, ignoreCase = true) } }
        .filter { (_, icons) -> icons.isNotEmpty() }
}

/** The user's imported images, an "add image" tile, and tint/delete for the selected one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomImages(
    selected: String?,
    tint: Color,
    tintImage: Boolean,
    onTintImage: (Boolean) -> Unit,
    onPick: (String?) -> Unit,
) {
    val customNames by CustomIcons.names.collectAsState()
    val (importing, pickImage) = rememberImageImporter(onImported = onPick)

    Text("Custom", fontSize = 12.sp, color = GroupTitleColor)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        IconTile("Add image", selected = false, tint = tint, onClick = { if (!importing) pickImage() }) {
            if (importing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
        customNames.forEachIndexed { i, name ->
            val key = CustomIcons.key(name)
            val isSelected = selected == key
            // Images have no names of their own; number them for screen readers.
            IconTile("Custom image ${i + 1}", selected = isSelected, tint = tint, onClick = { onPick(key) }) {
                ButtonIcon(key, tintImage && isSelected, Color.White, Modifier.size(30.dp), description = null)
            }
        }
    }

    if (selected != null && CustomIcons.isCustom(selected)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = tintImage, onCheckedChange = onTintImage)
            Text("Tint white", fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp).weight(1f))
            TextButton(onClick = {
                CustomIcons.delete(selected)
                onPick(null)
            }) { Text("Delete image", color = Palette[4]) }
        }
    }
}

/**
 * Opens the system photo picker and imports the chosen image. Returns whether an import is
 * running and the function that starts the picker.
 */
@Composable
private fun rememberImageImporter(onImported: (String) -> Unit): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val key = CustomIcons.import(context, uri)
            importing = false
            if (key != null) {
                onImported(key)
            } else {
                Toast.makeText(context, "Couldn't read that image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    return importing to { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGroup(title: String, icons: List<PadIcon>, selected: String?, tint: Color, onPick: (String) -> Unit) {
    Text(title, fontSize = 12.sp, color = GroupTitleColor)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (icon in icons) {
            IconTile(icon.name, selected = selected == icon.key, tint = tint, onClick = { onPick(icon.key) }) {
                Icon(icon.vector, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * A 48dp square choice; the selected one is filled with [tint] and outlined. [description] is
 * its name for screen readers, on the tile itself so it holds even if the picture doesn't load.
 */
@Composable
private fun IconTile(
    description: String?,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = KeySetShapes.Small
    val background = if (selected) tint else Color.White.copy(alpha = 0.06f)
    val outline = if (selected) Color.White else Color.Transparent
    Box(
        Modifier
            .size(48.dp)
            .clip(shape)
            .background(background)
            .border(2.dp, outline, shape)
            .clickable(onClick = onClick)
            .semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}
