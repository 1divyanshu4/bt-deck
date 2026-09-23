package dev.keyset.core.designsystem

import dev.keyset.core.data.CustomIcons
import dev.keyset.core.data.defaultLayouts
import dev.keyset.core.designsystem.icon.PadIcons
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PadIconsTest {
    private val all = PadIcons.groups.flatMap { it.second }

    @Test fun keysAreUnique() {
        val dupes = all.groupBy { it.key }.filter { it.value.size > 1 }.keys
        assertTrue("duplicate icon keys: $dupes", dupes.isEmpty())
    }

    @Test fun namesAreUniqueSoSearchResultsAreDistinct() {
        val dupes = all.groupBy { it.name.lowercase() }.filter { it.value.size > 1 }.keys
        assertTrue("duplicate icon names: $dupes", dupes.isEmpty())
    }

    @Test fun keysNeverCollideWithCustomImages() = assertTrue(all.none { CustomIcons.isCustom(it.key) })

    @Test fun defaultLayoutIconsExist() = defaultLayouts().flatMap { it.buttons }.mapNotNull { it.icon }
        .forEach { assertNotNull("missing icon $it", PadIcons.get(it)) }

    @Test fun keysUsedBySavedLayoutsStillExist() {
        // Renaming any of these breaks buttons people already configured.
        listOf(
            "undo", "redo", "save", "copy", "cut", "paste", "select_all", "deselect", "lasso", "transform",
            "move", "crop", "brush", "pen", "pencil", "eraser", "fill", "picker", "palette", "smudge", "blur",
            "text", "shapes", "line", "magic", "pan", "hand", "zoom_in", "zoom_out", "fit", "reset_view",
            "rotate_left", "rotate_right", "flip", "grid", "layers", "new_layer", "clear_layer", "visibility",
            "opacity", "invert", "adjust", "size_down", "size_up", "line_weight", "ctrl", "shift", "alt",
            "meta", "space", "backspace", "star", "bolt", "dot",
        ).forEach { assertNotNull("icon key '$it' was removed", PadIcons.get(it)) }
    }

    @Test fun lookups() {
        assertNull(PadIcons.get(null))
        assertNull(PadIcons.get("nope"))
        assertTrue(CustomIcons.isCustom("img:x.png"))
        assertFalse(CustomIcons.isCustom("brush"))
        assertFalse(CustomIcons.isCustom(null))
    }
}
