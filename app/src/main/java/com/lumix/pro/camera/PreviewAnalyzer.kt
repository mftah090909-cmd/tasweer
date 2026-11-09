package com.lumix.pro.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sqrt

data class FrameAnalysisResult(
    val histogram: IntArray,
    val meanLuma: Float,
    val rmsContrast: Float,
    val overexposedFraction: Float,
    val underexposedFraction: Float,
    val motionMetric: Float,
    val colorTempEstimateK: Int,
    val zebraBitmap: Bitmap?,
    val focusPeakingBitmap: Bitmap?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FrameAnalysisResult) return false
        return histogram.contentEquals(other.histogram) &&
            meanLuma == other.meanLuma &&
            rmsContrast == other.rmsContrast &&
            motionMetric == other.motionMetric
    }

    override fun hashCode(): Int {
        var r = histogram.contentHashCode()
        r = 31 * r + meanLuma.hashCode()
        r = 31 * r + rmsContrast.hashCode()
        return r
    }
}

class PreviewAnalyzer(
    var zebraEnabled: Boolean = false,
    var focusPeakingEnabled: Boolean = false,
    private val onResult: (FrameAnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var previousSubsampled: ByteArray? = null
    private var lastFrameTime = 0L

    companion object {
        private const val ANALYSIS_STRIDE = 4
        private const val OVEREXPOSE_THRESHOLD = 240
        private const val UNDEREXPOSE_THRESHOLD = 22
        private const val PEAK_MAGNITUDE_THRESHOLD = 32
        private const val ZEBRA_STRIPE_PERIOD = 14
    }

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastFrameTime < 45) { // ~22 fps max analysis rate
            image.close()
            return
        }
        lastFrameTime = now

        try {
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride
            val width = image.width
            val height = image.height

            val histogram = IntArray(256)
            var lumaSum = 0L
            var lumaSqSum = 0.0
            var overCount = 0
            var underCount = 0
            var sampleCount = 0

            val sampleW = (width / ANALYSIS_STRIDE).coerceAtLeast(1)
            val sampleH = (height / ANALYSIS_STRIDE).coerceAtLeast(1)
            val subsampled = ByteArray(sampleW * sampleH)

            // 1) Luminance sampling + histogram + exposure metrics
            var subIdx = 0
            var y = 0
            while (y < height) {
                var x = 0
                while (x < width) {
                    val index = y * yRowStride + x * yPixelStride
                    if (index < yBuffer.capacity()) {
                        val v = yBuffer.get(index).toInt() and 0xFF
                        histogram[v]++
                        lumaSum += v
                        lumaSqSum += v * v
                        if (v >= OVEREXPOSE_THRESHOLD) overCount++
                        if (v <= UNDEREXPOSE_THRESHOLD) underCount++
                        if (subIdx < subsampled.size) {
                            subsampled[subIdx++] = v.toByte()
                        }
                        sampleCount++
                    }
                    x += ANALYSIS_STRIDE
                }
                y += ANALYSIS_STRIDE
            }

            val samples = sampleCount.coerceAtLeast(1)
            val mean = lumaSum.toFloat() / samples
            val rmsContrast = sqrt((lumaSqSum / samples) - (mean * mean)).toFloat()
            val overFrac = overCount.toFloat() / samples
            val underFrac = underCount.toFloat() / samples

            // 2) Motion metric (difference with previous subsampled frame)
            var motion = 0f
            val prev = previousSubsampled
            if (prev != null && prev.size == subsampled.size) {
                var diffSum = 0L
                for (i in subsampled.indices) {
                    diffSum += abs((subsampled[i].toInt() and 0xFF) - (prev[i].toInt() and 0xFF))
                }
                motion = diffSum.toFloat() / subsampled.size
            }
            previousSubsampled = subsampled

            // 3) Color temperature estimate from U, V planes
            val colorTemp = estimateColorTemperature(image)

            // 4) Optional zebra stripes overlay bitmap (ARGB on subsampled resolution)
            val zebraBmp = if (zebraEnabled && overFrac > 0.005f) {
                renderZebra(subsampled, sampleW, sampleH)
            } else null

            // 5) Optional focus peaking edge map (Laplacian on subsampled grid)
            val peakingBmp = if (focusPeakingEnabled) {
                renderFocusPeaking(subsampled, sampleW, sampleH)
            } else null

            onResult(
                FrameAnalysisResult(
                    histogram = histogram,
                    meanLuma = mean,
                    rmsContrast = rmsContrast,
                    overexposedFraction = overFrac,
                    underexposedFraction = underFrac,
                    motionMetric = motion,
                    colorTempEstimateK = colorTemp,
                    zebraBitmap = zebraBmp,
                    focusPeakingBitmap = peakingBmp
                )
            )
        } finally {
            image.close()
        }
    }

    private fun estimateColorTemperature(image: ImageProxy): Int {
        if (image.planes.size < 3) return 5500
        val uBuf = image.planes[1].buffer
        val vBuf = image.planes[2].buffer
        val uStride = image.planes[1].rowStride
        val vStride = image.planes[2].rowStride
        val uStep = image.planes[1].pixelStride
        val vStep = image.planes[2].pixelStride

        var uSum = 0L
        var vSum = 0L
        var count = 0
        var y = 0
        while (y < image.height / 2) {
            var x = 0
            while (x < image.width / 2) {
                val uIdx = y * uStride + x * uStep
                val vIdx = y * vStride + x * vStep
                if (uIdx < uBuf.capacity() && vIdx < vBuf.capacity()) {
                    uSum += uBuf.get(uIdx).toInt() and 0xFF
                    vSum += vBuf.get(vIdx).toInt() and 0xFF
                    count++
                }
                x += 8
            }
            y += 8
        }
        if (count == 0) return 5500
        val avgU = uSum.toFloat() / count - 128f
        val avgV = vSum.toFloat() / count - 128f
        // Warm light -> positive V (red) and negative U (yellow), cool light -> positive U (blue)
        val temp = 5500 - avgV * 70f + avgU * 60f
        return temp.toInt().coerceIn(2000, 9000)
    }

    private fun renderZebra(sub: ByteArray, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val stripeColorA = 0xAAFF1744.toInt()
        val stripeColorB = 0x00000000
        var i = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val v = sub[i].toInt() and 0xFF
                if (v >= OVEREXPOSE_THRESHOLD) {
                    val diagonal = (x + y) % ZEBRA_STRIPE_PERIOD
                    pixels[i] = if (diagonal < ZEBRA_STRIPE_PERIOD / 2) stripeColorA else stripeColorB
                } else {
                    pixels[i] = 0
                }
                i++
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun renderFocusPeaking(sub: ByteArray, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val peakColor = 0xDD00E5FF.toInt() // vivid cyan edge highlight

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val center = sub[row + x].toInt() and 0xFF
                val up = sub[row - w + x].toInt() and 0xFF
                val down = sub[row + w + x].toInt() and 0xFF
                val left = sub[row + x - 1].toInt() and 0xFF
                val right = sub[row + x + 1].toInt() and 0xFF
                val laplacian = abs(4 * center - up - down - left - right)
                if (laplacian >= PEAK_MAGNITUDE_THRESHOLD) {
                    pixels[row + x] = peakColor
                }
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}
