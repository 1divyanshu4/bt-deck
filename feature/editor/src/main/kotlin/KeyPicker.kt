package dev.btdeck.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.btdeck.core.designsystem.theme.BTDeckShapes
import dev.btdeck.core.model.Key
import dev.btdeck.core.model.Keys

/** Every key the pad can send, in the groups the editor shows them in. */
internal val KeyGroups: List<Pair<String, List<Key>>> = listOf(
    "Letters" to named(('A'..'Z').map { "$it" }),
    "Numbers" to named(('1'..'9').map { "$it" } + "0"),
    "Symbols" to named(listOf("-", "=", "[", "]", "\\", ";", "'", "`", ",", ".", "/")),
    "Editing" to named(listOf("Space", "Enter", "Tab", "Backspace", "Delete", "Insert", "Esc")),
    "Navigation" to named(listOf("Up", "Down", "Left", "Right", "Home", "End", "PgUp", "PgDn")),
    "Function" to named((1..12).map { "F$it" }),
    // None: a button that only holds modifiers, like Ctrl on its own.
    "Other" to named(listOf("None", "Caps Lock", "Print Screen", "Scroll Lock", "Pause", "Menu")),
)

private fun named(names: List<String>) = names.map { name -> Keys.all.first { it.name == name } }

/**
 * The key a button presses: pick a group, then a key in it. Opens on the group of the
 * current [key] (Letters when there's none yet).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeyPicker(key: Int, onChange: (Int) -> Unit) {
    val current = KeyGroups.indexOfFirst { (_, keys) -> key != 0 && keys.any { it.usage == key } }
    var group by remember { mutableIntStateOf(current.coerceAtLeast(0)) }

    SectionTitle("Key")
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KeyGroups.forEachIndexed { i, (name, _) -> GroupTab(name, selected = i == group) { group = i } }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (k in KeyGroups[group].second) {
            FilterChip(
                selected = key == k.usage,
                onClick = { onChange(k.usage) },
                label = { Text(k.name) },
            )
        }
    }
}

/** A key group's name; the chosen one is filled with the accent colour. */
@Composable
private fun GroupTab(name: String, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Text(
        name,
        color = if (selected) primary else Color.Gray,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(BTDeckShapes.Small)
            .background(if (selected) primary.copy(alpha = 0.16f) else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
