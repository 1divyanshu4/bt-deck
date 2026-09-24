package dev.keyset

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.keyset.core.model.Mod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
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

    /** Every key on the main layer: its accessibility name, and the HID key it must send. */
    private val mainKeys = listOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=") +
        listOf("Backspace", "Tab", "[", "]", "\\", "Caps Lock", ";", "'", "Enter", ",", ".", "/") +
        listOf("Space", "Left", "Up", "Down", "Right")

    private val fnKeys = listOf("Esc") + (1..12).map { "F$it" } +
        listOf("Delete", "Insert", "Home", "PgUp", "Pause", "Print Screen", "End", "PgDn", "Scroll Lock") +
        listOf("Menu", "Tab", "Enter", "Backspace")

    private fun assertKeySends(description: String, keyName: String) {
        reports.clear()
        tapKey(description)
        awaitReport(expected(0 to key(keyName)))
        awaitReleased()
    }

    @Test
    fun everyMainKeySendsItsKey() {
        for (c in 'a'..'z') assertKeySends(c.toString(), c.uppercase())
        for (name in mainKeys) assertKeySends(name, name)
    }

    @Test
    fun everyFnKeySendsItsKey() {
        tapKey("Switch keys")
        for (name in fnKeys) assertKeySends(name, name)
    }

    @Test
    fun eachModifierAppliesItsBit() {
        for ((bit, name) in Mod.all) {
            tapKey(name)
            reports.clear()
            tapKey("a")
            awaitReport(expected(bit to key("A")))
            awaitReleased()
        }
    }

    @Test
    fun capsLockTogglesCapitalLetters() {
        tapKey("Caps Lock")
        awaitReport(expected(0 to key("Caps Lock")))
        keyNode("Caps Lock").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "On"))
        rule.onNodeWithText("Q").assertExists()
        rule.onNodeWithText("1").assertExists() // numbers don't follow Caps Lock

        // Shift with Caps Lock on gives small letters, like a real keyboard.
        tapKey("Shift")
        rule.onNodeWithText("q").assertExists()
        tapKey("a") // uses up the Shift

        tapKey("Caps Lock")
        keyNode("Caps Lock").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Off"))
        rule.onNodeWithText("q").assertExists()
    }

    @Test
    fun heldCtrlAppliesWithoutStaying() {
        keyNode("Ctrl").performTouchInput { down(center) }
        // A second finger, while the first stays on Ctrl.
        keyNode("c").performTouchInput {
            down(1, center)
            up(1)
        }
        awaitReport(expected(Mod.CTRL to key("C")))
        keyNode("Ctrl").performTouchInput { up() }
        reports.clear()
        tapKey("v")
        awaitReport(expected(0 to key("V")))
    }

    @Test
    fun switchingLayersReleasesAHeldKey() {
        keyNode("Backspace").performTouchInput { down(center) }
        awaitReport(expected(0 to key("Backspace")))
        keyNode("Switch keys").performSemanticsAction(SemanticsActions.OnClick)
        awaitReleased()
    }

    @Test
    fun keysTurnSidewaysOnATallScreen() {
        val root = rule.onRoot().fetchSemanticsNode().size
        assumeTrue(root.height > root.width)
        val space = keyNode("Space").fetchSemanticsNode().boundsInRoot
        assertTrue("Space bar should run top to bottom", space.height > space.width)
    }

    @Test
    fun screenReaderClickTypesTheKey() {
        keyNode("q").performSemanticsAction(SemanticsActions.OnClick)
        awaitReport(expected(0 to key("Q")))
        awaitReleased()
    }

    @Test
    fun keysWriteInTheBoxWhileItHasFocus() {
        rule.onNode(hasSetTextAction()).performClick()
        tapKey("h")
        tapKey("Shift")
        tapKey("i")
        tapKey("x")
        tapKey("Backspace")
        rule.onNode(hasSetTextAction()).assert(hasText("hI"))
        assertTrue("nothing goes to the other device while writing", reports.isEmpty())

        tapKey("Enter") // sends, and gives the keys back to the other device
        awaitReport(expected(0 to key("H")))
        awaitReport(expected(Mod.SHIFT to key("I")))
        awaitReleased()
        rule.waitUntil(3_000) { rule.onAllNodes(hasText("hI")).fetchSemanticsNodes().isEmpty() }
        reports.clear()
        tapKey("q")
        awaitReport(expected(0 to key("Q")))
    }

    @Test
    fun phoneKeyboardNeverOpens() {
        rule.onNode(hasSetTextAction()).performClick()
        Thread.sleep(1_000) // the phone's keyboard animates in, if it's going to
        val insets = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
        assertFalse(insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false)
    }

    @Test
    fun sentTextKeepsItsCaseWithCapsLockOn() {
        tapKey("Caps Lock")
        rule.onNode(hasSetTextAction()).performTextInput("Hi")
        rule.onNodeWithContentDescription("Send text").performClick()
        awaitReport(expected(0 to key("H")))
        awaitReport(expected(Mod.SHIFT to key("I")))
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
