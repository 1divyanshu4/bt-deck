package dev.keyset.feature.keyboard

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.designsystem.component.CompactTextField
import dev.keyset.core.designsystem.theme.KeySetColors
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.model.Keys
import dev.keyset.core.model.Mod
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A full keyboard, plus a text box to write in before sending. While the box has focus the
 * keys write into it instead of going to the other device, and Send (or Enter) types it all
 * out there; the phone's own keyboard never opens. [trailing] sits at the end of the text row
 * (the app puts the connection status there). The app turns this screen sideways on a tall
 * phone with [sidewaysWhenTall].
 */
@Composable
fun KeyboardScreen(vibrate: Boolean, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var draft by remember { mutableStateOf(TextFieldValue("")) }
    var writing by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    fun send() {
        val text = draft.text
        if (text.isEmpty() || sending) return
        // Done writing: the keys go to the other device again.
        focusManager.clearFocus()
        sending = true
        scope.launch {
            val skipped = HidKeyboard.typeText(text)
            sending = false
            draft = TextFieldValue("")
            if (skipped > 0) {
                Toast.makeText(context, "$skipped character(s) have no key and were skipped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun writeKey(spec: KeySpec, text: String) {
        when (spec.usage) {
            Keys.usage("Enter") -> send()
            Keys.usage("Esc") -> focusManager.clearFocus()
            else -> draft = draft.edit(spec.usage, text)
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextSender(
                draft = draft,
                onDraftChange = { draft = it },
                sending = sending,
                onSend = ::send,
                onWritingChange = { writing = it },
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
        FullKeyboard(
            vibrate = vibrate,
            writeKey = if (writing) ::writeKey else null,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp),
        )
    }
}

/**
 * Lays the content out in the space turned a quarter turn clockwise when that space is
 * taller than wide, so it reads upright with the phone turned left (the usual landscape).
 * A laptop-width keyboard needs the phone's long side, but the rest of the app stays upright.
 * Touches go through the same rotation, so keys still get the right finger positions.
 */
fun Modifier.sidewaysWhenTall() = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    if (height <= width) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val placeable = measurable.measure(Constraints.fixed(width = height, height = width))
    layout(width, height) {
        // Centre it, then turn it about its centre so it fills the space exactly.
        placeable.placeWithLayer(x = (width - height) / 2, y = (height - width) / 2) { rotationZ = 90f }
    }
}

/**
 * The text box and its Send button. The phone's own keyboard is kept from opening, so the
 * on-screen keys do the writing; tapping in the text still moves the cursor.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TextSender(
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
    sending: Boolean,
    onSend: () -> Unit,
    onWritingChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Every request to open the phone's keyboard for this box waits here forever instead.
        InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
            CompactTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = "Tap to write here, then Send",
                modifier = Modifier.weight(1f).onFocusChanged { onWritingChange(it.isFocused) },
            )
        }
        FilledIconButton(
            onClick = onSend,
            shape = KeySetShapes.Small,
            enabled = draft.text.isNotEmpty() && !sending,
            modifier = Modifier.padding(start = 8.dp).size(44.dp),
        ) {
            if (sending) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send text")
            }
        }
    }
}

/** Off → applies to the next key → locked on → off. */
private enum class ModifierState { OFF, NEXT_KEY, LOCKED }

/**
 * The keys and their state. Modifiers work two ways: tap one for the next key (tap again
 * to lock it), or hold it down while pressing another key, like a real keyboard.
 * With [writeKey] set, keys go to the text box instead of the other device.
 */
@Composable
private fun FullKeyboard(vibrate: Boolean, writeKey: ((KeySpec, String) -> Unit)?, modifier: Modifier) {
    var fnLayer by remember { mutableStateOf(false) }
    val modifiers = remember { mutableStateMapOf<Int, ModifierState>() }
    // Modifier keys a finger is on right now, and which of those another key was used with.
    val heldModifiers = remember { mutableStateListOf<Int>() }
    val usedWhileHeld = remember { mutableSetOf<Int>() }
    val capsLock by HidKeyboard.capsLock.collectAsState()

    fun stateOf(bit: Int) = modifiers[bit] ?: ModifierState.OFF
    val shiftOn = stateOf(Mod.SHIFT) != ModifierState.OFF || Mod.SHIFT in heldModifiers

    fun activeModifiers(): Int {
        val sticky = modifiers.filterValues { it != ModifierState.OFF }.keys
        return (sticky + heldModifiers).fold(0) { mask, bit -> mask or bit }
    }

    // ---- key actions ----

    fun pressKey(spec: KeySpec) {
        usedWhileHeld += heldModifiers
        // Caps Lock belongs to the other device, so it goes there even while writing.
        if (writeKey != null && !spec.isCapsLock) {
            // Shift is already in the label; Ctrl/Alt/Meta shortcuts mean nothing in the box.
            if (activeModifiers() and Mod.SHIFT.inv() == 0) writeKey(spec, spec.labelFor(shiftOn, capsLock))
            return
        }
        HidKeyboard.press(spec.source, activeModifiers(), spec.usage)
        if (spec.isCapsLock) HidKeyboard.toggleCapsLock()
    }

    fun releaseKey(spec: KeySpec) {
        HidKeyboard.release(spec.source)
        // Sticky modifiers are used up by one key.
        modifiers.keys.filter { modifiers[it] == ModifierState.NEXT_KEY }.forEach { modifiers[it] = ModifierState.OFF }
    }

    fun releaseModifier(bit: Int, lifted: Boolean) {
        heldModifiers.remove(bit)
        // Held down for a combo: it was only held, so it doesn't also toggle.
        if (usedWhileHeld.remove(bit) || !lifted) return
        modifiers[bit] = when (stateOf(bit)) {
            ModifierState.OFF -> ModifierState.NEXT_KEY
            ModifierState.NEXT_KEY -> ModifierState.LOCKED
            ModifierState.LOCKED -> ModifierState.OFF
        }
    }

    fun onDown(spec: KeySpec) {
        when {
            spec.isModifier -> heldModifiers += spec.modifier
            spec.isLayerSwitch -> Unit // switches on release
            else -> pressKey(spec)
        }
    }

    fun onUp(spec: KeySpec, lifted: Boolean) {
        when {
            spec.isModifier -> releaseModifier(spec.modifier, lifted)
            spec.isLayerSwitch -> if (lifted) fnLayer = !fnLayer
            else -> releaseKey(spec)
        }
    }

    // ---- layout ----

    val rows = if (fnLayer) FnLayer else MainLayer
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in rows) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (spec in row) {
                    val state = when {
                        spec.isModifier -> stateOf(spec.modifier).describe()
                        spec.isCapsLock -> if (capsLock) "On" else "Off"
                        else -> null
                    }
                    val locked = stateOf(spec.modifier) == ModifierState.LOCKED || (spec.isCapsLock && capsLock)
                    val on = spec.isModifier && (stateOf(spec.modifier) != ModifierState.OFF || spec.modifier in heldModifiers)
                    KeyboardKey(
                        spec = spec,
                        label = spec.labelFor(shift = shiftOn, capsLock = capsLock),
                        lit = on || locked,
                        locked = locked,
                        state = state,
                        vibrate = vibrate,
                        onDown = { onDown(spec) },
                        onUp = { lifted -> onUp(spec, lifted) },
                        modifier = Modifier.weight(spec.width).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/** Caps Lock flips letters only; Shift flips letters back and gives symbols. */
private fun KeySpec.labelFor(shift: Boolean, capsLock: Boolean): String {
    val upper = if (isLetter) shift != capsLock else shift
    return if (upper && shifted != null) shifted else label
}

/** Held keys are tracked by this id, so each key releases only itself. */
private val KeySpec.source get() = "keyboard:$usage"

/**
 * One key. It's down while the finger is on it, so the host repeats it like a real keyboard.
 * [onUp] always follows [onDown]; `lifted` is false when the touch was cancelled instead.
 */
@Composable
private fun KeyboardKey(
    spec: KeySpec,
    label: String,
    lit: Boolean,
    locked: Boolean,
    state: String?,
    vibrate: Boolean,
    onDown: () -> Unit,
    onUp: (lifted: Boolean) -> Unit,
    modifier: Modifier,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    // The touch handler below outlives recompositions; always use the latest values.
    val currentOnDown by rememberUpdatedState(onDown)
    val currentOnUp by rememberUpdatedState(onUp)
    val currentVibrate by rememberUpdatedState(vibrate)

    val background = if (pressed || lit) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else KeySetColors.SurfaceHigh

    Box(
        modifier
            .clip(KeySetShapes.Small)
            .background(background)
            .semantics(mergeDescendants = true) {
                contentDescription = spec.description
                role = Role.Button
                if (state != null) stateDescription = state
                onClick {
                    currentOnDown()
                    scope.launch {
                        delay(35)
                        currentOnUp(true)
                    }
                    true
                }
            }
            .pointerInput(spec) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (currentVibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    currentOnDown()
                    // The key that went down is the one to release, even if this spot shows
                    // another key by then (Fn switched layers mid-press).
                    val release = currentOnUp
                    var lifted = false
                    // finally: also runs if the key leaves the screen mid-press, so a held
                    // key can't stay stuck down on the host.
                    try {
                        lifted = waitForUpOrCancellation() != null
                    } finally {
                        pressed = false
                        release(lifted)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White,
            // Word labels (Shift, Enter…) are smaller so they fit narrow phone keys.
            fontSize = if (label.length > 2) 11.sp else 17.sp,
            fontWeight = if (spec.isModifier || spec.isCapsLock) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
        )
        if (locked) LockMark()
    }
}

/** A small bar under a locked modifier key. */
@Composable
private fun LockMark() {
    Box(Modifier.fillMaxSize().padding(bottom = 4.dp), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.size(width = 14.dp, height = 3.dp).clip(KeySetShapes.Small).background(Color.White))
    }
}

private fun ModifierState.describe() = when (this) {
    ModifierState.OFF -> "Off"
    ModifierState.NEXT_KEY -> "On for next key"
    ModifierState.LOCKED -> "Locked on"
}
