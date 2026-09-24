package dev.keyset.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.keyset.core.designsystem.component.PageTitle
import dev.keyset.core.designsystem.component.softCircles
import dev.keyset.core.designsystem.theme.KeySetColors
import dev.keyset.core.designsystem.theme.KeySetShapes

/** A short how-to: what it's for, then the steps. */
private class Tutorial(val title: String, val summary: String, val steps: List<String>)

private val tutorials = listOf(
    Tutorial(
        "Connect your tablet or laptop",
        "Pair once; after that KeySet reconnects by itself.",
        listOf(
            "On the tablet or laptop, open its Bluetooth settings so the phone can find it.",
            "In KeySet, tap the status at the top left, then Scan.",
            "Tap Pair next to your device and accept on both screens.",
            "If it doesn't connect, connect once from the tablet or laptop's Bluetooth settings. " +
                "After that, KeySet connects by itself.",
        ),
    ),
    Tutorial(
        "Make a layout",
        "A layout is one set of buttons, for example one per app.",
        listOf(
            "Open the Layouts tab and tap New layout.",
            "Give it a name and tap OK. It becomes the layout in use.",
            "Go to the Pad tab and tap Add to fill it with buttons.",
            "Tap a card on the Layouts tab to switch layouts. The one in use is tinted.",
            "Use ⋮ on a card to rename, duplicate, change columns or delete it.",
        ),
    ),
    Tutorial(
        "Add and edit buttons",
        "Each button sends one shortcut, like Ctrl + Z.",
        listOf(
            "On the Pad, tap ✎ to start editing.",
            "Tap Add for a new button, or tap a button to change it.",
            "Pick the keys it presses: any modifiers (Ctrl, Shift…) plus a key.",
            "Give it a label, an icon and a colour, then tap Save.",
            "Tap ✓ to keep your changes, or ✕ to undo everything since you tapped ✎.",
        ),
    ),
    Tutorial(
        "Rearrange buttons",
        "Put the buttons you use most where your thumb rests.",
        listOf(
            "On the Pad, tap ✎.",
            "Drag a button by the dots in its bottom-right corner and drop it on another spot.",
            "Or open a button and use ◀ Earlier and Later ▶.",
            "Tap ✓ when it looks right.",
        ),
    ),
    Tutorial(
        "Tap, Hold and Toggle",
        "Choose how a button presses its keys.",
        listOf(
            "Tap sends the shortcut once.",
            "Hold keeps the key down while your finger is on the button, for example Space to pan.",
            "Toggle locks the key down with one tap and lets go with the next.",
            "Choose it under Behaviour when editing a button. Hold and Toggle buttons have a bright outline.",
        ),
    ),
    Tutorial(
        "Use the keyboard",
        "A full keyboard for everything the pad doesn't have.",
        listOf(
            "Open the Keyboard tab and turn the phone sideways.",
            "Tap Ctrl, Shift, Alt or Meta once for the next key, twice to lock it, or hold it " +
                "while pressing another key.",
            "Fn switches to the F-keys and the navigation keys.",
            "To send a whole sentence, tap the text box, type it with the keys, then tap Send.",
        ),
    ),
    Tutorial(
        "Share a layout",
        "Layouts travel as .keyset files, custom icons included.",
        listOf(
            "On the Layouts tab, tap ⋮ on a card, then Share or Save to file.",
            "To bring one in, tap ⋮ at the top, then Import, and pick the file.",
            "You can also open a .keyset file from another app, like Files or a chat.",
        ),
    ),
    Tutorial(
        "Fix connection problems",
        "What to do when keys don't arrive.",
        listOf(
            "If the status says Restart Bluetooth, tap Restart. KeySet reconnects by itself.",
            "KeySet types on one device at a time. Connecting to another disconnects the first.",
            "Only run KeySet on the phone. A tablet acting as a keyboard can't receive keys.",
            "If typed text looks wrong, set the tablet or laptop's keyboard layout to US.",
        ),
    ),
)

/** Mini tutorials: tap one to see its steps. */
@Composable
fun TutorialsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle("Tutorials", onBack)
        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (tutorial in tutorials) TutorialCard(tutorial)
        }
    }
}

/** One tutorial: its title and summary, opening to show the numbered steps. */
@Composable
private fun TutorialCard(tutorial: Tutorial) {
    var open by rememberSaveable(tutorial.title) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(KeySetShapes.Medium)
            .background(KeySetColors.Surface)
            .softCircles()
            .border(1.dp, Color.White.copy(alpha = 0.06f), KeySetShapes.Medium)
            .clickable(role = Role.Button) { open = !open }
            .semantics { stateDescription = if (open) "Open" else "Closed" }
            .animateContentSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tutorial.title, style = MaterialTheme.typography.titleMedium)
                Text(tutorial.summary, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Icon(
                if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray,
            )
        }
        if (open) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tutorial.steps.forEachIndexed { i, step -> Step(i + 1, step) }
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row {
        Text(
            "$number",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            modifier = Modifier.width(22.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
    }
}
