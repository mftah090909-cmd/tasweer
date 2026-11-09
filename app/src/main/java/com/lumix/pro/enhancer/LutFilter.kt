package com.lumix.pro.enhancer

enum class LutLook(
    val title: String,
    val titleAr: String,
    val tempShift: Float,      // positive = warm
    val tintShift: Float,      // positive = magenta
    val saturationMul: Float,
    val curve: ToneCurve,
    val rMul: Float = 1f,
    val gMul: Float = 1f,
    val bMul: Float = 1f,
    val shadowTintB: Float = 0f,
    val highlightTintR: Float = 0f,
    val desatBand: ColorBand? = null
) {
    NATURAL("Original", "الأصلي", 0f, 0f, 1.0f, ToneCurve.LINEAR),
    PRO_COLOR("Pro Color", "لون احترافي", 4f, -2f, 1.08f, ToneCurve.CONTRAST, 1.02f, 1.0f, 0.98f),
    AI_VIVID("AI Vivid", "حيوي ذكي", 2f, 0f, 1.25f, ToneCurve.CONTRAST, 1.05f, 1.03f, 1.02f),
    CINEMATIC("Cinematic", "سينمائي", 12f, -6f, 1.1f, ToneCurve.FILM, 1.06f, 0.98f, 0.92f, shadowTintB = 0.12f, highlightTintR = 0.10f),
    TEAL_ORANGE("Teal & Orange", "تيل وأورانج", 14f, -4f, 1.15f, ToneCurve.CONTRAST, 1.12f, 0.94f, 0.88f, shadowTintB = 0.18f, highlightTintR = 0.14f),
    VINTAGE_FILM("Vintage Film", "فيلم كلاسيكي", 18f, 10f, 0.85f, ToneCurve.LIFT_SHADOWS, 1.10f, 1.02f, 0.82f),
    BW_CONTRAST("B&W Punch", "أبيض وأسود قوي", 0f, 0f, 0f, ToneCurve.CONTRAST),
    WARM_SUNSET("Warm Sunset", "غروب دافئ", 24f, 4f, 1.2f, ToneCurve.FILM, 1.16f, 0.98f, 0.80f),
    COLD_MOODY("Cold Moody", "بارد درامي", -22f, -8f, 0.95f, ToneCurve.CONTRAST, 0.88f, 0.96f, 1.16f, shadowTintB = 0.22f),
    FOREST("Forest", "غابات", 2f, -14f, 1.18f, ToneCurve.CONTRAST, 0.92f, 1.14f, 0.94f),
    PORTRAIT_GLOW("Portrait Glow", "تألق البورتريه", 8f, 6f, 1.02f, ToneCurve.LIFT_SHADOWS, 1.06f, 1.02f, 1.0f),
    CYBERPUNK("Cyberpunk", "سايبربانك", -8f, 22f, 1.35f, ToneCurve.CONTRAST, 1.08f, 0.86f, 1.22f);

    fun localizedTitle(isArabic: Boolean): String = if (isArabic) titleAr else title

    fun apply(pixels: IntArray, w: Int, h: Int, strength: Float = 1f): IntArray {
        if (strength <= 0.001f && this == NATURAL) return pixels
        val out = IntArray(pixels.size)
        val lumaR = 0.299f
        val lumaG = 0.587f
        val lumaB = 0.114f

        for (i in pixels.indices) {
            val px = pixels[i]
            val a = (px ushr 24) and 0xFF
            var r = (px ushr 16) and 0xFF
            var g = (px ushr 8) and 0xFF
            var b = px and 0xFF

            // 1) Tone curve
            r = curve.apply(r)
            g = curve.apply(g)
            b = curve.apply(b)

            // 2) Channel multipliers
            var rf = (r * rMul)
            var gf = (g * gMul)
            var bf = (b * bMul)

            // 3) Temperature & tint shift
            rf += tempShift * 0.8f + tintShift * 0.35f
            gf += -tintShift * 0.45f
            bf += -tempShift * 0.8f + tintShift * 0.35f

            // 4) Split toning: shadows cooled (cyan), highlights warmed
            val luma = (rf * lumaR + gf * lumaG + bf * lumaB) / 255f
            val shadowWeight = (1f - luma).coerceIn(0f, 1f)
            val highlightWeight = luma.coerceIn(0f, 1f)
            bf += shadowWeight * shadowTintB * 160f
            gf += shadowWeight * shadowTintB * 60f
            rf += highlightWeight * highlightTintR * 140f

            // 5) Saturation
            val l = rf * lumaR + gf * lumaG + bf * lumaB
            rf = l + (rf - l) * saturationMul
            gf = l + (gf - l) * saturationMul
            bf = l + (bf - l) * saturationMul

            // 6) Blend with original by strength
            val or_ = (px ushr 16) and 0xFF
            val og = (px ushr 8) and 0xFF
            val ob = px and 0xFF
            val finalR = (or_ + (rf - or_) * strength).toInt().coerceIn(0, 255)
            val finalG = (og + (gf - og) * strength).toInt().coerceIn(0, 255)
            val finalB = (ob + (bf - ob) * strength).toInt().coerceIn(0, 255)

            out[i] = (a shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
        }
        return out
    }
}
