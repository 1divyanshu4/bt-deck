package dev.btdeck

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.btdeck.core.bluetooth.HidKeyboard
import dev.btdeck.core.model.Mod
import dev.btdeck.core.model.PadButton
import dev.btdeck.core.model.PressMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PadButtonsTest : ZexDeckUiTest() {

    private fun pad(label: String) = rule.onNodeWithContentDescription(label, useUnmergedTree = false)

    /** What TalkBack / switch access do: invoke the click action, no touch involved. */
    private fun androidx.compose.ui.test.SemanticsNodeInteraction.a11yClick() =
        performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)

    @Test
    fun everyDefaultButtonSendsItsShortcut() {
        for (b in store.state.value.selected.buttons) {
            reports.clear()
            val node = pad(b.label)
            val want = expected(b.modifiers to b.key)
            when (b.mode) {
                PressMode.TAP -> {
                    node.performTouchInput {
                        down(center)
                        up()
                    }
                    awaitReport(want)
                    awaitReleased()
                }
                PressMode.HOLD -> {
                    node.performTouchInput { down(center) }
                    awaitReport(want)
                    assertEquals("${b.label} must stay down while held", want, lastReport)
                    node.performTouchInput { up() }
                    awaitReleased()
                }
                PressMode.TOGGLE -> error("no toggle buttons in defaults")
            }
        }
    }

    @Test
    fun holdingPanWhileTappingUndo_sendsBothTogether() {
        val pan = pad("Pan").centerInRoot()
        val undo = pad("Undo").centerInRoot()
        rule.onRoot().performTouchInput {
            down(0, pan)
            advanceEventTime(100)
            down(1, undo)
            advanceEventTime(50)
            up(1)
            advanceEventTime(100)
        }
        awaitReport(expected(0 to key("Space"), Mod.CTRL to key("Z")))
        // Undo released, Space still held.
        rule.waitUntil(3_000) { lastReport == expected(0 to key("Space")) }
        rule.onRoot().performTouchInput { up(0) }
        awaitReleased()
    }

    @Test
    fun twoHeldModifiersCombine() {
        val ctrl = pad("Ctrl").centerInRoot()
        val shift = pad("Shift").centerInRoot()
        rule.onRoot().performTouchInput {
            down(0, ctrl)
            advanceEventTime(50)
            down(1, shift)
            advanceEventTime(50)
        }
        awaitReport(expected(Mod.CTRL or Mod.SHIFT to 0))
        rule.onRoot().performTouchInput {
            up(1)
            up(0)
        }
        awaitReleased()
    }

    @Test
    fun toggleButton_latchesUntilTappedAgain() {
        rule.runOnUiThread {
            store.upsertButton(
                PadButton(id = "t", label = "Lock Alt", modifiers = Mod.ALT, key = 0, mode = PressMode.TOGGLE),
            )
        }
        rule.waitForIdle()
        val node = pad("Lock Alt")
        node.performTouchInput {
            down(center)
            up()
        }
        awaitReport(expected(Mod.ALT to 0))
        rule.waitForIdle()
        assertEquals("still latched after lifting the finger", expected(Mod.ALT to 0), lastReport)
        assertTrue(HidKeyboard.heldIds.value.contains("t"))
        node.performTouchInput {
            down(center)
            up()
        }
        awaitReleased()
        assertTrue(HidKeyboard.heldIds.value.isEmpty())
    }

    @Test
    fun editMode_tapsOpenTheEditorAndSendNothing() {
        rule.onNodeWithContentDescription("Edit layout").performClick()
        reports.clear()
        pad("Undo").performTouchInput {
            down(center)
            up()
        }
        rule.waitForIdle()
        assertTrue("edit mode must not type", reports.none { it.toList() != released })
        rule.onNode(androidx.compose.ui.test.hasText("Edit button")).assertExists()
    }

    @Test
    fun leavingTheApp_releasesHeldKeys() {
        pad("Pan").performTouchInput { down(center) }
        awaitReport(expected(0 to key("Space")))
        rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED) // onPause
        awaitReleased()
    }

    @Test
    fun padKeysAreAccessibleButtons() {
        // TalkBack / switch access need a click action on every key, not just raw touch.
        for (b in store.state.value.selected.buttons) pad(b.label).assertHasClickAction()
    }

    @Test
    fun accessibilityClick_latchesHoldKeys() {
        // A screen reader can't hold a finger down, so its click latches Pan on, then off.
        pad("Pan").a11yClick()
        awaitReport(expected(0 to key("Space")))
        pad("Pan").assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                "On",
            ),
        )
        pad("Pan").a11yClick()
        awaitReleased()
    }

    @Test
    fun accessibilityClick_triggersTheShortcut() {
        pad("Undo").a11yClick()
        awaitReport(expected(Mod.CTRL to key("Z")))
        awaitReleased()
    }
}
