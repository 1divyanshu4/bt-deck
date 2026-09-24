package dev.btdeck.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * User-picked button images, copied into app storage so they survive the original being
 * deleted. A button refers to one as "img:<file name>" in its icon field.
 */
object CustomIcons {
    const val PREFIX = "img:"
    private const val MAX_SIZE = 256

    private lateinit var dir: File
    private val cache = LruCache<String, Bitmap>(64)
    private val _names = MutableStateFlow<List<String>>(emptyList())

    /** Stored image names, newest first. */
    val names: StateFlow<List<String>> = _names.asStateFlow()

    fun init(context: Context) {
        dir = File(context.filesDir, "icons").apply { mkdirs() }
        refresh()
    }

    fun isCustom(key: String?) = key?.startsWith(PREFIX) == true
    fun key(name: String) = PREFIX + name
    fun nameOf(key: String) = key.removePrefix(PREFIX)

    /** Decodes, downsizes and stores the picked image. Returns its icon key, or null if unreadable. */
    suspend fun import(context: Context, uri: Uri): String? =
        store(ImageDecoder.createSource(context.contentResolver, uri))

    /** Stores image [bytes] (e.g. from a shared layout file) the same way as a picked image. */
    suspend fun importBytes(bytes: ByteArray): String? = store(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))

    /** The stored PNG for [key], or null if there isn't one. */
    fun readBytes(key: String): ByteArray? = File(dir, nameOf(key)).takeIf { it.exists() }?.readBytes()

    /**
     * Decodes, downsizes and saves an image under a new name. Re-encoding means whatever was in
     * the original file (metadata, odd formats) never ends up in app storage as-is.
     */
    private suspend fun store(source: ImageDecoder.Source): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = minOf(1f, MAX_SIZE.toFloat() / maxOf(w, h))
                decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val name = UUID.randomUUID().toString() + ".png"
            File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            refresh()
            key(name)
        }.getOrNull()
    }
    fun delete(key: String) {
        val name = nameOf(key)
        File(dir, name).delete()
        cache.remove(name)
        refresh()
    }

    fun bitmap(key: String): Bitmap? {
        val name = nameOf(key)
        cache.get(name)?.let { return it }
        val file = File(dir, name)
        if (!file.exists()) return null
        val bmp = BitmapFactory.decodeFile(file.path) ?: return null
        cache.put(name, bmp)
        return bmp
    }

    private fun refresh() {
        _names.value = dir.listFiles { f -> f.extension == "png" }.orEmpty()
            .sortedByDescending { it.lastModified() }
            .map { it.name }
    }
}
