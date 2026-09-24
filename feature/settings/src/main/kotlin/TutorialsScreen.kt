package dev.btdeck.feature.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.btdeck.core.designsystem.component.PageTitle
import dev.btdeck.core.designsystem.component.softCircles
import dev.btdeck.core.designsystem.theme.BTDeckColors
import dev.btdeck.core.designsystem.theme.BTDeckShapes
import kotlinx.coroutines.launch

/**
 * One step: where in the app it happens, the instruction, a line of extra detail, and a
 * screenshot with the thing to tap highlighted (or no picture).
 */
private class Step(val where: String, val text: String, val detail: String, @DrawableRes val image: Int? = null)

private class Tutorial(val title: String, val summary: String, val steps: List<Step>)

// Screenshots are taken from the app itself, at phone size, with the target highlighted.
// Every detail must stay true of the app; check them when behaviour changes.
private val tutorials = listOf(
    Tutorial(
        "Connect a device",
        "Pair once, then it reconnects by itself",
        listOf(
            Step(
                "Pad tab",
                "Tap the status to open Connect.",
                "It shows whether you're connected, and to what. It's at the top of the Pad, Layouts " +
                    "and Settings tabs.",
                R.drawable.tutorial_connect_1,
            ),
            Step(
                "Connect sheet",
                "Open Bluetooth on your tablet or laptop, then tap Scan and Pair.",
                "Your device only shows up while its Bluetooth settings are open. Accept the pairing " +
                    "request on both screens.",
                R.drawable.tutorial_connect_2,
            ),
            Step(
                "Connect sheet",
                "After that, just tap Connect.",
                "BTDeck also reconnects by itself. If the very first connect fails, connect once from " +
                    "the tablet or laptop's Bluetooth settings.",
                R.drawable.tutorial_connect_3,
            ),
        ),
    ),
    Tutorial(
        "Make a layout",
        "One set of buttons per app",
        listOf(
            Step(
                "Layouts tab",
                "Tap New layout.",
                "Make one for each app you use, like Photoshop or Krita.",
                R.drawable.tutorial_layout_1,
            ),
            Step("New layout", "Name it and tap OK.", "You can rename it any time.", R.drawable.tutorial_layout_2),
            Step(
                "Layouts tab",
                "The tinted card is the layout in use.",
                "Tap another card to switch to it; BTDeck takes you straight to the Pad.",
                R.drawable.tutorial_layout_3,
            ),
            Step(
                "Card menu",
                "⋮ renames, duplicates, sets columns or deletes.",
                "Columns is how many buttons fit in a row, from 2 to 6. Your last layout can't be deleted.",
                R.drawable.tutorial_layout_4,
            ),
        ),
    ),
    Tutorial(
        "Add a button",
        "Each button sends one shortcut",
        listOf(
            Step(
                "Pad tab",
                "Tap ✎ to start editing.",
                "In edit mode, tapping a button opens it instead of sending its shortcut.",
                R.drawable.tutorial_button_1,
            ),
            Step("Edit mode", "Tap Add.", "The dashed spots show where new buttons go.", R.drawable.tutorial_button_2),
            Step(
                "Button editor · Shortcut",
                "Pick modifiers and a key.",
                "Keys are grouped: letters, numbers, symbols, F-keys and more. The preview at the top " +
                    "shows the shortcut.",
                R.drawable.tutorial_button_3,
            ),
            Step(
                "Button editor · Display",
                "Name it and pick an icon. Then Save.",
                "Search icons by name, or tap + to use your own image. Colour is further down.",
                R.drawable.tutorial_button_4,
            ),
            Step(
                "Edit mode",
                "✓ keeps your changes. ✕ undoes them.",
                "✕ puts the layout back exactly as it was when you tapped ✎.",
                R.drawable.tutorial_button_5,
            ),
        ),
    ),
    Tutorial(
        "Rearrange buttons",
        "Drag them where your thumb rests",
        listOf(
            Step(
                "Edit mode",
                "Drag a button by its dots onto another spot.",
                "The spot under your finger is outlined. Let go to drop the button there.",
                R.drawable.tutorial_rearrange_1,
            ),
            Step(
                "Edit mode",
                "Tap ✓ when it looks right.",
                "No dragging? Open a button and use ◀ Earlier and Later ▶ on its Display tab.",
                R.drawable.tutorial_button_5,
            ),
        ),
    ),
    Tutorial(
        "Tap, Hold or Toggle",
        "How a button presses its keys",
        listOf(
            Step(
                "Button editor · Shortcut",
                "Pick Tap, Hold or Toggle under Behaviour.",
                "Tap sends the shortcut once. Hold keeps it down while your finger is on the button. " +
                    "Toggle locks it down until you tap again.",
                R.drawable.tutorial_hold_1,
            ),
            Step(
                "Pad tab",
                "Outlined buttons are Hold or Toggle.",
                "Hold suits keys like Space to pan: keep it pressed while you draw with the other hand.",
                R.drawable.tutorial_hold_2,
            ),
        ),
    ),
    Tutorial(
        "Use the keyboard",
        "Every key, with the phone turned sideways",
        listOf(
            Step(
                "Keyboard tab",
                "Tap Ctrl, Alt or Meta once for the next key, twice to lock it.",
                "Or hold one while pressing another key, like a real keyboard. Shift works the same way.",
                R.drawable.tutorial_keyboard_1,
            ),
            Step(
                "Keyboard tab",
                "Fn switches to F-keys and navigation keys.",
                "Tap abc, in the same corner, to go back to letters.",
                R.drawable.tutorial_keyboard_2,
            ),
            Step(
                "Keyboard tab",
                "For a whole sentence: tap the box, type, then Send.",
                "While the box is selected, the keys type into it instead of your device. Enter sends too.",
                R.drawable.tutorial_keyboard_3,
            ),
        ),
    ),
    Tutorial(
        "Share a layout",
        "Send it as a .keyset file, icons included",
        listOf(
            Step(
                "Card menu",
                "Tap Share or Save to file.",
                "The .keyset file includes your custom icons, so it looks the same on another phone.",
                R.drawable.tutorial_share_1,
            ),
            Step(
                "Layouts tab",
                "To add one: ⋮ at the top, then Import.",
                "You can also open a .keyset file straight from Files or a chat app.",
                R.drawable.tutorial_share_2,
            ),
        ),
    ),
    Tutorial(
        "Fix connection problems",
        "When keys don't arrive",
        listOf(
            Step(
                "Connect sheet",
                "Restart Bluetooth.",
                "Fixes most stuck connections. BTDeck reconnects by itself afterwards.",
                R.drawable.tutorial_fix_1,
            ),
            Step(
                "Your tablet or laptop",
                "Run BTDeck on the phone only.",
                "A device that's acting as a keyboard can't receive keys from another one.",
            ),
            Step(
                "Your tablet or laptop",
                "Odd characters? Use a US keyboard layout.",
                "BTDeck types as a US keyboard, so the other device should be set to that layout.",
            ),
        ),
    ),
)

