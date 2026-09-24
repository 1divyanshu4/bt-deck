package dev.btdeck.core.data

import dev.btdeck.core.model.PadLayout
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject

/**
 * The `.keyset` file format: a zip with
 *
 * - `layout.json`: `{"format": "keyset-layout", "version": 1, "layout": {…}}`
 * - `images/<name>.png`: each custom image the layout's buttons use (icon key `img:<name>.png`)
 *
 * Reading treats the file as untrusted. Entry names are never used as file paths (the classic
 * "zip path traversal" attack), only the two expected kinds of entry are read, and every entry
 * has a size limit so a small file can't unpack into gigabytes.
 */
object LayoutArchive {
    const val EXTENSION = "keyset"

    private const val FORMAT = "keyset-layout"
    private const val VERSION = 1
    private const val LAYOUT_ENTRY = "layout.json"
    private const val IMAGES_DIR = "images/"
    private val IMAGE_NAME = Regex("[A-Za-z0-9_-]{1,64}\\.png")

    private const val MAX_JSON_BYTES = 512 * 1024
    private const val MAX_IMAGE_BYTES = 2 * 1024 * 1024
    private const val MAX_IMAGES = 64

    /** A layout and the custom images it uses, keyed by image file name. */
    data class Contents(val layout: PadLayout, val images: Map<String, ByteArray>)

    class InvalidFileException(message: String) : IOException(message)

    /** Writes [layout] and [images] (only the ones its buttons use) as a `.keyset` file. */
    fun write(layout: PadLayout, images: Map<String, ByteArray>, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            val json = JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("layout", layout.toJson())
            zip.putNextEntry(ZipEntry(LAYOUT_ENTRY))
            zip.write(json.toString(2).toByteArray())
            zip.closeEntry()
            for (name in imageNamesUsedBy(layout)) {
                val bytes = images[name] ?: continue
                zip.putNextEntry(ZipEntry(IMAGES_DIR + name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    /** Reads a `.keyset` file. Throws [InvalidFileException] if it isn't one. */
    fun read(input: InputStream): Contents {
        var layout: PadLayout? = null
        val images = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                when {
                    name == LAYOUT_ENTRY -> layout = parseLayout(zip.readLimited(MAX_JSON_BYTES))
                    name.startsWith(IMAGES_DIR) -> {
                        val imageName = name.removePrefix(IMAGES_DIR)
                        // Anything that isn't a plain file name is ignored, never used as a path.
                        if (IMAGE_NAME.matches(imageName) && images.size < MAX_IMAGES) {
                            images[imageName] = zip.readLimited(MAX_IMAGE_BYTES)
                        }
                    }
                }
            }
        }
        val found = layout ?: throw InvalidFileException("This isn't a BTDeck layout file.")
        // Drop references to images the file doesn't contain; those buttons show their text.
        val cleaned = found.copy(
            buttons = found.buttons.map { button ->
                val image = button.icon?.let(::imageName)
                if (image != null && image !in images) button.copy(icon = null) else button
            },
        )
        return Contents(cleaned, images.filterKeys { it in imageNamesUsedBy(cleaned) })
    }

    /** File names of the custom images [layout] uses. */
    fun imageNamesUsedBy(layout: PadLayout): Set<String> =
        layout.buttons.mapNotNull { it.icon?.let(::imageName) }.toSet()

    private fun imageName(iconKey: String): String? =
        if (CustomIcons.isCustom(iconKey)) CustomIcons.nameOf(iconKey) else null

    private fun parseLayout(bytes: ByteArray): PadLayout {
        val json = runCatching { JSONObject(String(bytes)) }.getOrNull()
            ?: throw InvalidFileException("This BTDeck file is damaged.")
        if (json.optString("format") != FORMAT) throw InvalidFileException("This isn't a BTDeck layout file.")
        if (json.optInt("version") > VERSION) {
            throw InvalidFileException("This layout was made with a newer BTDeck. Update the app to import it.")
        }
        return runCatching { json.getJSONObject("layout").toLayout() }.getOrNull()
            ?: throw InvalidFileException("This BTDeck file is damaged.")
    }

    /** Reads the current entry, refusing to go past [limit] bytes. */
    private fun ZipInputStream.readLimited(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            if (out.size() + n > limit) throw InvalidFileException("This BTDeck file is too large.")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
