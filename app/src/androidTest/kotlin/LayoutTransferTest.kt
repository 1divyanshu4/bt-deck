package dev.keyset

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.keyset.core.data.CustomIcons
import dev.keyset.core.data.LayoutTransfer
import dev.keyset.core.model.PadButton
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutTransferTest : KeySetUiTest() {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val transfer get() = (context.applicationContext as KeySetApp).transfer

    /** Adds a button with a freshly imported custom image to the current layout. */
    private fun addCustomImageButton(): String {
        val png = ByteArrayOutputStream().also {
            Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        val key = runBlocking { CustomIcons.importBytes(png) }!!
        rule.runOnUiThread {
            store.upsertButton(PadButton(label = "Stamp", modifiers = 0, key = key("S"), icon = key))
        }
        return key
    }

    @Test
    fun shareThenImport_bringsTheLayoutAndItsImageBack() {
        val originalKey = addCustomImageButton()
        val file = runBlocking { transfer.shareFile(store.state.value.selected) }
        val contents = runBlocking { transfer.read(android.net.Uri.fromFile(file)) }
        val name = runBlocking { transfer.import(contents) }

        assertEquals("Drawing (imported)", name)
        val imported = store.state.value.selected.buttons.first { it.label == "Stamp" }
        assertNotNull(imported.icon)
        assertNotEquals(originalKey, imported.icon) // stored again under a new name
        assertNotNull(CustomIcons.readBytes(imported.icon!!))
    }

    @Test
    fun openingAFileFromAnotherApp_asksThenImports() {
        val file = runBlocking { transfer.shareFile(store.state.value.selected) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        rule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .setClass(activity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Import \"Drawing\"?")).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasText("Import") and hasAnyAncestor(isDialog())).performClick()
        rule.waitUntil(5_000) { store.state.value.layouts.size == 2 }
        assertEquals("Drawing (imported)", store.state.value.selected.name)
    }

    @Test
    fun layoutsScreenOffersImportShareAndSave() {
        openTab("Layouts")
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Import").assertExists()
        // A click action: the screen menu is still open on top.
        rule.onNodeWithContentDescription("Options for Drawing").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Share").assertExists()
        rule.onNodeWithText("Save to file").assertExists()
    }

    @Test
    fun fileNamesAreSafe() {
        val layout = store.state.value.selected.copy(name = "Krita / clip: v2?")
        assertEquals("Krita _ clip_ v2_.keyset", LayoutTransfer.fileName(layout))
    }
}
