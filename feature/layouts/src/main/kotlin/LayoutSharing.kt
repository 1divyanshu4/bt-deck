package dev.keyset.feature.layouts

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.keyset.core.data.LayoutTransfer
import dev.keyset.core.model.PadLayout

/** The type `.keyset` files are sent as. Chat and file apps rarely know custom types. */
internal const val KEYSET_MIME = "application/octet-stream"

/** Opens the share sheet with [layout] as a `.keyset` file. */
internal suspend fun shareLayout(context: Context, transfer: LayoutTransfer, layout: PadLayout) {
    val file = transfer.shareFile(layout)
    // Other apps can only read the file through this content URI, and only for this share.
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType(KEYSET_MIME)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, "KeySet layout: ${layout.name}")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Share layout"))
}
