package dev.keyset.feature.keyboard

import android.content.pm.ActivityInfo
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.designsystem.theme.KeySetColors
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.model.Mod
import kotlinx.coroutines.launch

/**
 * A full keyboard, plus a box for typing text with the phone's own keyboard. Turns the phone
 * to landscape while it's shown, since a laptop-width keyboard needs the width.
 * [trailing] sits at the end of the text row (the app puts the connection status there).
 */
@Composable
fun KeyboardScreen(vibrate: Boolean, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    LandscapeWhileShown()
    Column(modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextSender(Modifier.weight(1f))
            trailing()
        }
        FullKeyboard(vibrate, Modifier.weight(1f).padding(bottom = 8.dp))
    }
}

/** Switches to landscape (either way up) while on screen, then back to what it was. */
@Composable
private fun LandscapeWhileShown() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity.requestedOrientation = previous }
    }
}

/** Type with the phone's keyboard (autocorrect, voice…) and send the text as key presses. */
@Composable
private fun TextSender(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun send() {
        sending = true
        scope.launch {
            val skipped = HidKeyboard.typeText(text)
            sending = false
            text = ""
            if (skipped > 0) {
                Toast.makeText(context, "$skipped character(s) have no key and were skipped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Type text to send") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        FilledIconButton(
            onClick = ::send,
            shape = KeySetShapes.Small,
            enabled = text.isNotEmpty() && !sending,
            modifier = Modifier.padding(start = 8.dp).size(56.dp),
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

@Composable
private fun FullKeyboard(vibrate: Boolean, modifier: Modifier) {
    var fnLayer by remember { mutableStateOf(false) }
    val modifiers = remember { mutableStateMapOf<Int, ModifierState>() }
    val shiftOn = (modifiers[Mod.SHIFT] ?: ModifierState.OFF) != ModifierState.OFF

    fun activeModifiers(): Int = modifiers.filterValues { it != ModifierState.OFF }.keys.fold(0) { mask, bit ->
        mask or
            bit
    }

    fun onModifierTap(bit: Int) {
        modifiers[bit] = when (modifiers[bit] ?: ModifierState.OFF) {
            ModifierState.OFF -> ModifierState.NEXT_KEY
            ModifierState.NEXT_KEY -> ModifierState.LOCKED
            ModifierState.LOCKED -> ModifierState.OFF
        }
    }

    fun afterKey() {
        modifiers.keys.filter { modifiers[it] == ModifierState.NEXT_KEY }.forEach { modifiers[it] = ModifierState.OFF }
    }

    val rows = if (fnLayer) FnLayer else MainLayer
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in rows) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (spec in row) {
                    // Non-modifier keys have modifier 0, which is never in the map.
                    val state = modifiers[spec.modifier] ?: ModifierState.OFF
                    KeyboardKey(
                        spec = spec,
                        label = if (shiftOn && spec.shifted != null) spec.shifted else spec.label,
                        modifierState = state,
                        vibrate = vibrate,
                        onTap = {
                            when {
                                spec.isModifier -> onModifierTap(spec.modifier)
                                spec.isLayerSwitch -> fnLayer = !fnLayer
                            }
                        },
                        activeModifiers = ::activeModifiers,
                        afterKey = ::afterKey,
                        modifier = Modifier.weight(spec.width).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/**
 * One key. Normal keys stay pressed while the finger is down, so the host repeats them like
 * a real keyboard. Modifier and Fn keys act on tap.
 */
@Composable
private fun KeyboardKey(
    spec: KeySpec,
    label: String,
    modifierState: ModifierState,
    vibrate: Boolean,
    onTap: () -> Unit,
    activeModifiers: () -> Int,
    afterKey: () -> Unit,
    modifier: Modifier,
) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    // The touch handler below outlives recompositions; always use the latest values.
    val currentOnTap by rememberUpdatedState(onTap)
    val currentVibrate by rememberUpdatedState(vibrate)
    val sendsKey = spec.usage != 0
    val source = "keyboard:${spec.usage}"

    val lit = pressed || modifierState != ModifierState.OFF
    val background = if (lit) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else KeySetColors.SurfaceHigh

    Box(
        modifier
            .clip(KeySetShapes.Small)
            .background(background)
            .semantics(mergeDescendants = true) {
                contentDescription = spec.description
                role = Role.Button
                if (spec.isModifier) stateDescription = modifierState.describe()
                onClick {
                    if (sendsKey) {
                        HidKeyboard.tap(source, activeModifiers(), spec.usage)
                        afterKey()
                    } else {
                        currentOnTap()
                    }
                    true
                }
            }
            .pointerInput(spec) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (currentVibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (sendsKey) HidKeyboard.press(source, activeModifiers(), spec.usage)
                    val lifted = waitForUpOrCancellation() != null
                    if (sendsKey) {
                        HidKeyboard.release(source)
                        afterKey()
                    } else if (lifted) {
                        currentOnTap()
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White,
            // Word labels (Shift, Enter…) are smaller so they fit narrow phone keys.
            fontSize = if (label.length > 2) 11.sp else 17.sp,
            fontWeight = if (spec.isModifier) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
        )
        if (modifierState == ModifierState.LOCKED) LockMark()
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
