package dev.keyset

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.keyset.core.model.ButtonSpacing
import dev.keyset.core.model.ButtonStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTest : KeySetUiTest() {

    @Test
    fun iconOnly_hidesLabelsButKeepsNamesForScreenReaders() {
        rule.onNodeWithText("Undo").assertExists() // label under the icon by default
        setSettings { it.copy(buttonStyle = ButtonStyle.ICON_ONLY) }
        rule.onNodeWithText("Undo").assertDoesNotExist()
        rule.onNodeWithContentDescription("Undo").assertExists()
    }

    @Test
    fun textOnly_showsLabelAndShortcut() {
        setSettings { it.copy(buttonStyle = ButtonStyle.TEXT_ONLY) }
        rule.onNodeWithText("Ctrl+Z", useUnmergedTree = true).assertExists()
        setSettings { it.copy(showShortcut = false) }
        rule.onNodeWithText("Ctrl+Z", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun changingSettingsOnTheScreenSavesThem() {
        openTab("Settings")
        rule.onNodeWithText("Show on buttons").performClick()
        rule.onNodeWithText("Icon only").performClick()
        rule.onNodeWithText("Spacing").performClick()
        rule.onNodeWithText("Roomy").performClick()
        rule.onNodeWithText("Vibrate on press").performClick()
        rule.waitUntil(3_000) { !settingsStore.settings.value.vibrate }
        val saved = settingsStore.settings.value
        assertEquals(ButtonStyle.ICON_ONLY, saved.buttonStyle)
        assertEquals(ButtonSpacing.ROOMY, saved.spacing)
        assertFalse(saved.vibrate)
    }

    @Test
    fun aboutPagesOpenAndGoBack() {
        openTab("Settings")
        rule.onNodeWithText("About KeySet").performScrollTo().performClick()
        rule.onNodeWithText("Connection help").performClick()
        rule.onNodeWithText("First time connecting").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Open-source licenses").performClick()
        // The list loads in the background; several entries mention Compose.
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Compose", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Back").performClick() // licenses → About
        rule.onNodeWithContentDescription("Back").performClick() // About → Settings
        rule.onNodeWithText("Vibrate on press").assertExists()
    }
}
