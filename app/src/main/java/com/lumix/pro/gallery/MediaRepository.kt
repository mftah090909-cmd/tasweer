package com.lumix.pro.gallery

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("lumix_gallery", Context.MODE_PRIVATE)

    fun isFavorite(id: Long): Boolean =
        prefs.getStringSet("favorites", emptySet())?.contains(id.toString()) == true

    fun toggleFavorite(id: Long): Boolean {
        val current = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
        val key = id.toString()
        val fav = if (current.contains(key)) { current.remove(key); false } else { current.add(key); true }
        prefs.edit().putStringSet("favorites", current).apply()
        return fav
    }

    suspend fun loadAll(filterLumixOnly: Boolean = false): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = queryImages(filterLumixOnly)
        val videos = queryVideos(filterLumixOnly)
        val privateItems = queryPrivateAlbum()
        (images + videos + privateItems).sortedByDescending { it.dateAddedMs }
    }

    suspend fun getLatestThumbnail(): Uri? = withContext(Dispatchers.IO) {
        loadAll(filterLumixOnly = true).firstOrNull()?.contentUri
            ?: loadAll(filterLumixOnly = false).firstOrNull()?.contentUri
    }

    private fun queryImages(lumixOnly: Boolean): List<MediaItem> {
        val proj = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sel = if (lumixOnly) {
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE 'LUMIX_%' OR ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = 'LUMIX_PRO'"
        } else null
        return runCatching {
            val list = mutableListOf<MediaItem>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                proj, sel, null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val mime = c.getString(mimeCol) ?: "image/jpeg"
                    val isRaw = mime == "image/x-adobe-dng" || (c.getString(nameCol)?.endsWith(".dng", ignoreCase = true) == true)
                    list += MediaItem(
                        id = id,
                        contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                        displayName = c.getString(nameCol) ?: "photo",
                        mimeType = mime,
                        width = c.getInt(wCol),
                        height = c.getInt(hCol),
                        sizeBytes = c.getLong(sizeCol),
                        dateAddedMs = c.getLong(dateCol) * 1000L,
                        isFavorite = isFavorite(id),
                        isRaw = isRaw
                    )
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun queryVideos(lumixOnly: Boolean): List<MediaItem> {
        val proj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )
        val sel = if (lumixOnly) {
            "${MediaStore.Video.Media.DISPLAY_NAME} LIKE 'LUMIX_%' OR ${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = 'LUMIX_PRO'"
        } else null
        return runCatching {
            val list = mutableListOf<MediaItem>()
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                proj, sel, null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val wCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val hCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    list += MediaItem(
                        id = id,
                        contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                        displayName = c.getString(nameCol) ?: "video",
                        mimeType = c.getString(mimeCol) ?: "video/mp4",
                        width = c.getInt(wCol),
                        height = c.getInt(hCol),
                        sizeBytes = c.getLong(sizeCol),
                        dateAddedMs = c.getLong(dateCol) * 1000L,
                        durationMs = c.getLong(durCol),
                        isFavorite = isFavorite(id)
                    )
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun queryPrivateAlbum(): List<MediaItem> {
        val dir = context.filesDir.resolve("private_album").apply { mkdirs() }
        return dir.listFiles().orEmpty().mapNotNull { f ->
            val mime = when {
                f.name.endsWith(".jpg", true) || f.name.endsWith(".jpeg", true) -> "image/jpeg"
                f.name.endsWith(".png", true) -> "image/png"
                f.name.endsWith(".mp4", true) -> "video/mp4"
                else -> null
            } ?: return@mapNotNull null
            MediaItem(
                id = -f.lastModified(),
                contentUri = Uri.fromFile(f),
                displayName = f.name,
                mimeType = mime,
                width = 0,
                height = 0,
                sizeBytes = f.length(),
                dateAddedMs = f.lastModified(),
                album = "Private",
                isPrivateAlbum = true
            )
        }
    }

    fun moveToPrivateAlbum(item: MediaItem): Boolean {
        return runCatching {
            val dir = context.filesDir.resolve("private_album").apply { mkdirs() }
            val dst = File(dir, item.displayName)
            context.contentResolver.openInputStream(item.contentUri)?.use { input ->
                dst.outputStream().use { input.copyTo(it) }
            }
            context.contentResolver.delete(item.contentUri, null, null)
            true
        }.getOrDefault(false)
    }

    fun deleteItem(item: MediaItem): Boolean {
        if (item.isPrivateAlbum) {
            return File(item.contentUri.path ?: return false).delete()
        }
        return runCatching { context.contentResolver.delete(item.contentUri, null, null) > 0 }.getOrDefault(false)
    }

    fun makeDeleteRequest(activity: Activity, item: MediaItem): IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(item.contentUri)).intentSender
            }.getOrNull()
        }
        return null
    }
}
