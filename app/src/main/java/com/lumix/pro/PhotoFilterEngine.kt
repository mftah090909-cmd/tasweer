package com.lumix.pro

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/** Real, offline photo processing for the capture and edit flows. */
object PhotoFilterEngine {
    data class Recipe(
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
        val temperature: Float = 0f,
        val fade: Float = 0f,
        val vignette: Float = 0f,
        val sharpen: Float = 0f,
        val monochrome: Boolean = false
    )

    fun recipe(name: String, brightness: Float, contrast: Float, sharpness: Float): Recipe {
        val base = when (name) {
            "Cinematic" -> Recipe(contrast = 1.12f, saturation = 1.08f, temperature = 0.05f, vignette = .28f)
            "Warm" -> Recipe(saturation = 1.12f, temperature = .16f, contrast = 1.04f)
            "Cool" -> Recipe(saturation = 1.04f, temperature = -.14f, contrast = 1.05f)
            "Noir" -> Recipe(contrast = 1.22f, saturation = 0f, monochrome = true, vignette = .32f)
            "Neon" -> Recipe(contrast = 1.2f, saturation = 1.38f, temperature = -.03f)
            "Faded" -> Recipe(contrast = .92f, saturation = .88f, fade = .12f)
            "Clean" -> Recipe(contrast = 1.06f, saturation = 1.02f)
            else -> Recipe()
        }
        return base.copy(
            brightness = base.brightness + brightness,
            contrast = base.contrast * contrast,
            sharpen = sharpness
        )
    }

    suspend fun processAndSave(context: Context, source: Uri?, filter: String, brightness: Float, contrast: Float, sharpness: Float): Uri? = withContext(Dispatchers.IO) {
        source ?: return@withContext null
        val sourceBitmap = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
        val processed = process(sourceBitmap, recipe(filter, brightness, contrast, sharpness))
        val values = ContentValues().apply {
            val stamp = System.currentTimeMillis()
            put(MediaStore.Images.Media.DISPLAY_NAME, "LUMIX_EDIT_$stamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/LUMIX_PRO/Edits")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) return@withContext null
        val ok = context.contentResolver.openOutputStream(uri)?.use { processed.compress(Bitmap.CompressFormat.JPEG, 94, it) } == true
        if (!ok) context.contentResolver.delete(uri, null, null)
        processed.recycle()
        sourceBitmap.recycle()
        if (ok) uri else null
    }

    fun process(source: Bitmap, r: Recipe): Bitmap {
        val src = source.copy(Bitmap.Config.ARGB_8888, false)
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(src.width * src.height)
        src.getPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
        val cx = src.width / 2f
        val cy = src.height / 2f
        val maxDistance = kotlin.math.sqrt(cx * cx + cy * cy)
        val factor = r.contrast.coerceIn(.35f, 2.4f)
        for (i in pixels.indices) {
            val x = i % src.width
            val y = i / src.width
            var red = Color.red(pixels[i]).toFloat()
            var green = Color.green(pixels[i]).toFloat()
            var blue = Color.blue(pixels[i]).toFloat()
            val luma = .299f * red + .587f * green + .114f * blue
            if (r.monochrome) red = luma; green = luma; blue = luma
            val avg = (red + green + blue) / 3f
            red = avg + (red - avg) * r.saturation
            green = avg + (green - avg) * r.saturation
            blue = avg + (blue - avg) * r.saturation
            red += r.temperature * 45f
            blue -= r.temperature * 38f
            green += r.temperature * 8f
            red = (red - 128f) * factor + 128f + r.brightness * 255f
            green = (green - 128f) * factor + 128f + r.brightness * 255f
            blue = (blue - 128f) * factor + 128f + r.brightness * 255f
            val distance = kotlin.math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / maxDistance
            val vignette = 1f - r.vignette.coerceIn(0f, .8f) * distance * distance
            red = red * vignette + 128f * r.fade
            green = green * vignette + 128f * r.fade
            blue = blue * vignette + 128f * r.fade
            pixels[i] = Color.argb(255, clamp(red), clamp(green), clamp(blue))
        }
        out.setPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
        if (r.sharpen > .01f) sharpen(out, r.sharpen)
        src.recycle()
        return out
    }

    private fun sharpen(bitmap: Bitmap, strength: Float) {
        val w = bitmap.width; val h = bitmap.height
        if (w < 3 || h < 3) return
        val pixels = IntArray(w * h); bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val copy = pixels.copyOf(); val amount = strength.coerceIn(0f, 1f)
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            fun lum(p: Int) = .299f * Color.red(p) + .587f * Color.green(p) + .114f * Color.blue(p)
            val edge = lum(copy[i]) * 5f - lum(copy[i - 1]) - lum(copy[i + 1]) - lum(copy[i - w]) - lum(copy[i + w])
            val delta = (edge - lum(copy[i])) * amount
            pixels[i] = Color.argb(255, clamp(Color.red(copy[i]) + delta), clamp(Color.green(copy[i]) + delta), clamp(Color.blue(copy[i]) + delta))
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun clamp(v: Float): Int = max(0, min(255, v.roundToIntSafe()))
    private fun Float.roundToIntSafe(): Int = kotlin.math.round(this).toInt()
}
