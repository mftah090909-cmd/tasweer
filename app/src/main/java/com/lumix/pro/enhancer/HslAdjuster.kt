package com.lumix.pro.enhancer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class ColorBand(val centerHue: Float, val bandWidth: Float) {
    RED(0f, 30f),
    ORANGE(30f, 30f),
    YELLOW(60f, 30f),
    GREEN(120f, 60f),
    CYAN(180f, 40f),
    BLUE(225f, 45f),
    PURPLE(285f, 40f),
    MAGENTA(330f, 40f)
}

data class HslBandAdjustment(
    val hueShift: Float = 0f,       // -30 .. +30 degrees
    val saturationDelta: Float = 0f,// -1.0 .. +1.0
    val luminanceDelta: Float = 0f  // -1.0 .. +1.0
)

class HslAdjuster(
    val adjustments: Map<ColorBand, HslBandAdjustment> = emptyMap()
) {
    fun applyRgb(r: Int, g: Int, b: Int): IntArray {
        if (adjustments.isEmpty()) return intArrayOf(r, g, b)
        val hsl = rgbToHsl(r, g, b)
        var h = hsl[0]
        var s = hsl[1]
        var l = hsl[2]

        for ((band, adj) in adjustments) {
            val w = bandWeight(h, band.centerHue, band.bandWidth)
            if (w <= 0f) continue
            h = (h + adj.hueShift * w + 360f) % 360f
            s = (s + adj.saturationDelta * w).coerceIn(0f, 1f)
            l = (l + adj.luminanceDelta * w * 0.5f).coerceIn(0f, 1f)
        }
        return hslToRgb(h, s, l)
    }

    private fun bandWeight(hue: Float, center: Float, width: Float): Float {
        var d = abs(hue - center)
        if (d > 180f) d = 360f - d
        val t = 1f - (d / width)
        return if (t <= 0f) 0f else t * t * (3f - 2f * t) // smoothstep
    }

    companion object {
        fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
            val rf = r / 255f
            val gf = g / 255f
            val bf = b / 255f
            val mx = max(rf, max(gf, bf))
            val mn = min(rf, min(gf, bf))
            var h: Float
            val s: Float
            val l = (mx + mn) / 2f

            if (mx == mn) {
                h = 0f
                s = 0f
            } else {
                val d = mx - mn
                s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
                h = when (mx) {
                    rf -> ((gf - bf) / d + (if (gf < bf) 6f else 0f))
                    gf -> ((bf - rf) / d + 2f)
                    else -> ((rf - gf) / d + 4f)
                } * 60f
            }
            return floatArrayOf(h, s, l)
        }

        fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
            if (s == 0f) {
                val v = (l * 255f + 0.5f).toInt().coerceIn(0, 255)
                return intArrayOf(v, v, v)
            }
            val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
            val p = 2f * l - q
            val hk = h / 360f
            fun hue2rgb(t0: Float): Float {
                var t = t0
                if (t < 0f) t += 1f
                if (t > 1f) t -= 1f
                return when {
                    t < 1f / 6f -> p + (q - p) * 6f * t
                    t < 1f / 2f -> q
                    t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                    else -> p
                }
            }
            val r = (hue2rgb(hk + 1f / 3f) * 255f + 0.5f).toInt().coerceIn(0, 255)
            val g = (hue2rgb(hk) * 255f + 0.5f).toInt().coerceIn(0, 255)
            val b = (hue2rgb(hk - 1f / 3f) * 255f + 0.5f).toInt().coerceIn(0, 255)
            return intArrayOf(r, g, b)
        }
    }
}
