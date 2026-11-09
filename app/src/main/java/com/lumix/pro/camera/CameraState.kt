package com.lumix.pro.camera

import android.net.Uri
import android.util.Range
import com.lumix.pro.assistant.AssistantRecommendation
import com.lumix.pro.modes.CameraMode

enum class FlashOption {
    AUTO, ON, OFF, TORCH
}

enum class GridType {
    NONE, RULE_OF_THIRDS, GOLDEN_RATIO, DIAGONALS, KEYSTONE
}

enum class AspectRatioOption(val label: String, val ratio: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_1_1("1:1", 1f),
    FULL("FULL", 0f)
}

data class CameraState(
    val activeFacing: LensFacing = LensFacing.BACK,
    val activeMode: CameraMode = CameraMode.AUTO,
    val zoomRatio: Float = 1f,
    val maxZoomRatio: Float = 4f,
    val flashOption: FlashOption = FlashOption.AUTO,
    val exposureIndex: Int = 0,
    val exposureRange: Range<Int> = Range(-12, 12),
    val exposureStep: Float = 1f / 3f,
    val manualIso: Int? = null,
    val manualShutterNs: Long? = null,
    val whiteBalanceKelvin: Int? = null,
    val manualFocusDistance: Float? = null,
    val aeLocked: Boolean = false,
    val awbLocked: Boolean = false,
    val timerSeconds: Int = 0,
    val timerCountdown: Int? = null,
    val gridType: GridType = GridType.RULE_OF_THIRDS,
    val levelEnabled: Boolean = true,
    val histogramEnabled: Boolean = true,
    val focusPeakingEnabled: Boolean = false,
    val zebraEnabled: Boolean = false,
    val hdrEnabled: Boolean = false,
    val rawEnabled: Boolean = false,
    val burstCount: Int = 5,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val videoQuality: String = "1080p FHD",
    val videoStabilization: Boolean = true,
    val audioEnabled: Boolean = true,
    val frameRate: Int = 30,
    val isCapturing: Boolean = false,
    val captureProgress: Float? = null,
    val lastCapturedUri: Uri? = null,
    val lastCapturedPath: String? = null,
    val sceneDescriptor: String = "",
    val assistantRecommendation: AssistantRecommendation? = null,
    val shakeWarning: Boolean = false,
    val overexposedWarning: Boolean = false,
    val lowLightWarning: Boolean = false,
    val goldenHourActive: Boolean = false,
    val storageFreeMb: Long = 0L,
    val batteryPercent: Int = 100,
    val thermalThrottled: Boolean = false,
    val rollDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val audioLevelRms: Float = 0f,
    val activeCapabilities: CameraCapabilities? = null
) {
    val isVideoMode: Boolean get() = activeMode.isVideoMode
}
