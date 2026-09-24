package dev.btdeck.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeysTest {
    @Test fun usagesAreUnique() {
        val dupes = Keys.all.groupBy { it.usage }.filter { it.value.size > 1 }
        assertTrue("duplicate usages: $dupes", dupes.isEmpty())
    }

    @Test fun namesAreUnique() {
        val dupes = Keys.all.groupBy { it.name }.filter { it.value.size > 1 }.keys
        assertTrue("duplicate names: $dupes", dupes.isEmpty())
    }

    @Test fun everyKeyFitsTheReportDescriptor() {
        // The descriptor declares key usages 0..0x65; anything above is dropped by hosts.
        val outOfRange = Keys.all.filter { it.usage !in 0..0x65 }
        assertTrue("outside descriptor range: $outOfRange", outOfRange.isEmpty())
    }

    @Test fun lettersDigitsAndFunctionKeysPresent() {
        val names = Keys.all.map { it.name }.toSet()
        (('A'..'Z') + ('0'..'9')).forEach { assertTrue("$it missing", it.toString() in names) }
        (1..12).forEach { assertTrue("F$it missing", "F$it" in names) }
    }

    @Test fun standardUsages() {
        fun u(n: String) = Keys.all.first { it.name == n }.usage
        assertEquals(0x04, u("A"))
        assertEquals(0x1D, u("Z"))
        assertEquals(0x27, u("0"))
        assertEquals(0x2C, u("Space"))
        assertEquals(0x3A, u("F1"))
        assertEquals(0x45, u("F12"))
        assertEquals(0x2F, u("["))
    }

    @Test fun comboLabels() {
        fun u(n: String) = Keys.all.first { it.name == n }.usage
        assertEquals("Ctrl+Shift+Z", comboLabel(Mod.CTRL or Mod.SHIFT, u("Z")))
        assertEquals("Ctrl+Shift+Alt+Meta", comboLabel(Mod.META or Mod.ALT or Mod.SHIFT or Mod.CTRL, 0))
        assertEquals("Space", comboLabel(0, u("Space")))
        assertEquals("—", comboLabel(0, 0))
    }

    @Test fun unknownUsageShownAsHex() = assertEquals("0x7F", Keys.name(0x7F))
}
