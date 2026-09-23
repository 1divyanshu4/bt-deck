package dev.keyset.core.data

import dev.keyset.core.model.Keys
import dev.keyset.core.model.Mod
import dev.keyset.core.model.PadButton
import dev.keyset.core.model.PadLayout
import dev.keyset.core.model.PadState
import dev.keyset.core.model.PressMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutStoreTest {
    @Test fun defaultsRoundTrip() {
        val s = defaultState()
        assertEquals(s, decodeState(encodeState(s)))
    }

    @Test fun everyButtonFieldRoundTrips() {
        val b = PadButton(
            label = "Stamp \"quoted\" ✓",
            modifiers = Mod.CTRL or Mod.ALT,
            key = 0x0E,
            mode = PressMode.TOGGLE,
            color = 5,
            icon = "img:abc.png",
            tintImage = true,
        )
        val s = PadState(listOf(PadLayout(name = "Krita", columns = 5, buttons = listOf(b))), selectedId = "x")
        val decoded = decodeState(encodeState(s))!!
        assertEquals(b, decoded.layouts.single().buttons.single())
        assertEquals(5, decoded.layouts.single().columns)
    }

    @Test fun multipleLayoutsKeepSelection() {
        val a = PadLayout(name = "A", buttons = emptyList())
        val b = PadLayout(name = "B", buttons = emptyList())
        assertEquals(b.id, decodeState(encodeState(PadState(listOf(a, b), b.id)))!!.selectedId)
    }

    @Test fun missingSelection_fallsBackToFirst() {
        val a = PadLayout(name = "A", buttons = emptyList())
        assertEquals(a.id, decodeState(encodeState(PadState(listOf(a), "gone")))!!.selectedId)
    }

    @Test fun garbage_isNull() {
        assertNull(decodeState("not json"))
        assertNull(decodeState("{}"))
        assertNull(decodeState("""{"layouts":[]}"""))
    }

    @Test fun olderVersionJson_withoutIconOrTint() {
        // Saved by 0.1 before icons existed.
        val raw = """{"selected":"L","layouts":[{"id":"L","name":"Old","columns":3,"buttons":[
            {"id":"b","label":"Undo","mods":1,"key":29,"mode":"TAP","color":1}]}]}"""
        val b = decodeState(raw)!!.selected.buttons.single()
        assertNull(b.icon)
        assertEquals(false, b.tintImage)
        assertEquals(Mod.CTRL, b.modifiers)
    }

    @Test fun unknownMode_becomesTap() {
        val raw = """{"layouts":[{"id":"L","name":"X","buttons":[
            {"id":"b","label":"","mods":1,"key":4,"mode":"LONGPRESS"}]}]}"""
        assertEquals(PressMode.TAP, decodeState(raw)!!.selected.buttons.single().mode)
    }

    @Test fun defaultLayoutIsSane() {
        val buttons = defaultLayouts().flatMap { it.buttons }
        assertTrue(buttons.isNotEmpty())
        assertEquals(buttons.size, buttons.map { it.id }.toSet().size)
        buttons.forEach { b ->
            assertTrue("${b.label} sends nothing", b.modifiers != 0 || b.key != 0)
            assertTrue("${b.label} key not in table", b.key == 0 || Keys.all.any { it.usage == b.key })
            assertTrue("${b.label} color out of range", b.color in 0..5)
        }
    }
}
