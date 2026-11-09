package com.lumix.pro.assistant

import com.lumix.pro.camera.CameraController
import com.lumix.pro.camera.FrameAnalysisResult
import com.lumix.pro.camera.LensFacing
import com.lumix.pro.modes.CameraMode
import kotlin.math.max
import kotlin.math.min

class AiPhotographyAssistant {

    var isArabic: Boolean = false

    fun analyze(f: FrameAnalysisResult): Pair<String, AssistantRecommendation?> {
        val scene = SceneClassifier.classify(f)
        val rec = recommendationFor(scene, f)
        val desc = if (isArabic) scene.titleAr else scene.title
        return desc to rec
    }

    private fun recommendationFor(scene: SceneType, f: FrameAnalysisResult): AssistantRecommendation = when (scene) {
        SceneType.NIGHT -> AssistantRecommendation(
            scene = "Night",
            tip = "Use Night mode or Pro ISO 1600 · 1/8s · Night stack recommended",
            tipAr = "استخدم الوضع الليلي أو ISO 1600 · 1/8 ثانية مع دمج اللقطات",
            recommendedIso = 1600,
            recommendedShutterNs = 125_000_000L,
            recommendedMode = CameraMode.NIGHT,
            recommendedFacing = LensFacing.BACK
        )
        SceneType.LOW_LIGHT -> AssistantRecommendation(
            scene = "Low Light",
            tip = "Increase ISO or switch to Night mode; keep hands steady",
            tipAr = "ارفع ISO أو بدّل للوضع الليلي وثبّت يدك جيدًا",
            recommendedIso = 800,
            recommendedShutterNs = 33_333_333L,
            recommendedMode = CameraMode.NIGHT
        )
        SceneType.GOLDEN_HOUR -> AssistantRecommendation(
            scene = "Golden Hour",
            tip = "Warm cinematic light · EV -0.5 preserves highlight gradients",
            tipAr = "إضاءة سينمائية دافئة · تعريض -0.5 يحفظ تدرجات الضوء",
            recommendedEv = -1,
            recommendedKelvin = 4800,
            recommendedMode = CameraMode.LANDSCAPE
        )
        SceneType.SUNSET -> AssistantRecommendation(
            scene = "Sunset",
            tip = "Sunset scene · HDR mode or EV -1 keeps sky saturated",
            tipAr = "مشهد غروب · وضع HDR أو تعريض -1 يحافظ على تشبع السماء",
            recommendedEv = -2,
            recommendedMode = CameraMode.HDR
        )
        SceneType.OVEREXPOSED -> AssistantRecommendation(
            scene = "Overexposed",
            tip = "Highlights clipping · Lower EV or increase shutter speed",
            tipAr = "قص في المناطق الساطعة · قلل التعريض أو ارفع سرعة الغالق",
            recommendedEv = -2,
            recommendedIso = 100
        )
        SceneType.BACKLIT -> AssistantRecommendation(
            scene = "Backlit",
            tip = "Backlit subject · Enable HDR or +1 EV fill-light bias",
            tipAr = "إضاءة من الخلف · فعّل HDR أو ارفع التعريض +1",
            recommendedEv = 2,
            recommendedMode = CameraMode.HDR
        )
        SceneType.SHAKE -> AssistantRecommendation(
            scene = "Motion Blur Risk",
            tip = "Camera motion detected · Shutter 1/500s freezes movement",
            tipAr = "حركة واهتزاز · غالق 1/500 ثانية يجمّد الحركة",
            recommendedShutterNs = 2_000_000L,
            recommendedMode = CameraMode.SPORTS
        )
        SceneType.TEXT_DOCUMENT -> AssistantRecommendation(
            scene = "Document",
            tip = "Document detected · Document Scanner mode maximizes contrast",
            tipAr = "مستند مكتشف · وضع ماسح المستندات يعظم التباين والوضوح",
            recommendedMode = CameraMode.DOCUMENT
        )
        SceneType.DAYLIGHT -> AssistantRecommendation(
            scene = "Daylight",
            tip = "Good balanced light · Base ISO 100 delivers cleanest pixels",
            tipAr = "إضاءة متوازنة ممتازة · ISO 100 يمنح أنقى تفاصيل",
            recommendedIso = 100,
            recommendedMode = CameraMode.AUTO
        )
    }

    fun applyRecommendation(controller: CameraController, rec: AssistantRecommendation) {
        rec.recommendedMode?.let { controller.setMode(it) }
        rec.recommendedIso?.let { controller.setManualIso(it) }
        rec.recommendedShutterNs?.let { controller.setManualShutterNs(it) }
        rec.recommendedEv?.let { controller.setExposureIndex(it) }
        rec.recommendedKelvin?.let { controller.setWhiteBalanceKelvin(it) }
    }
}
