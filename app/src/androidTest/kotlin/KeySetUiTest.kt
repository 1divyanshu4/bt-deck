package dev.keyset

import android.Manifest
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.bluetooth.HidStatus
import dev.keyset.core.data.LayoutStore
import dev.keyset.core.model.Keys
import dev.keyset.core.model.buildReport
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Shared setup: Bluetooth permissions granted, layouts reset to the defaults, and every HID
 * report the app builds is recorded in [reports] (test mode never sends them anywhere).
 */
abstract class KeySetUiTest {
    @get:Rule(order = 0)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        *buildList {
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray(),
    )

    @get:Rule(order = 1)
    val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    protected val reports = CopyOnWriteArrayList<ByteArray>()
    protected val store: LayoutStore
        get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as KeySetApp).layouts

    @Before
    fun baseSetUp() {
        rule.runOnUiThread {
            store.resetToDefaults()
            HidKeyboard.releaseAll()
            HidKeyboard.setStatusForTest(HidStatus.Ready)
        }
        rule.waitForIdle()
        reports.clear()
        HidKeyboard.reportListener = { reports += it }
    }

    @After
    fun baseTearDown() {
        HidKeyboard.reportListener = null
        rule.runOnUiThread { HidKeyboard.releaseAll() }
    }

    protected fun key(name: String) = Keys.all.first { it.name == name }.usage

    protected fun expected(vararg held: Pair<Int, Int>) = buildReport(held.toList()).toList()

    protected val lastReport: List<Byte>? get() = reports.lastOrNull()?.toList()

    protected val released = ByteArray(8).toList()

    /** Waits for a report equal to [want] to have been built. */
    protected fun awaitReport(want: List<Byte>, timeoutMs: Long = 3_000) =
        rule.waitUntil(timeoutMs) { reports.any { it.toList() == want } }

    /** Waits until everything is released again. */
    protected fun awaitReleased(timeoutMs: Long = 3_000) = rule.waitUntil(timeoutMs) { lastReport == released }

    protected fun SemanticsNodeInteraction.centerInRoot(): Offset = fetchSemanticsNode().boundsInRoot.center
}
