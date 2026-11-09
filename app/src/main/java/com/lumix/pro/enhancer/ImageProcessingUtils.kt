package com.lumix.pro.enhancer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object ImageProcessingUtils {

    fun decodeOrientedBitmap(context: Context, uri: Uri, maxDimension: Int = 4096): Bitmap? {
        val cr = context.contentResolver

        // 1) Read orientation from EXIF
        val orientation = runCatching {
            cr.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        // 2) Read bounds
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        var inSample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (max(w, h) > maxDimension) {
            inSample *= 2
            w /= 2
            h /= 2
        }

        // 3) Decode
        val opts = BitmapFactory.Options().apply {
            inSampleSize = inSample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }
        val raw = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        // 4) Apply EXIF rotation
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }
        return if (matrix.isIdentity) raw else {
            val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            if (rotated != raw) raw.recycle()
            rotated
        }
    }

    fun saveToMediaStore(
        context: Context,
        bitmap: Bitmap,
        quality: Int = 96,
        prefix: String = "LUMIX_AI"
    ): Uri? {
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${prefix}_$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/LUMIX_PRO")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val finish = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            runCatching { context.contentResolver.update(uri, finish, null, null) }
        }
        return uri
    }

    fun getPixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }

    fun setPixels(bitmap: Bitmap, pixels: IntArray) {
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    fun copyBitmap(bitmap: Bitmap): Bitmap =
        bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)

    fun createBitmap(w: Int, h: Int): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

    fun clamp(v: Int, lo: Int = 0, hi: Int = 255): Int = v.coerceIn(lo, hi)
    fun clamp(v: Float, lo: Float = 0f, hi: Float = 255f): Float = v.coerceIn(lo, hi)

    inline fun packArgb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b
}
