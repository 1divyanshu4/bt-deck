package dev.btdeck.core.data

import dev.btdeck.core.model.Mod
import dev.btdeck.core.model.PadButton
import dev.btdeck.core.model.PadLayout
import dev.btdeck.core.model.PressMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutArchiveTest {
    private val png = byteArrayOf(1, 2, 3)
    private val layout = PadLayout(
        name = "Krita",
        columns = 4,
        buttons = listOf(
            PadButton(label = "Undo", modifiers = Mod.CTRL, key = 0x1D, icon = "undo"),
            PadButton(label = "Stamp", modifiers = 0, key = 0x0E, mode = PressMode.HOLD, icon = "img:abc.png"),
        ),
    )

    private fun archive(layout: PadLayout, images: Map<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { LayoutArchive.write(layout, images, it) }.toByteArray()

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z ->
            for ((name, bytes) in entries) {
                z.putNextEntry(ZipEntry(name))
                z.write(bytes)
                z.closeEntry()
            }
        }
    }.toByteArray()

    private fun read(bytes: ByteArray) = LayoutArchive.read(ByteArrayInputStream(bytes))

    private fun layoutJson(format: String = "keyset-layout", version: Int = 1) =
        """{"format":"$format","version":$version,"layout":{"id":"x","name":"L","buttons":[]}}""".toByteArray()

    @Test fun roundTripKeepsLayoutAndImages() {
        val back = read(archive(layout, mapOf("abc.png" to png, "unused.png" to png)))
        assertEquals(layout, back.layout)
        assertEquals(setOf("abc.png"), back.images.keys) // unused images aren't written
        assertArrayEquals(png, back.images["abc.png"])
    }

    @Test fun missingImage_buttonFallsBackToText() {
        val back = read(archive(layout, images = emptyMap()))
        assertNull(back.layout.buttons[1].icon)
        assertEquals("undo", back.layout.buttons[0].icon)
    }

    @Test fun notAZip_isRejected() {
        assertThrows(LayoutArchive.InvalidFileException::class.java) { read("hello".toByteArray()) }
    }

    @Test fun zipWithoutLayout_isRejected() {
        assertThrows(LayoutArchive.InvalidFileException::class.java) { read(zip("other.txt" to png)) }
    }

    @Test fun wrongFormat_isRejected() {
        assertThrows(LayoutArchive.InvalidFileException::class.java) {
            read(zip("layout.json" to layoutJson(format = "something-else")))
        }
    }

    @Test fun newerVersion_asksToUpdate() {
        val e = assertThrows(LayoutArchive.InvalidFileException::class.java) {
            read(zip("layout.json" to layoutJson(version = 99)))
        }
        assertTrue(e.message!!.contains("newer"))
    }

    @Test fun pathTraversalNames_areIgnored() {
        val back = read(
            zip(
                "layout.json" to layoutJson(),
                "images/../../evil.png" to png,
                "images/sub/dir.png" to png,
                "../layout.json" to png,
            ),
        )
        assertTrue(back.images.isEmpty())
    }

    @Test fun oversizedEntry_isRejected() {
        val huge = ByteArray(3 * 1024 * 1024)
        assertThrows(LayoutArchive.InvalidFileException::class.java) {
            read(zip("layout.json" to layoutJson(), "images/big.png" to huge))
        }
    }
}
