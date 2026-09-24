package dev.btdeck.core.data

import android.content.Context
import android.net.Uri
import dev.btdeck.core.model.PadLayout
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Moves layouts in and out of the app as `.keyset` files. */
class LayoutTransfer(context: Context, private val store: LayoutStore) {
    private val appContext = context.applicationContext

    /** Writes [layout] to [uri] (a file the user picked with "Save to file"). */
    suspend fun export(layout: PadLayout, uri: Uri) = withContext(Dispatchers.IO) {
        val out = appContext.contentResolver.openOutputStream(uri) ?: error("Can't write that file")
        out.use { LayoutArchive.write(layout, imagesOf(layout), it) }
    }

    /** Writes [layout] to a temporary file for sharing, and returns it. */
    suspend fun shareFile(layout: PadLayout): File = withContext(Dispatchers.IO) {
        val dir = File(appContext.cacheDir, SHARE_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // only the latest share is kept
        val file = File(dir, fileName(layout))
        file.outputStream().use { LayoutArchive.write(layout, imagesOf(layout), it) }
        file
    }

    /** Reads a `.keyset` file without importing it, to show what's inside first. */
    suspend fun read(uri: Uri): LayoutArchive.Contents = withContext(Dispatchers.IO) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw LayoutArchive.InvalidFileException("Couldn't open that file.")
        input.use { LayoutArchive.read(it) }
    }

    /**
     * Adds [contents] as a new layout: its images are stored under new names and the buttons
     * pointed at them. Returns the layout's final name.
     */
    suspend fun import(contents: LayoutArchive.Contents): String {
        val newKeys = contents.images.mapValues { (_, bytes) -> CustomIcons.importBytes(bytes) }
        val buttons = contents.layout.buttons.map { button ->
            val icon = button.icon
            if (icon != null &&
                CustomIcons.isCustom(icon)
            ) {
                button.copy(icon = newKeys[CustomIcons.nameOf(icon)])
            } else {
                button
            }
        }
        withContext(Dispatchers.Main) { store.addImported(contents.layout.copy(buttons = buttons)) }
        return store.state.value.selected.name
    }

    private fun imagesOf(layout: PadLayout): Map<String, ByteArray> = LayoutArchive.imageNamesUsedBy(layout)
        .mapNotNull { name -> CustomIcons.readBytes(CustomIcons.key(name))?.let { name to it } }
        .toMap()

    companion object {
        /** Folder in the cache that FileProvider shares from (see the app's file_paths.xml). */
        const val SHARE_DIR = "shared"

        /** "My Layout.keyset", with characters file systems dislike replaced. */
        fun fileName(layout: PadLayout): String {
            val safe = layout.name.replace(Regex("[^A-Za-z0-9 _-]"), "_").trim().ifEmpty { "layout" }
            return "$safe.${LayoutArchive.EXTENSION}"
        }
    }
}