/** Visual mini tutorials: pick one, then swipe through its steps. */
@Composable
fun TutorialsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    // Back closes an open tutorial before it leaves the page.
    BackHandler(enabled = open != null) { open = null }

    val tutorial = open?.let { tutorials[it] }
    Column(modifier.fillMaxSize()) {
        PageTitle(tutorial?.title ?: "Tutorials", onBack = { if (tutorial != null) open = null else onBack() })
        if (tutorial == null) {
            TutorialList(onOpen = { open = it })
        } else {
            TutorialSteps(tutorial, onDone = { open = null })
        }
    }
}

@Composable
private fun TutorialList(onOpen: (Int) -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tutorials.forEachIndexed { i, tutorial -> TutorialCard(tutorial, onClick = { onOpen(i) }) }
    }
}

/** A tutorial in the list: its first picture, title, and what it covers. */
@Composable
private fun TutorialCard(tutorial: Tutorial, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(BTDeckShapes.Medium)
            .background(BTDeckColors.Surface)
            .softCircles()
            .border(1.dp, Color.White.copy(alpha = 0.06f), BTDeckShapes.Medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val thumbnail = tutorial.steps.first().image
        if (thumbnail != null) {
            Image(
                painterResource(thumbnail),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(BTDeckShapes.Small),
            )
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(tutorial.title, style = MaterialTheme.typography.titleMedium)
            Text(tutorial.summary, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        Text("${tutorial.steps.size} steps", fontSize = 12.sp, color = Color.Gray)
    }
}

/** The steps of one tutorial: swipe, or use Back and Next. */
@Composable
private fun TutorialSteps(tutorial: Tutorial, onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { tutorial.steps.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == tutorial.steps.lastIndex

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        HorizontalPager(pager, Modifier.weight(1f), pageSpacing = 12.dp) { page ->
            StepPage(tutorial.steps[page], number = page + 1)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                enabled = pager.currentPage > 0,
            ) { Text("Back") }
            PageDots(count = tutorial.steps.size, current = pager.currentPage, modifier = Modifier.weight(1f))
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                shape = BTDeckShapes.Small,
            ) { Text(if (last) "Done" else "Next") }
        }
    }
}

@Composable
private fun StepPage(step: Step, number: Int) = BoxWithConstraints(Modifier.fillMaxSize()) {
    // The picture gets at most ~60% of the page, so the words below always show too.
    val pictureHeight = maxHeight * 0.6f
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (step.image != null) {
            val picture = painterResource(step.image)
            Image(
                picture,
                // The text below says what the picture shows.
                contentDescription = null,
                contentScale = ContentScale.Fit,
                // The biggest size with the picture's own shape that fits the width, 480dp (for
                // tablets) and pictureHeight. Pictures don't grow past their pixel size otherwise.
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .heightIn(max = pictureHeight)
                    .aspectRatio(picture.intrinsicSize.width / picture.intrinsicSize.height)
                    .clip(BTDeckShapes.Medium)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), BTDeckShapes.Medium),
            )
        }
        Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            // "3 · BUTTON EDITOR · SHORTCUT": which step, and where in the app it happens.
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("$number") }
                    append("  ·  ${step.where.uppercase()}")
                },
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = Color.Gray,
            )
            Text(step.text, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 4.dp))
            Text(
                step.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Which step you're on: a dot per step, the current one wider and in the accent colour. */
@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val on = i == current
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (on) 16.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)),
            )
        }
    }
}
