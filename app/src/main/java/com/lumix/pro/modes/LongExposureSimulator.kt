package com.lumix.pro.modes

import android.graphics.Bitmap
import com.lumix.pro.enhancer.ImageProcessingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class LongExposureStyle {
    SILKY_WATER, LIGHT_TRAILS
}

class LongExposureSimulator(
    val style: LongExposureStyle = LongExposureStyle.SILKY_WATER,
    val durationSeconds: Int = 3
) {
    private var accumR: LongArray? = null
    private var accumG: LongArray? = null
    private var accumB: LongArray? = null
    private var count = 0
    private var width = 0
    private var height = 0

    fun reset() {
        accumR = null
        accumG = null
        accumB = null
        count = 0
    }

    fun pushFrame(frame: Bitmap) {
        val w = frame.width
        val h = frame.height
        if (accumR == null || width != w || height != h) {
            width = w
            height = h
            accumR = LongArray(w * h)
            accumG = LongArray(w * h)
            accumB = LongArray(w * h)
            count = 0
        }
        val px = ImageProcessingUtils.getPixels(frame)
        val ar = accumR!!
        val ag = accumG!!
        val ab = accumB!!

        if (style == LongExposureStyle.LIGHT_TRAILS) {
            // Maximum blend keeps light streaks
            for (i in px.indices) {
                val p = px[i]
                ar[i] = max(ar[i], ((p ushr 16) and 0xFF).toLong())
                ag[i] = max(ag[i], ((p ushr 8) and 0xFF).toLong())
                ab[i] = max(ab[i], (p and 0xFF).toLong())
            }
        } else {
            // Average blend for silky motion blur
            for (i in px.indices) {
                val p = px[i]
                ar[i] += (p ushr 16) and 0xFF
                ag[i] += (p ushr 8) and 0xFF
                ab[i] += p and 0xFF
            }
        }
        count++
    }

    suspend fun render(): Bitmap? = withContext(Dispatchers.Default) {
        val ar = accumR ?: return@withContext null
        val ag = accumG ?: return@withContext null
        val ab = accumB ?: return@withContext null
        if (count == 0) return@withContext null
        val dst = IntArray(width * height)
        val divisor = if (style == LongExposureStyle.LIGHT_TRAILS) 1 else count
        for (i in dst.indices) {
            val r = (ar[i] / divisor).toInt().coerceIn(0, 255)
            val g = (ag[i] / divisor).toInt().coerceIn(0, 255)
            val b = (ab[i] / divisor).toInt().coerceIn(0, 255)
            dst[i] = -0x1000000 or (r shl 16) or (g shl 8) or b
        }
        val out = ImageProcessingUtils.createBitmap(width, height)
        ImageProcessingUtils.setPixels(out, dst)
        out
    }
}
