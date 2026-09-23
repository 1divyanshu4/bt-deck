package dev.keyset

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutsTest : KeySetUiTest() {

    private fun layouts() = store.state.value.layouts
    private fun selected() = store.state.value.selected
    private fun nameField() = rule.onNode(hasSetTextAction())
    private fun menuFor(name: String) = rule.onNodeWithContentDescription("Options for $name").performClick()
    private fun dialogButton(text: String) = rule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    // The floating button's label lives on a child node, not the merged button node.
    private fun newLayoutButton() = rule.onNodeWithText("New layout", useUnmergedTree = true)

    @Before
    fun openLayouts() = openTab("Layouts")

    @Test
    fun tappingACardOpensItOnThePad() {
        rule.runOnUiThread { store.createLayout("Second") }
        openTab("Layouts")
        rule.onNodeWithText("Drawing").performClick()
        assertEquals("Drawing", selected().name)
        rule.onNodeWithContentDescription("Edit layout").assertExists() // back on the pad
    }

    @Test
    fun newLayout() {
        newLayoutButton().performClick()
        nameField().performTextInput("Krita")
        dialogButton("OK").performClick()
        assertEquals("Krita", selected().name)
        assertEquals(0, selected().buttons.size)
    }

    @Test
    fun renameLayout() {
        menuFor("Drawing")
        rule.onNodeWithText("Rename").performClick()
        nameField().performTextReplacement("Clip Studio")
        dialogButton("OK").performClick()
        assertEquals("Clip Studio", selected().name)
    }

    @Test
    fun emptyName_cannotBeSaved() {
        menuFor("Drawing")
        rule.onNodeWithText("Rename").performClick()
        nameField().performTextReplacement("   ")
        dialogButton("OK").assertIsNotEnabled()
    }

    @Test
    fun duplicateLayout_copiesButtonsWithNewIds() {
        val original = selected()
        menuFor("Drawing")
        rule.onNodeWithText("Duplicate").performClick()
        dialogButton("OK").performClick()
        val copy = selected()
        assertEquals("Drawing copy", copy.name)
        assertEquals(original.buttons.map { it.label }, copy.buttons.map { it.label })
        assertTrue(copy.buttons.map { it.id }.intersect(original.buttons.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun changeColumns() {
        menuFor("Drawing")
        rule.onNodeWithText("5").performClick()
        assertEquals(5, selected().columns)
    }

    @Test
    fun restoreDefaults_addsTheBuiltInLayoutWithIcons_andKeepsYours() {
        rule.runOnUiThread { store.updateSelected { it.copy(buttons = emptyList()) } }
        rule.onNodeWithText("Restore defaults").performClick()
        dialogButton("Restore").performClick()
        assertEquals(listOf("Drawing", "Drawing (default)"), layouts().map { it.name })
        assertEquals("Drawing (default)", selected().name)
        assertTrue(selected().buttons.any { it.icon != null })
        assertTrue(layouts().first().buttons.isEmpty()) // the user's layout is untouched
    }

    @Test
    fun deleteAsksFirst_andNeverDeletesTheLastLayout() {
        rule.runOnUiThread { store.createLayout("Second") }
        menuFor("Second")
        rule.onNodeWithText("Delete").performClick()
        dialogButton("Delete").performClick()
        assertEquals(listOf("Drawing"), layouts().map { it.name })
        menuFor("Drawing")
        rule.onNodeWithText("Delete").assertDoesNotExist()
    }
}
