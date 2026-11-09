package com.lumix.pro.modes

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import com.lumix.pro.enhancer.ImageProcessingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object DocumentScanner {

    /**
     * Document enhancement: local adaptive threshold + edge sharpening for crisp text.
     */
    suspend fun enhanceDocument(bitmap: Bitmap, windowRadius: Int = 16, kFactor: Float = 0.2f): Bitmap =
        withContext(Dispatchers.Default) {
            val w = bitmap.width
            val h = bitmap.height
            val src = ImageProcessingUtils.getPixels(bitmap)
            val luma = IntArray(src.size)
            for (i in src.indices) {
                val p = src[i]
                luma[i] = (0.299f * ((p ushr 16) and 0xFF) + 0.587f * ((p ushr 8) and 0xFF) + 0.114f * (p and 0xFF)).toInt()
            }

            // Integral image for fast local mean computation
            val integral = LongArray((w + 1) * (h + 1))
            val stride = w + 1
            for (y in 0 until h) {
                var rowSum = 0L
                for (x in 0 until w) {
                    rowSum += luma[y * w + x]
                    integral[(y + 1) * stride + (x + 1)] = integral[y * stride + (x + 1)] + rowSum
                }
            }

            val dst = IntArray(src.size)
            val r = windowRadius
            for (y in 0 until h) {
                val y0 = max(0, y - r)
                val y1 = min(h, y + r + 1)
                for (x in 0 until w) {
                    val x0 = max(0, x - r)
                    val x1 = min(w, x + r + 1)
                    val count = (x1 - x0) * (y1 - y0)
                    val sum = integral[y1 * stride + x1] - integral[y0 * stride + x1] -
                        integral[y1 * stride + x0] + integral[y0 * stride + x0]
                    val mean = sum / count
                    val v = luma[y * w + x]
                    // Sauvola-like threshold
                    val thresh = mean * (1f - kFactor)
                    val outVal = if (v > thresh) 255 else 0
                    dst[y * w + x] = -0x1000000 or (outVal shl 16) or (outVal shl 8) or outVal
                }
            }
            val out = ImageProcessingUtils.createBitmap(w, h)
            ImageProcessingUtils.setPixels(out, dst)
            out
        }

    /**
     * Perspective keystone correction given four corners (TL, TR, BR, BL).
     */
    fun perspectiveCorrect(
        bitmap: Bitmap,
        tl: PointF,
        tr: PointF,
        br: PointF,
        bl: PointF,
        outW: Int = bitmap.width,
        outH: Int = bitmap.height
    ): Bitmap {
        val src = floatArrayOf(tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)
        val dst = floatArrayOf(0f, 0f, outW.toFloat(), 0f, outW.toFloat(), outH.toFloat(), 0f, outH.toFloat())
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
