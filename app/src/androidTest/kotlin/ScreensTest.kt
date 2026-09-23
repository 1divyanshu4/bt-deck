package dev.keyset

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.bluetooth.HidStatus
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreensTest : KeySetUiTest() {

    private fun status(s: HidStatus) {
        rule.runOnUiThread { HidKeyboard.setStatusForTest(s) }
        rule.waitForIdle()
    }

    @Test
    fun fullscreen_hidesChromeAndBackExits() {
        rule.onNodeWithContentDescription("Fullscreen").performClick()
        rule.onNodeWithContentDescription("Edit layout").assertDoesNotExist()
        rule.onNodeWithText("Settings").assertDoesNotExist() // bottom bar hidden
        rule.onNodeWithContentDescription("Undo").assertExists()
        Espresso.pressBack()
        rule.onNodeWithContentDescription("Edit layout").assertExists()
    }

    @Test
    fun fullscreen_showsStripOnlyWhenDisconnected() {
        rule.onNodeWithContentDescription("Fullscreen").performClick()
        status(HidStatus.Ready)
        rule.onNodeWithText("Not connected").assertExists()
        status(HidStatus.Connected("Tab"))
        rule.onNodeWithText("Not connected").assertDoesNotExist()
        status(HidStatus.Ready)
        rule.onNodeWithText("Exit fullscreen").performClick()
        rule.onNodeWithContentDescription("Edit layout").assertExists()
    }

    @Test
    fun everyStatusBannerHasTheRightAction() {
        val cases = listOf(
            HidStatus.BluetoothOff to ("Bluetooth is off" to "Turn on"),
            HidStatus.NeedsPermission to ("Permission needed" to "Allow"),
            HidStatus.Unsupported("Keyboard mode is busy", "detail") to ("Keyboard mode is busy" to "Retry"),
            HidStatus.ConnectFailed("Tab") to ("Connect from Tab once" to "Connect"),
            HidStatus.Stopped to ("Keyboard stopped" to "Start"),
        )
        for ((s, texts) in cases) {
            status(s)
            rule.onNodeWithText(texts.first).assertExists()
            rule.onNodeWithText(texts.second).assertExists()
        }
        status(HidStatus.Connected("Tab"))
        rule.onNodeWithText("Not connected").assertDoesNotExist()
        rule.onNodeWithText("Connected to Tab").assertExists() // status chip
    }

    @Test
    fun failureBanner_helpExplainsWhatToDo() {
        status(HidStatus.ConnectFailed("Tab"))
        rule.onNodeWithContentDescription("Help").performClick()
        rule.onNode(hasText("Connect once from Tab's side", substring = true)).assertExists()
    }

    @Test
    fun connectSheet_hasEveryControl() {
        rule.onNodeWithText("Not connected").performClick()
        rule.onNodeWithText("Connect a device").assertExists()
        rule.onNodeWithContentDescription("Refresh paired devices").assertExists()
        rule.onNodeWithText("Scan").assertExists()
        rule.onNodeWithText("Make phone discoverable").assertExists()
        rule.onAllNodesWithContentDescription("Help")[0].performClick()
        rule.onNode(hasText("Works with tablets and laptops", substring = true)).assertExists()
    }

    @Test
    fun statusChip_opensTheSheet() {
        status(HidStatus.Ready)
        rule.onNodeWithText("Not connected").performClick() // the status chip
        rule.onNodeWithText("Connect a device").assertExists()
    }

    @Test
    fun backFromAnotherTabReturnsToThePad() {
        openTab("Settings")
        Espresso.pressBack()
        rule.onNodeWithContentDescription("Edit layout").assertExists()
    }

    @Test
    fun keyboardTab_canOpenTheConnectSheet() {
        openTab("Keyboard")
        rule.onNodeWithText("Not connected").performClick()
        rule.onNodeWithText("Connect a device").assertExists()
    }

    // ---- accessibility (ATF: labels, touch target size, contrast) ----

    @Test
    fun accessibility_layouts() {
        openTab("Layouts")
        rule.enableAccessibilityChecks()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun accessibility_settings() {
        openTab("Settings")
        rule.enableAccessibilityChecks()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun accessibility_help() {
        openTab("Settings")
        rule.onNodeWithText("About KeySet").performScrollTo().performClick()
        rule.onNodeWithText("Connection help").performClick()
        rule.enableAccessibilityChecks()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun accessibility_mainScreen() {
        rule.enableAccessibilityChecks()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun accessibility_editMode() {
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Edit layout").performClick()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun accessibility_buttonEditor() {
        rule.onNodeWithContentDescription("Edit layout").performClick()
        rule.onNodeWithContentDescription("Undo").performClick()
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("Edit button").tryPerformAccessibilityChecks()
    }

    @Test
    fun accessibility_connectSheet() {
        rule.onNodeWithText("Not connected").performClick()
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("Connect a device").tryPerformAccessibilityChecks()
    }

    @Test
    fun accessibility_fullscreen() {
        rule.onNodeWithContentDescription("Fullscreen").performClick()
        rule.enableAccessibilityChecks()
        rule.onRoot().tryPerformAccessibilityChecks()
    }
}
