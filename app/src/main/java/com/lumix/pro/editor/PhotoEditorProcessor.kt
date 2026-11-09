package com.lumix.pro.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.lumix.pro.enhancer.AiPhotoEnhancer
import com.lumix.pro.enhancer.HslAdjuster
import com.lumix.pro.enhancer.ImageProcessingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

object PhotoEditorProcessor {

    private val rng = Random(42)

    suspend fun apply(src: Bitmap, model: PhotoEditorModel): Bitmap = withContext(Dispatchers.Default) {
        var bmp = ImageProcessingUtils.copyBitmap(src)

        // 1) Geometry
        if (model.rotationDegrees % 360 != 0) {
            val m = Matrix().apply { postRotate(model.rotationDegrees.toFloat()) }
            val rot = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rot != bmp) bmp.recycle()
            bmp = rot
        }
        model.cropAspect?.let { aspect ->
            if (aspect > 0f) bmp = cropToAspect(bmp, aspect)
        }

        // 2) Noise reduction
        if (model.noiseReduction > 0f) {
            bmp = AiPhotoEnhancer.bilateralDenoise(
                bmp,
                spatialSigma = 2f + 2f * (model.noiseReduction / 100f),
                rangeSigma = 20f + 50f * (model.noiseReduction / 100f)
            )
        }

        // 3) Clarity & texture via unsharp masking
        val detailAmount = (model.clarity * 0.006f + model.texture * 0.004f)
        if (abs(detailAmount) > 0.01f) {
            bmp = AiPhotoEnhancer.unsharpMask(bmp, amount = detailAmount.coerceAtLeast(0f), radius = 2.0f, threshold = 2)
        }

        // 4) Pixel-level adjustments
        val w = bmp.width
        val h = bmp.height
        val px = ImageProcessingUtils.getPixels(bmp)
        val dst = IntArray(px.size)

        val expFactor = 2f.pow(model.exposure)
        val contrastFactor = (model.contrast + 100f) / 100f
        val tempK = model.temperature * 0.7f
        val tintK = model.tint * 0.6f
        val vib = model.vibrance / 100f
        val sat = (model.saturation + 100f) / 100f
        val highK = model.highlights / 100f
        val shadK = model.shadows / 100f
        val whiteK = model.whites / 100f
        val blackK = model.blacks / 100f

        val hslAdj = if (model.hslAdjustments.isNotEmpty()) HslAdjuster(model.hslAdjustments) else null

        for (i in px.indices) {
            val p = px[i]
            val a = (p ushr 24) and 0xFF
            var r = ((p ushr 16) and 0xFF).toFloat()
            var g = ((p ushr 8) and 0xFF).toFloat()
            var b = (p and 0xFF).toFloat()

            // Exposure
            r *= expFactor
            g *= expFactor
            b *= expFactor

            // Temperature / Tint
            r += tempK * 0.9f + tintK * 0.4f
            g += -tintK * 0.5f
            b += -tempK * 0.9f + tintK * 0.4f

            // Contrast around midtone 128
            r = 128f + (r - 128f) * contrastFactor
            g = 128f + (g - 128f) * contrastFactor
            b = 128f + (b - 128f) * contrastFactor

            // Highlights & Shadows
            val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            val hiWeight = luma.coerceIn(0f, 1f)
            val shWeight = (1f - luma).coerceIn(0f, 1f)
            val hiDelta = highK * hiWeight * 90f
            val shDelta = shadK * shWeight * 90f
            r += hiDelta + shDelta + whiteK * hiWeight * 60f + blackK * shWeight * -60f
            g += hiDelta + shDelta + whiteK * hiWeight * 60f + blackK * shWeight * -60f
            b += hiDelta + shDelta + whiteK * hiWeight * 60f + blackK * shWeight * -60f

            // Vibrance + Saturation
            val mean = (r + g + b) / 3f
            val mx = max(r, max(g, b))
            val curSat = if (mx > 0) (mx - min(r, min(g, b))) / mx else 0f
            val vibBoost = vib * (1f - curSat)
            r = mean + (r - mean) * (sat + vibBoost)
            g = mean + (g - mean) * (sat + vibBoost)
            b = mean + (b - mean) * (sat + vibBoost)

            // Clamp
            var ri = r.toInt().coerceIn(0, 255)
            var gi = g.toInt().coerceIn(0, 255)
            var bi = b.toInt().coerceIn(0, 255)

            // HSL per band
            if (hslAdj != null) {
                val rgb = hslAdj.applyRgb(ri, gi, bi)
                ri = rgb[0]; gi = rgb[1]; bi = rgb[2]
            }

            dst[i] = (a shl 24) or (ri shl 16) or (gi shl 8) or bi
        }
        ImageProcessingUtils.setPixels(bmp, dst)

