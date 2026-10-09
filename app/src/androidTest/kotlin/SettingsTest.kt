package dev.btdeck

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.btdeck.core.model.ButtonSpacing
import dev.btdeck.core.model.ButtonStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTest : ZexDeckUiTest() {

    @Test
    fun iconOnly_hidesLabelsButKeepsNamesForScreenReaders() {
        rule.onNodeWithText("Undo").assertExists() // label under the icon by default
        setSettings { it.copy(buttonStyle = ButtonStyle.ICON_ONLY) }
        rule.onNodeWithText("Undo").assertDoesNotExist()
        rule.onNodeWithContentDescription("Undo").assertExists()
    }

    @Test
    fun shortcutBadge_followsTheSetting() {
        // Default style has icons, and the shortcut still shows as a badge.
        rule.onNodeWithText("Ctrl + Z", useUnmergedTree = true).assertExists()
        setSettings { it.copy(showShortcut = false) }
        rule.onNodeWithText("Ctrl + Z", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun textOnly_showsLabelAndShortcut() {
        setSettings { it.copy(buttonStyle = ButtonStyle.TEXT_ONLY) }
        rule.onNodeWithText("Ctrl + Z", useUnmergedTree = true).assertExists()
        setSettings { it.copy(showShortcut = false) }
        rule.onNodeWithText("Ctrl + Z", useUnmergedTree = true).assertDoesNotExist()
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
    fun settingsTab_alwaysOpensTheMainPage() {
        openTab("Settings")
        rule.onNodeWithText("Tutorials").performScrollTo().performClick()
        openTab("Pad")
        openTab("Settings")
        rule.onNodeWithText("Vibrate on press").assertExists()
        rule.onNodeWithText("Tutorials").performScrollTo().performClick()
        openTab("Settings") // tapped again while on a Settings page
        rule.onNodeWithText("Vibrate on press").assertExists()
    }

    @Test
    fun aboutPagesOpenAndGoBack() {
        openTab("Settings")
        rule.onNodeWithText("Tutorials").performScrollTo().performClick()
        // A tutorial opens on its first step; Next moves on, Back closes it.
        rule.onNodeWithText("Connect a device").performClick()
        rule.onNodeWithText("Tap the status to open Connect.").assertExists()
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithText("Open Bluetooth on your tablet or laptop, then tap Scan and Pair.").assertExists()
        rule.onNodeWithContentDescription("Back").performClick() // tutorial → list
        rule.onNodeWithText("Share a layout").assertExists()
        rule.onNodeWithContentDescription("Back").performClick() // Tutorials → Settings
        rule.onNodeWithText("Privacy policy").performScrollTo().performClick()
        rule.onNodeWithText("Bluetooth").assertExists()
        rule.onNodeWithContentDescription("Back").performClick() // Privacy → Settings
        rule.onNodeWithText("About ZexDeck").performScrollTo().performClick()
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
