package com.lumix.pro.camera

import android.hardware.camera2.CameraMetadata
import android.util.Range
import android.util.Size

/**
 * Verified hardware capabilities for a single camera sensor.
 * Every manual control slider and resolution option in LUMIX PRO is bounded
 * by these values so we never display controls or values the device does not support.
 */
data class CameraCapabilities(
    val cameraId: String,
    val facing: LensFacing,
    val hardwareLevelName: String,
    val isFullOrBetter: Boolean,
    val maxPhotoResolution: Size,
    val maxVideoResolutionName: String,
    val supportedPhotoResolutions: List<Size>,
    val supportedVideoQualities: List<String>,
    val isoRange: Range<Int>?,
    val exposureTimeRangeNs: Range<Long>?,
    val exposureCompensationRange: Range<Int>,
    val exposureCompensationStep: Float,
    val minimumFocusDistanceDiopters: Float?,
    val focalLengthsMm: List<Float>,
    val apertures: List<Float>,
    val opticalStabilizationSupported: Boolean,
    val videoStabilizationSupported: Boolean,
    val rawSensorSupported: Boolean,
    val supportedAwbModes: List<Int>,
    val supportedAeModes: List<Int>,
    val supportedAfModes: List<Int>,
    val supportedFpsRanges: List<Range<Int>>,
    val maxZoomRatio: Float,
    val physicalCameraIds: List<String>
)

enum class LensFacing {
    BACK, FRONT, EXTERNAL, UNKNOWN
}

data class SystemCameraInventory(
    val rearCamera: CameraCapabilities?,
    val frontCamera: CameraCapabilities?,
    val telephotoCamera: CameraCapabilities?,
    val ultrawideCamera: CameraCapabilities?,
    val allCameras: List<CameraCapabilities>
) {
    fun bestCameraFor(facing: LensFacing): CameraCapabilities? =
        allCameras.firstOrNull { it.facing == facing } ?: rearCamera ?: frontCamera
}
