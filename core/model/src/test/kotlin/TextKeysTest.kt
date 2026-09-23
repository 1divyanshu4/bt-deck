package dev.keyset.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextKeysTest {
    @Test fun lettersAndCase() {
        assertEquals(0 to 0x04, keystrokeFor('a'))
        assertEquals(Mod.SHIFT to 0x1D, keystrokeFor('Z'))
    }

    @Test fun digits() {
        assertEquals(0 to 0x1E, keystrokeFor('1'))
        assertEquals(0 to 0x27, keystrokeFor('0'))
    }

    @Test fun symbolsUseTheirUsKeys() {
        assertEquals(Mod.SHIFT to 0x1E, keystrokeFor('!'))
        assertEquals(Mod.SHIFT to 0x34, keystrokeFor('"'))
        assertEquals(0 to 0x34, keystrokeFor('\''))
        assertEquals(Mod.SHIFT to 0x38, keystrokeFor('?'))
    }

    @Test fun whitespace() {
        assertEquals(0 to 0x2C, keystrokeFor(' '))
        assertEquals(0 to 0x28, keystrokeFor('\n'))
        assertEquals(0 to 0x2B, keystrokeFor('\t'))
    }

    @Test fun everyPrintableAsciiCharacterHasAKey() {
        (' '..'~').forEach { c -> assert(keystrokeFor(c) != null) { "no key for '$c'" } }
    }

    @Test fun nonAsciiHasNoKey() {
        assertNull(keystrokeFor('é'))
        assertNull(keystrokeFor('€'))
    }
}
