package dev.keyset

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutsTest : KeySetUiTest() {

    private fun openMenu() = rule.onNodeWithContentDescription("Layouts", useUnmergedTree = true).performClick()
    private fun nameField() = rule.onNode(hasSetTextAction())
    private fun selected() = store.state.value.selected

    @Test
    fun newEmptyLayout() {
        openMenu()
        rule.onNodeWithText("New empty layout").performClick()
        nameField().performTextInput("Krita")
        rule.onNodeWithText("OK").performClick()
        assertEquals("Krita", selected().name)
        assertEquals(0, selected().buttons.size)
        rule.onNodeWithText("No buttons yet", substring = true).assertExists()
    }

    @Test
    fun duplicateLayout_copiesButtonsWithNewIds() {
        val original = selected()
        openMenu()
        rule.onNodeWithText("Duplicate layout").performClick()
        rule.onNodeWithText("OK").performClick()
        val copy = selected()
        assertEquals("Drawing copy", copy.name)
        assertEquals(original.buttons.map { it.label }, copy.buttons.map { it.label })
        assert(copy.buttons.map { it.id }.intersect(original.buttons.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun renameLayout() {
        openMenu()
        rule.onNodeWithText("Rename layout").performClick()
        nameField().performTextReplacement("Clip Studio")
        rule.onNodeWithText("OK").performClick()
        assertEquals("Clip Studio", selected().name)
        rule.onNodeWithText("Clip Studio").assertExists()
    }

    @Test
    fun emptyName_cannotBeSaved() {
        openMenu()
        rule.onNodeWithText("Rename layout").performClick()
        nameField().performTextReplacement("   ")
        rule.onNodeWithText("OK").assertIsNotEnabled()
    }

    @Test
    fun changeColumns() {
        for (n in 2..6) {
            openMenu()
            rule.onNodeWithText("$n").performClick()
            assertEquals(n, selected().columns)
            rule.onNodeWithText("New empty layout").assertExists() // menu stays open
            androidx.test.espresso.Espresso.pressBack()
        }
    }

    @Test
    fun switchAndDeleteLayouts() {
        rule.runOnUiThread { store.addLayout("Second", copyCurrent = false) }
        openMenu()
        rule.onNode(hasText("Drawing") and hasAnyAncestor(isPopup())).performClick()
        assertEquals("Drawing", selected().name)
        openMenu()
        rule.onNodeWithText("Delete layout").performClick()
        assertEquals(listOf("Second"), store.state.value.layouts.map { it.name })
        openMenu()
        rule.onNodeWithText("Delete layout").assertDoesNotExist() // never delete the last one
    }
}
