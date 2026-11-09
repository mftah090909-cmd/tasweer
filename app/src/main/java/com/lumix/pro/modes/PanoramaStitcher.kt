package com.lumix.pro.modes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.lumix.pro.enhancer.ImageProcessingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PanoramaStitcher {

    suspend fun stitchHorizontal(
        frames: List<Bitmap>,
        onProgress: ((Float) -> Unit)? = null
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) return@withContext null
        if (frames.size == 1) return@withContext ImageProcessingUtils.copyBitmap(frames[0])

        val h = frames[0].height
        val normalized = frames.map {
            if (it.height != h) Bitmap.createScaledBitmap(it, (it.width * h / it.height.toFloat()).toInt(), h, true) else it
        }

        var result = normalized[0]
        for (i in 1 until normalized.size) {
            val next = normalized[i]
            val shift = estimateOverlapX(result, next)
            result = blendOverlap(result, next, shift)
            onProgress?.invoke(i / (normalized.size - 1f))
        }
        result
    }

    private fun estimateOverlapX(left: Bitmap, right: Bitmap): Int {
        val stripW = min(120, min(left.width / 4, right.width / 4))
        val sampleH = min(200, left.height)
        val lyStep = max(1, left.height / sampleH)

        val rStrip = extractLumaStrip(right, 0, stripW, sampleH, lyStep)
        var bestShift = left.width / 3
        var bestSad = Long.MAX_VALUE

        val startX = (left.width * 0.35f).toInt()
        val endX = (left.width * 0.85f).toInt()
        var sx = startX
        while (sx < endX) {
            val lStrip = extractLumaStrip(left, sx, stripW, sampleH, lyStep)
            var sad = 0L
            for (k in 0 until min(lStrip.size, rStrip.size)) {
                sad += abs(lStrip[k] - rStrip[k])
            }
            if (sad < bestSad) {
                bestSad = sad
                bestShift = sx
            }
            sx += 4
        }
        return bestShift
    }

    private fun extractLumaStrip(bmp: Bitmap, x0: Int, w: Int, sampleH: Int, yStep: Int): IntArray {
        val pixels = IntArray(sampleH * w)
        var idx = 0
        var y = 0
        while (y < bmp.height && idx < pixels.size) {
            for (x in 0 until w) {
                val p = bmp.getPixel(min(x0 + x, bmp.width - 1), y)
                val luma = (0.299f * ((p ushr 16) and 0xFF) + 0.587f * ((p ushr 8) and 0xFF) + 0.114f * (p and 0xFF)).toInt()
                pixels[idx++] = luma
            }
            y += yStep
        }
        return pixels
    }

    private fun blendOverlap(left: Bitmap, right: Bitmap, shift: Int): Bitmap {
        val overlap = left.width - shift
        val outW = shift + right.width
        val out = ImageProcessingUtils.createBitmap(outW, left.height)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(left, 0f, 0f, paint)

        // Feather-blend the right image in the overlap zone
        val rightPx = ImageProcessingUtils.getPixels(right)
        val leftPx = ImageProcessingUtils.getPixels(left)
        val outPx = IntArray(outW * left.height)
        out.getPixels(outPx, 0, outW, 0, 0, outW, left.height)

        for (y in 0 until left.height) {
            val lRow = y * left.width
            val rRow = y * right.width
            val oRow = y * outW
            for (x in 0 until right.width) {
                val outX = shift + x
                val rp = rightPx[rRow + x]
                if (x < overlap) {
                    val t = x.toFloat() / max(1, overlap)
                    val lp = leftPx[lRow + outX]
                    val lr = (lp ushr 16) and 0xFF
                    val lg = (lp ushr 8) and 0xFF
                    val lb = lp and 0xFF
                    val rr = (rp ushr 16) and 0xFF
                    val rg = (rp ushr 8) and 0xFF
                    val rb = rp and 0xFF
                    val r = (lr * (1 - t) + rr * t).toInt().coerceIn(0, 255)
                    val g = (lg * (1 - t) + rg * t).toInt().coerceIn(0, 255)
                    val b = (lb * (1 - t) + rb * t).toInt().coerceIn(0, 255)
                    outPx[oRow + outX] = -0x1000000 or (r shl 16) or (g shl 8) or b
                } else {
                    outPx[oRow + outX] = rp
                }
            }
        }
        ImageProcessingUtils.setPixels(out, outPx)
        return out
    }
}
