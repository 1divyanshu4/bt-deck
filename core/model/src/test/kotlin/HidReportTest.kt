package dev.keyset.core.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HidReportTest {
    private fun key(name: String) = Keys.all.first { it.name == name }.usage
    private fun report(mods: Int, vararg keys: Int) = ByteArray(8).also { r ->
        r[0] = mods.toByte()
        keys.forEachIndexed { i, k -> r[2 + i] = k.toByte() }
    }

    @Test fun nothingHeld_isAllZeros() = assertArrayEquals(ByteArray(8), buildReport(emptyList()))

    @Test fun singleShortcut() = assertArrayEquals(
        report(Mod.CTRL, key("Z")),
        buildReport(listOf(Mod.CTRL to key("Z"))),
    )

    @Test fun modifierOnly_hasNoKeys() = assertArrayEquals(report(Mod.SHIFT), buildReport(listOf(Mod.SHIFT to 0)))

    @Test fun allModifierBits() = assertArrayEquals(
        report(Mod.CTRL or Mod.SHIFT or Mod.ALT or Mod.META, key("K")),
        buildReport(listOf(Mod.CTRL or Mod.SHIFT or Mod.ALT or Mod.META to key("K"))),
    )

    @Test fun heldKeysCombine_panWhileUndo() = // Hold Space (pan) and tap Ctrl+Z: both must be in one report.
        assertArrayEquals(
            report(Mod.CTRL, key("Space"), key("Z")),
            buildReport(listOf(0 to key("Space"), Mod.CTRL to key("Z"))),
        )

    @Test fun modifiersFromSeparateButtonsAreOred() = assertArrayEquals(
        report(Mod.CTRL or Mod.SHIFT, key("A")),
        buildReport(listOf(Mod.CTRL to 0, Mod.SHIFT to 0, 0 to key("A"))),
    )

    @Test fun sameKeyFromTwoButtons_sentOnce() = assertArrayEquals(
        report(Mod.CTRL, key("Z")),
        buildReport(listOf(Mod.CTRL to key("Z"), 0 to key("Z"))),
    )

    @Test fun moreThanSixKeys_keepsFirstSixInPressOrder() {
        val names = listOf("A", "B", "C", "D", "E", "F", "G")
        val r = buildReport(names.map { 0 to key(it) })
        assertEquals(names.take(MAX_KEYS).map { key(it).toByte() }, r.drop(2))
    }

    @Test fun reservedByteIsZero() = assertEquals(0, buildReport(listOf(Mod.ALT to key("F12")))[1].toInt())
}
