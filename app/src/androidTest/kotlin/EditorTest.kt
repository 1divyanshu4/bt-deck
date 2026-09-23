package dev.keyset

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.keyset.core.model.Mod
import dev.keyset.core.model.PressMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorTest : KeySetUiTest() {

    private fun enterEdit() = rule.onNodeWithContentDescription("Edit layout").performClick()
    private fun exitEdit() = rule.onNodeWithContentDescription("Done editing").performClick()
    private fun openEditor(label: String) {
        enterEdit()
        rule.onNodeWithContentDescription(label).performClick()
        rule.onNodeWithText("Edit button").assertExists()
    }
    private fun labelField() = rule.onNode(hasSetTextAction() and hasText("Label (optional)"))
    private fun searchField() = rule.onNode(hasSetTextAction() and hasText("Search icons"))
    private fun inDialog() = hasAnyAncestor(isDialog())
    private fun role(r: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, r)

    /** A modifier or key chip in the editor. */
    private fun chip(text: String) = rule.onNode(hasText(text) and role(Role.Checkbox) and inDialog())

    /** Save / Cancel / Delete at the bottom of the editor. */
    private fun dialogButton(text: String) = rule.onNode(hasText(text) and role(Role.Button) and inDialog())
    private fun buttons() = store.state.value.selected.buttons

    @Test
    fun addButton_withEveryOption_thenItWorks() {
        enterEdit()
        rule.onNodeWithContentDescription("Add button").performClick()
        rule.onNodeWithText("New button").assertExists()
        labelField().performTextInput("Stamp")
        chip("Alt").performScrollTo().performClick() // Ctrl is preselected
        chip("K").performScrollTo().performClick()
        rule.onNodeWithText("Hold").performScrollTo().performClick()
        searchField().performScrollTo().performTextInput("clone")
        rule.onNodeWithContentDescription("Clone stamp").performScrollTo().performClick()
        dialogButton("Save").performClick()

        val b = buttons().last()
        assertEquals("Stamp", b.label)
        assertEquals(Mod.CTRL or Mod.ALT, b.modifiers)
        assertEquals(key("K"), b.key)
        assertEquals(PressMode.HOLD, b.mode)
        assertEquals("clone", b.icon)

        exitEdit()
        val node = rule.onNodeWithContentDescription("Stamp")
        node.performTouchInput { down(center) }
        awaitReport(expected(Mod.CTRL or Mod.ALT to key("K")))
        node.performTouchInput { up() }
        awaitReleased()
    }

    @Test
    fun save_disabledUntilTheButtonSendsSomething() {
        enterEdit()
        rule.onNodeWithContentDescription("Add button").performClick()
        chip("Ctrl").performScrollTo().performClick() // now nothing selected
        dialogButton("Save").assertIsNotEnabled()
        chip("E").performScrollTo().performClick()
        dialogButton("Save").assertIsEnabled()
    }

    @Test
    fun editLabel() {
        openEditor("Undo")
        labelField().performTextReplacement("Back")
        dialogButton("Save").performClick()
        assertEquals("Back", buttons().first().label)
    }

    @Test
    fun cancel_discardsChanges() {
        openEditor("Undo")
        labelField().performTextReplacement("Nope")
        dialogButton("Cancel").performClick()
        assertEquals("Undo", buttons().first().label)
    }

    @Test
    fun deleteButton() {
        val before = buttons().size
        openEditor("Save")
        dialogButton("Delete").performClick()
        assertEquals(before - 1, buttons().size)
        assertFalse(buttons().any { it.label == "Save" })
    }

    @Test
    fun moveButtonEarlierAndLater() {
        openEditor("Redo")
        rule.onNodeWithText("◀ Earlier").performScrollTo().performClick()
        assertEquals("Redo", buttons()[0].label)
        rule.onNodeWithText("Later ▶").performScrollTo().performClick()
        rule.onNodeWithText("Later ▶").performClick()
        assertEquals("Redo", buttons()[2].label)
    }

    @Test
    fun iconSearch_filtersAndReportsNoMatch() {
        openEditor("Undo")
        searchField().performScrollTo().performTextInput("lasso")
        listOf("Lasso", "Lasso fill", "Lasso erase").forEach {
            rule.onNodeWithContentDescription(it).assertExists()
        }
        rule.onNodeWithContentDescription("Eyedropper").assertDoesNotExist()
        searchField().performTextReplacement("zzzz")
        rule.onNodeWithText("No icons match \"zzzz\"").assertExists()
    }

    @Test
    fun choosingNoIcon_showsTextOnThePad() {
        openEditor("Undo")
        rule.onNode(hasText("None") and inDialog() and SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .performScrollTo().performClick()
        dialogButton("Save").performClick()
        exitEdit()
        rule.onNodeWithText("Ctrl+Z").assertExists()
    }

    @Test
    fun behaviourHelp_explainsAllModes() {
        openEditor("Undo")
        rule.onAllNodes(hasContentDescription("Help") and inDialog())[0].performScrollTo().performClick()
        rule.onNode(hasText("Hold:", substring = true)).assertExists()
    }
}
