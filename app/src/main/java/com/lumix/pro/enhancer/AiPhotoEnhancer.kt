package com.lumix.pro.enhancer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

object AiPhotoEnhancer {

    private const val MAX_AI_DIMENSION = 3840

    suspend fun enhance(
        bitmap: Bitmap,
        level: EnhancementLevel,
        onProgress: ((Float, String) -> Unit)? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        var current = ImageProcessingUtils.copyBitmap(bitmap)

        onProgress?.invoke(0.05f, "Color correction…")
        current = autoWhiteBalance(current, level.colorCorrectionStrength)

        if (level.upscalingFactor > 1.05f) {
            onProgress?.invoke(0.18f, "Super-resolution…")
            val targetW = (current.width * level.upscalingFactor).toInt().coerceAtMost(MAX_AI_DIMENSION)
            val targetH = (current.height * level.upscalingFactor).toInt().coerceAtMost(MAX_AI_DIMENSION)
            current = superResolution(current, targetW, targetH)
        }

        onProgress?.invoke(0.35f, "Dynamic range enhancement…")
        current = adaptiveToneMap(current, level.dynamicRangeStrength)
        current = highlightShadowRecovery(
            current,
            highlightRecovery = 0.5f * level.dynamicRangeStrength,
            shadowBoost = 0.7f * level.dynamicRangeStrength
        )

        onProgress?.invoke(0.52f, "Noise reduction…")
        current = bilateralDenoise(
            current,
            spatialSigma = 2.0f + 1.5f * level.denoiseStrength,
            rangeSigma = 18f + 30f * level.denoiseStrength
        )

        onProgress?.invoke(0.68f, "Texture & skin detail…")
        current = frequencySeparateSkin(
            current,
            textureBoost = level.sharpeningStrength,
            skinSoftening = level.skinSmoothingStrength
        )

        if (level.deblurStrength > 0.05f) {
            onProgress?.invoke(0.80f, "Deblurring…")
            current = wienerDeblur(current, level.deblurStrength)
        }

        onProgress?.invoke(0.90f, "Smart sharpening…")
        current = unsharpMask(
            current,
            amount = 0.6f * level.sharpeningStrength,
            radius = 1.2f,
            threshold = 2
        )

        onProgress?.invoke(0.96f, "Cinematic color science…")
        current = applyLut(current, level.lutLook, 0.9f)

        onProgress?.invoke(1.0f, "Done")
        current
    }

