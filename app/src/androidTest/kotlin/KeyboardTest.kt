package dev.keyset

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.keyset.core.model.Mod
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardTest : KeySetUiTest() {

    /** The key with this accessibility name (the left one, for keys that appear twice like Shift). */
    private fun keyNode(description: String) = rule.onAllNodesWithContentDescription(description)[0]

    /** A finger tap: press on down, release on up. */
    private fun tapKey(description: String) = keyNode(description).performTouchInput {
        down(center)
        up()
    }

    @Before
    fun openKeyboard() = openTab("Keyboard")

    @Test
    fun letterKeyPressesAndReleases() {
        tapKey("q")
        awaitReport(expected(0 to key("Q")))
        awaitReleased()
    }

    @Test
    fun heldKeyStaysDownUntilLifted() {
        keyNode("Backspace").performTouchInput { down(center) }
        awaitReport(expected(0 to key("Backspace")))
        assertEquals(expected(0 to key("Backspace")), lastReport)
        keyNode("Backspace").performTouchInput { up() }
        awaitReleased()
    }

    @Test
    fun ctrlAppliesToTheNextKeyOnly() {
        tapKey("Ctrl")
        tapKey("z")
        awaitReport(expected(Mod.CTRL to key("Z")))
        reports.clear()
        tapKey("z")
        awaitReport(expected(0 to key("Z")))
    }

    @Test
    fun ctrlTappedTwiceStaysLocked() {
        tapKey("Ctrl")
        tapKey("Ctrl")
        tapKey("c")
        awaitReport(expected(Mod.CTRL to key("C")))
        reports.clear()
        tapKey("v")
        awaitReport(expected(Mod.CTRL to key("V")))
        tapKey("Ctrl") // third tap turns it off
        reports.clear()
        tapKey("x")
        awaitReport(expected(0 to key("X")))
    }

    @Test
    fun shiftShowsShiftedLabels() {
        rule.onNodeWithText("1").assertExists()
        tapKey("Shift")
        rule.onNodeWithText("!").assertExists()
        rule.onNodeWithText("Q").assertExists()
    }

    @Test
    fun fnShowsFunctionKeys() {
        tapKey("Switch keys")
        tapKey("F5")
        awaitReport(expected(0 to key("F5")))
    }

    @Test
    fun screenReaderClickTypesTheKey() {
        keyNode("q").performSemanticsAction(SemanticsActions.OnClick)
        awaitReport(expected(0 to key("Q")))
        awaitReleased()
    }

    @Test
    fun sendTextTypesEachCharacter() {
        rule.onNode(hasSetTextAction()).performTextInput("Hi!")
        rule.onNodeWithContentDescription("Send text").performClick()
        awaitReport(expected(Mod.SHIFT to key("H")))
        awaitReport(expected(0 to key("I")))
        awaitReport(expected(Mod.SHIFT to key("1")))
        awaitReleased()
    }
}
