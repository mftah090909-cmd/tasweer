package com.lumix.pro.assistant

import com.lumix.pro.camera.LensFacing
import com.lumix.pro.modes.CameraMode

data class AssistantRecommendation(
    val scene: String,
    val tip: String,
    val tipAr: String,
    val recommendedIso: Int? = null,
    val recommendedShutterNs: Long? = null,
    val recommendedEv: Int? = null,
    val recommendedFacing: LensFacing? = null,
    val recommendedMode: CameraMode? = null,
    val recommendedKelvin: Int? = null
)