    // ---------------------------------------------------------------- Auto WB (Gray-World)
    fun autoWhiteBalance(bitmap: Bitmap, strength: Float = 0.5f): Bitmap {
        if (strength <= 0f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val pixels = ImageProcessingUtils.getPixels(bitmap)
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        val step = max(1, (w * h) / 100_000)
        var count = 0
        var i = 0
        while (i < pixels.size) {
            val px = pixels[i]
            val r = (px ushr 16) and 0xFF
            val g = (px ushr 8) and 0xFF
            val b = px and 0xFF
            // Exclude clipped pixels from neutral estimate
            if (r in 12..245 && g in 12..245 && b in 12..245) {
                sumR += r
                sumG += g
                sumB += b
                count++
            }
            i += step
        }
        if (count == 0) return bitmap
        val avgR = sumR.toFloat() / count
        val avgG = sumG.toFloat() / count
        val avgB = sumB.toFloat() / count
        val avgAll = (avgR + avgG + avgB) / 3f
        val kR = 1f + (avgAll / avgR - 1f) * strength
        val kG = 1f + (avgAll / avgG - 1f) * strength
        val kB = 1f + (avgAll / avgB - 1f) * strength

        val out = ImageProcessingUtils.copyBitmap(bitmap)
        for (idx in pixels.indices) {
            val px = pixels[idx]
            val a = px and -0x1000000
            val r = (((px ushr 16) and 0xFF) * kR).toInt().coerceIn(0, 255)
            val g = (((px ushr 8) and 0xFF) * kG).toInt().coerceIn(0, 255)
            val b = ((px and 0xFF) * kB).toInt().coerceIn(0, 255)
            pixels[idx] = a or (r shl 16) or (g shl 8) or b
        }
        ImageProcessingUtils.setPixels(out, pixels)
        return out
    }

    // ---------------------------------------------------------------- Super-resolution
    // Two-pass upscale: smooth bicubic + high-frequency synthesis refinement
    fun superResolution(bitmap: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val up = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        val w = up.width
        val h = up.height
        val pixels = ImageProcessingUtils.getPixels(up)
        val src = pixels.clone()
        val out = IntArray(pixels.size)

        // Detail synthesis kernel (edge-preserving high-pass boost)
        val kernel = floatArrayOf(
            -0.05f, -0.1f, -0.05f,
            -0.1f, 1.6f, -0.1f,
            -0.05f, -0.1f, -0.05f
        )
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                var accR = 0f
                var accG = 0f
                var accB = 0f
                var ki = 0
                for (ky in -1..1) {
                    val krow = (y + ky) * w
                    for (kx in -1..1) {
                        val px = src[krow + x + kx]
                        val kw = kernel[ki++]
                        accR += ((px ushr 16) and 0xFF) * kw
                        accG += ((px ushr 8) and 0xFF) * kw
                        accB += (px and 0xFF) * kw
                    }
                }
                val center = src[row + x]
                val a = center and -0x1000000
                val r = accR.toInt().coerceIn(0, 255)
                val g = accG.toInt().coerceIn(0, 255)
                val b = accB.toInt().coerceIn(0, 255)
                out[row + x] = a or (r shl 16) or (g shl 8) or b
            }
        }
        // Border copy
        for (x in 0 until w) {
            out[x] = src[x]
            out[(h - 1) * w + x] = src[(h - 1) * w + x]
        }
        for (y in 0 until h) {
            out[y * w] = src[y * w]
            out[y * w + w - 1] = src[y * w + w - 1]
        }
        val result = ImageProcessingUtils.copyBitmap(up)
        ImageProcessingUtils.setPixels(result, out)
        if (result != up) up.recycle()
        return result
    }

    // ---------------------------------------------------------------- Bilateral denoise
    // Spatial + range Gaussian edge-preserving filter (fast subsampled kernel radius 3)
    fun bilateralDenoise(bitmap: Bitmap, spatialSigma: Float = 3f, rangeSigma: Float = 30f): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = ImageProcessingUtils.getPixels(bitmap)
        val dst = IntArray(src.size)
        val radius = 2
        val twoSs2 = 2f * spatialSigma * spatialSigma
        val twoRs2 = 2f * rangeSigma * rangeSigma

        for (y in 0 until h) {
            val yMin = max(0, y - radius)
            val yMax = min(h - 1, y + radius)
            val row = y * w
            for (x in 0 until w) {
                val xMin = max(0, x - radius)
                val xMax = min(w - 1, x + radius)
                val cp = src[row + x]
                val cr = (cp ushr 16) and 0xFF
                val cg = (cp ushr 8) and 0xFF
                val cb = cp and 0xFF
                var weightSum = 0f
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                for (ky in yMin..yMax) {
                    val krow = ky * w
                    val dy = (ky - y).toFloat()
                    val dy2 = dy * dy
                    for (kx in xMin..xMax) {
                        val dx = (kx - x).toFloat()
                        val spDist = -(dx * dx + dy2) / twoSs2
                        val np = src[krow + kx]
                        val nr = (np ushr 16) and 0xFF
                        val ng = (np ushr 8) and 0xFF
                        val nb = np and 0xFF
                        val colorDist = -((cr - nr) * (cr - nr) + (cg - ng) * (cg - ng) + (cb - nb) * (cb - nb)) / twoRs2
                        val wgt = exp(spDist + colorDist)
                        sumR += nr * wgt
                        sumG += ng * wgt
                        sumB += nb * wgt
                        weightSum += wgt
                    }
                }
                val a = cp and -0x1000000
                val r = (sumR / weightSum).toInt().coerceIn(0, 255)
                val g = (sumG / weightSum).toInt().coerceIn(0, 255)
                val b = (sumB / weightSum).toInt().coerceIn(0, 255)
                dst[row + x] = a or (r shl 16) or (g shl 8) or b
            }
        }
        val out = ImageProcessingUtils.copyBitmap(bitmap)
        ImageProcessingUtils.setPixels(out, dst)
        return out
    }

    // ---------------------------------------------------------------- CLAHE-style adaptive tone map
    fun adaptiveToneMap(bitmap: Bitmap, strength: Float = 0.5f): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = ImageProcessingUtils.getPixels(bitmap)
        val dst = IntArray(src.size)
        val tiles = 4
        val tileW = ceil(w.toFloat() / tiles).toInt()
        val tileH = ceil(h.toFloat() / tiles).toInt()

        // Per-tile equalized CDF lookup tables
        val luts = Array(tiles * tiles) { IntArray(256) }
        for (ty in 0 until tiles) {
            for (tx in 0 until tiles) {
                val hist = IntArray(256)
                var count = 0
                val x0 = tx * tileW
                val x1 = min(w, x0 + tileW)
                val y0 = ty * tileH
                val y1 = min(h, y0 + tileH)
                for (yy in y0 until y1) {
                    val row = yy * w
                    for (xx in x0 until x1) {
                        val px = src[row + xx]
                        val luma = (0.299f * ((px ushr 16) and 0xFF) + 0.587f * ((px ushr 8) and 0xFF) + 0.114f * (px and 0xFF)).toInt()
                        hist[luma]++
                        count++
                    }
                }
                val lut = luts[ty * tiles + tx]
                if (count == 0) {
                    for (v in 0..255) lut[v] = v
                    continue
                }
                // Clip limit
                val clip = (count * 3f / 256f).toInt().coerceAtLeast(1)
                var excess = 0
                for (v in 0..255) if (hist[v] > clip) { excess += hist[v] - clip; hist[v] = clip }
                val perBin = excess / 256
                for (v in 0..255) hist[v] += perBin
                var cdf = 0
                for (v in 0..255) {
                    cdf += hist[v]
                    lut[v] = (cdf * 255 / count).coerceIn(0, 255)
                }
            }
        }

        // Bilinear interpolation between tile LUTs
        for (y in 0 until h) {
            val tyf = (y.toFloat() / tileH - 0.5f).coerceIn(0f, (tiles - 1.0001f))
            val ty0 = tyf.toInt()
            val ty1 = min(tiles - 1, ty0 + 1)
            val ay = tyf - ty0
            val row = y * w
            for (x in 0 until w) {
                val txf = (x.toFloat() / tileW - 0.5f).coerceIn(0f, (tiles - 1.0001f))
                val tx0 = txf.toInt()
                val tx1 = min(tiles - 1, tx0 + 1)
                val ax = txf - tx0

                val lut00 = luts[ty0 * tiles + tx0]
                val lut10 = luts[ty0 * tiles + tx1]
                val lut01 = luts[ty1 * tiles + tx0]
                val lut11 = luts[ty1 * tiles + tx1]

                val px = src[row + x]
                val r = (px ushr 16) and 0xFF
                val g = (px ushr 8) and 0xFF
                val b = px and 0xFF
                val luma = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
                val eq0 = lut00[luma] * (1 - ax) + lut10[luma] * ax
                val eq1 = lut01[luma] * (1 - ax) + lut11[luma] * ax
                val eq = eq0 * (1 - ay) + eq1 * ay

                val gain = if (luma <= 0) 1f else (eq / luma.toFloat())
                val scaledGain = 1f + (gain - 1f) * strength
                val a = px and -0x1000000
                val nr = (r * scaledGain).toInt().coerceIn(0, 255)
                val ng = (g * scaledGain).toInt().coerceIn(0, 255)
                val nb = (b * scaledGain).toInt().coerceIn(0, 255)
                dst[row + x] = a or (nr shl 16) or (ng shl 8) or nb
            }
        }
        val out = ImageProcessingUtils.copyBitmap(bitmap)
        ImageProcessingUtils.setPixels(out, dst)
        return out
    }

    // ---------------------------------------------------------------- Highlight & shadow recovery
    fun highlightShadowRecovery(bitmap: Bitmap, highlightRecovery: Float = 0.4f, shadowBoost: Float = 0.5f): Bitmap {
        val src = ImageProcessingUtils.getPixels(bitmap)
        val dst = IntArray(src.size)
        val lutShadow = IntArray(256)
        val lutHighlight = IntArray(256)
        for (v in 0..255) {
            val vf = v / 255f
            val sLift = vf + (1f - vf) * shadowBoost * 0.45f * (1f - vf)
            val hRoll = 1f - (1f - vf).pow(1f + highlightRecovery * 0.8f)
            lutShadow[v] = (sLift * 255f).toInt().coerceIn(0, 255)
            lutHighlight[v] = (hRoll * 255f).toInt().coerceIn(0, 255)
        }
        for (i in src.indices) {
            val px = src[i]
            val a = px and -0x1000000
            var r = (px ushr 16) and 0xFF
            var g = (px ushr 8) and 0xFF
            var b = px and 0xFF
            r = lutShadow[lutHighlight[r]]
            g = lutShadow[lutHighlight[g]]
            b = lutShadow[lutHighlight[b]]
            dst[i] = a or (r shl 16) or (g shl 8) or b
        }
        val out = ImageProcessingUtils.copyBitmap(bitmap)
        ImageProcessingUtils.setPixels(out, dst)
        return out
    }

    // ---------------------------------------------------------------- Frequency separation (texture vs skin)
    fun frequencySeparateSkin(bitmap: Bitmap, textureBoost: Float, skinSoftening: Float): Bitmap {
        if (textureBoost <= 0f && skinSoftening <= 0f) return bitmap
        val blurred = bilateralDenoise(bitmap, spatialSigma = 4f, rangeSigma = 55f)
        val basePx = ImageProcessingUtils.getPixels(blurred)
        val origPx = ImageProcessingUtils.getPixels(bitmap)
        val out = IntArray(origPx.size)

        for (i in origPx.indices) {
            val op = origPx[i]
            val bp = basePx[i]
            val or_ = (op ushr 16) and 0xFF
            val og = (op ushr 8) and 0xFF
            val ob = op and 0xFF
            val br = (bp ushr 16) and 0xFF
            val bg = (bp ushr 8) and 0xFF
            val bb = bp and 0xFF

            // High-frequency detail layer
            val dr = or_ - br
            val dg = og - bg
            val db = ob - bb

            // Simple skin tone detector in YCbCr: Cr in 133..173, Cb in 80..120
            val y = (0.299f * or_ + 0.587f * og + 0.114f * ob)
            val cb = (-0.1687f * or_ - 0.3313f * og + 0.5f * ob + 128f)
            val cr = (0.5f * or_ - 0.4187f * og - 0.0813f * ob + 128f)
            val isSkin = y > 60 && cb in 78f..128f && cr in 130f..176f
            val soft = if (isSkin) skinSoftening else 0.1f * skinSoftening

            val baseBlendR = br + (or_ - br) * (1f - soft)
            val baseBlendG = bg + (og - bg) * (1f - soft)
            val baseBlendB = bb + (ob - bb) * (1f - soft)

            val a = op and -0x1000000
            val nr = (baseBlendR + dr * (1f + textureBoost * 0.7f)).toInt().coerceIn(0, 255)
            val ng = (baseBlendG + dg * (1f + textureBoost * 0.7f)).toInt().coerceIn(0, 255)
            val nb = (baseBlendB + db * (1f + textureBoost * 0.7f)).toInt().coerceIn(0, 255)
            out[i] = a or (nr shl 16) or (ng shl 8) or nb
        }
        val result = ImageProcessingUtils.copyBitmap(bitmap)
        ImageProcessingUtils.setPixels(result, out)
        if (blurred != result) blurred.recycle()
        return result
    }

    // ---------------------------------------------------------------- Wiener deblur kernel
    fun wienerDeblur(bitmap: Bitmap, strength: Float = 0.3f): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = ImageProcessingUtils.getPixels(bitmap)
        val dst = IntArray(src.size)
        // 3x3 deconvolution kernel approximating inverse Gaussian PSF with noise regularization
        val k = strength * 0.18f
        val kCenter = 1f + 8f * k
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val cp = src[row + x]
                val cr = (cp ushr 16) and 0xFF
                val cg = (cp ushr 8) and 0xFF
                val cb = cp and 0xFF
                var neighR = 0f
                var neighG = 0f
                var neighB = 0f
                for (ky in -1..1) {
                    val krow = (y + ky) * w
                    for (kx in -1..1) {
                        if (kx == 0 && ky == 0) continue
                        val np = src[krow + x + kx]
                        neighR += (np ushr 16) and 0xFF
                        neighG += (np ushr 8) and 0xFF
                        neighB += np and 0xFF
                    }
                }
                val nr = (cr * kCenter - neighR * k).toInt().coerceIn(0, 255)
                val ng = (cg * kCenter - neighG * k).toInt().coerceIn(0, 255)
                val nb = (cb * kCenter - neighB * k).toInt().coerceIn(0, 255)
                dst[row + x] = (cp and -0x1000000) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        for (x in 0 until w) { dst[x] = src[x]; dst[(h - 1) * w + x] = src[(h - 1) * w + x] }
        for (y in 0 until h) { dst[y * w] = src[y * w]; dst[y * w + w - 1] = src[y * w + w - 1] }
        val out = ImageProcessingUtils.copyBitmap(bitmap)
        ImageProcessingUtils.setPixels(out, dst)
        return out
    }

    // ---------------------------------------------------------------- Unsharp mask sharpening
    fun unsharpMask(bitmap: Bitmap, amount: Float = 0.5f, radius: Float = 1.0f, threshold: Int = 3): Bitmap {
        if (amount <= 0.01f) return bitmap
        val blurred = bilateralDenoise(bitmap, spatialSigma = radius, rangeSigma = 120f)
        val origPx = ImageProcessingUtils.getPixels(bitmap)
        val blurPx = ImageProcessingUtils.getPixels(blurred)
        val dst = IntArray(origPx.size)
        for (i in origPx.indices) {
            val op = origPx[i]
            val bp = blurPx[i]
            val or_ = (op ushr 16) and 0xFF
            val og = (op ushr 8) and 0xFF
            val ob = op and 0xFF
            val dr = or_ - ((bp ushr 16) and 0xFF)
            val dg = og - ((bp ushr 8) and 0xFF)
            val db = ob - (bp and 0xFF)
            val diff = (abs(dr) + abs(dg) + abs(db)) / 3
            if (diff >= threshold) {
                val nr = (or_ + dr * amount).toInt().coerceIn(0, 255)
                val ng = (og + dg * amount).toInt().coerceIn(0, 255)
                val nb = (ob + db * amount).toInt().coerceIn(0, 255)
                dst[i] = (op and -0x1000000) or (nr shl 16) or (ng shl 8) or nb
            } else {
                dst[i] = op
            }
        }
        val out = ImageProcessingUtils.copyBitmap(bitmap)
        ImageProcessingUtils.setPixels(out, dst)
        if (blurred != out) blurred.recycle()
        return out
    }

    // ---------------------------------------------------------------- LUT application
    fun applyLut(bitmap: Bitmap, look: LutLook, strength: Float = 1f): Bitmap {
        if (look == LutLook.NATURAL && strength <= 0.01f) return bitmap
        val pixels = ImageProcessingUtils.getPixels(bitmap)
        val graded = look.apply(pixels, bitmap.width, bitmap.height, strength)
        val out = ImageProcessingUtils.copyBitmap(bitmap)
        ImageProcessingUtils.setPixels(out, graded)
        return out
    }

    // ---------------------------------------------------------------- Lens distortion correction
    fun correctLensDistortion(bitmap: Bitmap, k1: Float = 0.0f, k2: Float = 0.0f): Bitmap {
        if (k1 == 0f && k2 == 0f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val src = ImageProcessingUtils.getPixels(bitmap)
        val dst = IntArray(src.size)
        val cx = w / 2f
        val cy = h / 2f
        val rMax = sqrt(cx * cx + cy * cy)
        for (y in 0 until h) {
            val dy = (y - cy) / rMax
            val row = y * w
            for (x in 0 until w) {
                val dx = (x - cx) / rMax
                val r2 = dx * dx + dy * dy
                val r4 = r2 * r2
                val factor = 1f + k1 * r2 + k2 * r4
                val sx = (cx + dx * rMax * factor).toInt()
                val sy = (cy + dy * rMax * factor).toInt()
                dst[row + x] = if (sx in 0 until w && sy in 0 until h) src[sy * w + sx] else -0x1000000
            }
        }
        val out = ImageProcessingUtils.copyBitmap(bitmap)
        ImageProcessingUtils.setPixels(out, dst)
        return out
    }

    // ---------------------------------------------------------------- Auto scene enhance
    suspend fun autoSceneEnhance(bitmap: Bitmap, scene: String): Bitmap = when (scene.lowercase()) {
        "night", "low-light" -> enhance(bitmap, EnhancementLevel.AI_MAXIMUM)
        "golden hour", "sunset" -> enhance(bitmap, EnhancementLevel.CINEMATIC)
        "portrait" -> enhance(bitmap, EnhancementLevel.PROFESSIONAL)
        "landscape", "nature" -> enhance(bitmap, EnhancementLevel.ULTRA_DETAIL)
        else -> enhance(bitmap, EnhancementLevel.NATURAL)
    }
}
