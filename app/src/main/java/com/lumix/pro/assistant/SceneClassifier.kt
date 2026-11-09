package com.lumix.pro.assistant

import com.lumix.pro.camera.FrameAnalysisResult

enum class SceneType(val title: String, val titleAr: String) {
    NIGHT("Night", "ليلي"),
    LOW_LIGHT("Low Light", "إضاءة منخفضة"),
    GOLDEN_HOUR("Golden Hour", "الساعة الذهبية"),
    SUNSET("Sunset", "غروب"),
    DAYLIGHT("Daylight", "ضوء النهار"),
    OVEREXPOSED("Overexposed", "تعريض مفرط"),
    BACKLIT("Backlit Scene", "إضاءة خلفية"),
    SHAKE("Motion / Shake", "حركة واهتزاز"),
    TEXT_DOCUMENT("Document / Text", "مستند ونصوص")
}

object SceneClassifier {

    fun classify(f: FrameAnalysisResult): SceneType {
        // High motion wins immediate attention
        if (f.motionMetric > 34f) return SceneType.SHAKE

        // Overexposure
        if (f.overexposedFraction > 0.12f) return SceneType.OVEREXPOSED

        // Night / very low light
        if (f.meanLuma < 26f || f.underexposedFraction > 0.45f) return SceneType.NIGHT
        if (f.meanLuma < 52f || f.underexposedFraction > 0.30f) return SceneType.LOW_LIGHT

        // Golden hour / sunset: warm color temperature + medium luminance
        if (f.colorTempEstimateK in 2300..3400 && f.meanLuma in 60f..180f) {
            return if (f.meanLuma < 110f) SceneType.SUNSET else SceneType.GOLDEN_HOUR
        }

        // Backlit: high contrast with significant under AND over exposure
        if (f.rmsContrast > 70f && f.overexposedFraction > 0.04f && f.underexposedFraction > 0.12f) {
            return SceneType.BACKLIT
        }

        // Document-like: high contrast, bimodal histogram, cool color temp
        if (f.rmsContrast > 65f && bimodalScore(f.histogram) > 0.5f) {
            return SceneType.TEXT_DOCUMENT
        }

        return SceneType.DAYLIGHT
    }

    private fun bimodalScore(hist: IntArray): Float {
        // High counts near both ends vs midtones indicates document/text
        val dark = (0..50).sumOf { hist[it].toLong() }
        val light = (205..255).sumOf { hist[it].toLong() }
        val mid = (80..175).sumOf { hist[it].toLong() }
        val total = (dark + light + mid).coerceAtLeast(1)
        return (dark + light - mid).toFloat() / total
    }
}
