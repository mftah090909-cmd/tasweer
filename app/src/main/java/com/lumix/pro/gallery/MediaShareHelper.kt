package com.lumix.pro.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri

object MediaShareHelper {

    fun shareItem(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, item.contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareMultiple(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        if (items.size == 1) {
            shareItem(context, items.first())
            return
        }
        val uris = ArrayList(items.map { it.contentUri })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share media")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