        // 5) LUT
        bmp = AiPhotoEnhancer.applyLut(bmp, model.lutLook, model.lutStrength)

        // 6) Sharpness
        if (model.sharpness > 0f) {
            bmp = AiPhotoEnhancer.unsharpMask(
                bmp,
                amount = model.sharpness * 0.015f,
                radius = 1.0f,
                threshold = 2
            )
        }

        // 7) Vignette
        if (model.vignette > 0f) bmp = applyVignette(bmp, model.vignette / 100f)

        // 8) Film grain
        if (model.grain > 0f) bmp = applyGrain(bmp, model.grain / 100f)

        bmp
    }

    private fun cropToAspect(src: Bitmap, targetAspect: Float): Bitmap {
        val srcAspect = src.width.toFloat() / src.height.toFloat()
        val (cw, ch) = if (srcAspect > targetAspect) {
            val nw = (src.height * targetAspect).toInt()
            nw to src.height
        } else {
            val nh = (src.width / targetAspect).toInt()
            src.width to nh
        }
        val x = (src.width - cw) / 2
        val y = (src.height - ch) / 2
        val cropped = Bitmap.createBitmap(src, x, y, cw, ch)
        if (cropped != src) src.recycle()
        return cropped
    }

    private fun applyVignette(bmp: Bitmap, strength: Float): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val px = ImageProcessingUtils.getPixels(bmp)
        val cx = w / 2f
        val cy = h / 2f
        val maxR = sqrt(cx * cx + cy * cy)
        for (y in 0 until h) {
            val dy = y - cy
            val row = y * w
            for (x in 0 until w) {
                val dx = x - cx
                val r = sqrt(dx * dx + dy * dy) / maxR
                val vig = 1f - strength * (r * r).coerceIn(0f, 1f) * 0.75f
                val p = px[row + x]
                val a = p and -0x1000000
                val red = (((p ushr 16) and 0xFF) * vig).toInt().coerceIn(0, 255)
                val grn = (((p ushr 8) and 0xFF) * vig).toInt().coerceIn(0, 255)
                val blu = ((p and 0xFF) * vig).toInt().coerceIn(0, 255)
                px[row + x] = a or (red shl 16) or (grn shl 8) or blu
            }
        }
        val out = ImageProcessingUtils.copyBitmap(bmp)
        ImageProcessingUtils.setPixels(out, px)
        if (out != bmp) bmp.recycle()
        return out
    }

    private fun applyGrain(bmp: Bitmap, strength: Float): Bitmap {
        val px = ImageProcessingUtils.getPixels(bmp)
        val mag = strength * 28f
        for (i in px.indices) {
            val noise = (rng.nextGaussian() * mag).toFloat()
            val p = px[i]
            val a = p and -0x1000000
            val r = (((p ushr 16) and 0xFF) + noise).toInt().coerceIn(0, 255)
            val g = (((p ushr 8) and 0xFF) + noise).toInt().coerceIn(0, 255)
            val b = ((p and 0xFF) + noise).toInt().coerceIn(0, 255)
            px[i] = a or (r shl 16) or (g shl 8) or b
        }
        val out = ImageProcessingUtils.copyBitmap(bmp)
        ImageProcessingUtils.setPixels(out, px)
        if (out != bmp) bmp.recycle()
        return out
    }

    fun export(
        context: Context,
        fullResSrc: Bitmap,
        model: PhotoEditorModel,
        srcUriForExif: Uri? = null,
        quality: Int = 96
    ): Uri? {
        val finalBmp = kotlinx.coroutines.runBlocking { apply(fullResSrc, model) }
        val outUri = ImageProcessingUtils.saveToMediaStore(context, finalBmp, quality, prefix = "LUMIX_EDIT")
        finalBmp.recycle()
        if (outUri != null && srcUriForExif != null) copyExif(context, srcUriForExif, outUri)
        return outUri
    }

    private fun copyExif(context: Context, srcUri: Uri, dstUri: Uri) {
        runCatching {
            var srcExif: ExifInterface? = null
            context.contentResolver.openInputStream(srcUri)?.use { srcExif = ExifInterface(it) }
            val se = srcExif ?: return
            context.contentResolver.openFileDescriptor(dstUri, "rw")?.use { pfd ->
                val dstExif = ExifInterface(pfd.fileDescriptor)
                listOf(
                    ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_APERTURE_VALUE,
                    ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_FLASH,
                    ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE_REF
                ).forEach { tag ->
                    se.getAttribute(tag)?.let { dstExif.setAttribute(tag, it) }
                }
                dstExif.setAttribute(ExifInterface.TAG_SOFTWARE, "LUMIX PRO Editor")
                dstExif.saveAttributes()
            }
        }
    }
}
