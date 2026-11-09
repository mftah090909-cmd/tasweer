package com.lumix.pro.gallery

import android.net.Uri

data class MediaItem(
    val id: Long,
    val contentUri: Uri,
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isRaw: Boolean = false,
    val album: String = "LUMIX PRO",
    val isPrivateAlbum: Boolean = false
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val resolutionLabel: String get() = if (width > 0 && height > 0) "${width}×${height}" else ""
    val durationLabel: String
        get() {
            if (!isVideo) return ""
            val s = (durationMs / 1000).toInt()
            val m = s / 60
            return "%d:%02d".format(m, s % 60)
        }
}
