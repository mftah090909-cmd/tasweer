package com.lumix.pro.modes

import android.graphics.Bitmap
import com.lumix.pro.enhancer.ImageProcessingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Multi-frame exposure fusion: combines bracketed EV shots (e.g. -2, 0, +2)
 * into a single high-dynamic-range photo using Mertens-style well-exposedness,
 * saturation, and contrast weighting.
 */
object MultiShotCapture {

    suspend fun fuseHdr(brackets: List<Bitmap>): Bitmap = withContext(Dispatchers.Default) {
        require(brackets.isNotEmpty()) { "At least one frame required" }
        if (brackets.size == 1) return@withContext ImageProcessingUtils.copyBitmap(brackets[0])

        val w = brackets[0].width
        val h = brackets[0].height
        val N = brackets.size
        val allPixels = brackets.map { ImageProcessingUtils.getPixels(it) }

        val accumR = FloatArray(w * h)
        val accumG = FloatArray(w * h)
        val accumB = FloatArray(w * h)
        val weightSum = FloatArray(w * h)

        for (b in 0 until N) {
            val pixels = allPixels[b]
            for (i in pixels.indices) {
                val px = pixels[i]
                val r = (px ushr 16) and 0xFF
                val g = (px ushr 8) and 0xFF
                val bVal = px and 0xFF
                val luma = 0.299f * r + 0.587f * g + 0.114f * bVal
                val rf = r / 255f
                val gf = g / 255f
                val bf = bVal / 255f

                // Well-exposedness Gaussian around 0.5
                val sigma = 0.2f
                val we = exp(-((rf - 0.5f) * (rf - 0.5f) + (gf - 0.5f) * (gf - 0.5f) + (bf - 0.5f) * (bf - 0.5f)) / (2f * sigma * sigma))

                // Saturation
                val mu = (rf + gf + bf) / 3f
                val sat = kotlin.math.sqrt(((rf - mu) * (rf - mu) + (gf - mu) * (gf - mu) + (bf - mu) * (bf - mu)) / 3f)

                val weight = (we * (sat + 0.05f)).coerceAtLeast(1e-4f)

                accumR[i] += rf * weight
                accumG[i] += gf * weight
                accumB[i] += bf * weight
                weightSum[i] += weight
            }
        }

        val dst = IntArray(w * h)
        for (i in dst.indices) {
            val ws = weightSum[i]
            val r = (accumR[i] / ws * 255f).toInt().coerceIn(0, 255)
            val g = (accumG[i] / ws * 255f).toInt().coerceIn(0, 255)
            val bVal = (accumB[i] / ws * 255f).toInt().coerceIn(0, 255)
            dst[i] = -0x1000000 or (r shl 16) or (g shl 8) or bVal
        }

        val out = ImageProcessingUtils.createBitmap(w, h)
        ImageProcessingUtils.setPixels(out, dst)
        out
    }

    suspend fun stackNight(frames: List<Bitmap>): Bitmap = withContext(Dispatchers.Default) {
        require(frames.isNotEmpty())
        if (frames.size == 1) return@withContext ImageProcessingUtils.copyBitmap(frames[0])

        val w = frames[0].width
        val h = frames[0].height
        val sumR = LongArray(w * h)
        val sumG = LongArray(w * h)
        val sumB = LongArray(w * h)

        frames.forEach { frame ->
            val px = ImageProcessingUtils.getPixels(frame)
            for (i in px.indices) {
                val p = px[i]
                sumR[i] += (p ushr 16) and 0xFF
                sumG[i] += (p ushr 8) and 0xFF
                sumB[i] += p and 0xFF
            }
        }
        val N = frames.size
        val dst = IntArray(w * h)
        for (i in dst.indices) {
            dst[i] = -0x1000000 or
                ((sumR[i] / N).toInt().coerceIn(0, 255) shl 16) or
                ((sumG[i] / N).toInt().coerceIn(0, 255) shl 8) or
                (sumB[i] / N).toInt().coerceIn(0, 255)
        }
        val out = ImageProcessingUtils.createBitmap(w, h)
        ImageProcessingUtils.setPixels(out, dst)
        out
    }
}
