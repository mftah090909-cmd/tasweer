package com.lumix.pro.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Range
import android.util.Size

class CameraHardwareManager(private val context: Context) {

    private val cameraManager: CameraManager? =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    fun discoverInventory(): SystemCameraInventory {
        val manager = cameraManager ?: return SystemCameraInventory(
            rearCamera = null,
            frontCamera = null,
            telephotoCamera = null,
            ultrawideCamera = null,
            allCameras = emptyList()
        )

        val all = runCatching {
            manager.cameraIdList.mapNotNull { id -> readCapabilities(manager, id) }
        }.getOrDefault(emptyList())

        val rearCameras = all.filter { it.facing == LensFacing.BACK }
        val frontCamera = all.firstOrNull { it.facing == LensFacing.FRONT }

        // Distinguish main vs ultrawide vs telephoto by native focal length if multiple rear cameras
        val sortedRear = rearCameras.sortedBy { it.focalLengthsMm.firstOrNull() ?: Float.MAX_VALUE }
        val rearCamera = sortedRear.getOrNull(0)
        val ultrawideCamera = if (sortedRear.size > 2) sortedRear.firstOrNull() else null
        val telephotoCamera = if (sortedRear.size > 1) sortedRear.lastOrNull() else null

        return SystemCameraInventory(
            rearCamera = rearCamera,
            frontCamera = frontCamera,
            telephotoCamera = telephotoCamera,
            ultrawideCamera = ultrawideCamera,
            allCameras = all
        )
    }

    private fun readCapabilities(manager: CameraManager, id: String): CameraCapabilities? {
        val chars = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: return null

        val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
            CameraMetadata.LENS_FACING_BACK -> LensFacing.BACK
            CameraMetadata.LENS_FACING_FRONT -> LensFacing.FRONT
            CameraMetadata.LENS_FACING_EXTERNAL -> LensFacing.EXTERNAL
            else -> LensFacing.UNKNOWN
        }

        val hwLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val (hwName, isFull) = when (hwLevel) {
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED" to false
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL" to true
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3" to true
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL" to false
            else -> "LEGACY" to false
        }

        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val photoSizes = map?.getOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()
        val maxPhoto = photoSizes.maxByOrNull { it.width.toLong() * it.height.toLong() }
            ?: Size(1920, 1080)

        val videoQualities = buildList {
            val maxPixels = maxPhoto.width * maxPhoto.height
            if (maxPixels >= 3840 * 2160) add("4K UHD")
            add("1080p FHD")
            add("720p HD")
            add("480p SD")
        }

        val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposureTimeRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val evRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            ?: Range(-24, 24)
        val evStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.toFloat()
            ?: (1f / 3f)
        val minFocus = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList().orEmpty()
        val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList().orEmpty()

        val opticalOis = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.any {
            it == CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
        } ?: false

        val videoStab = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.any {
            it == CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
        } ?: false

        val rawSupported = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.any {
            it == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
        } ?: false

        val awb = chars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toList().orEmpty()
        val ae = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)?.toList().orEmpty()
        val af = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.toList().orEmpty()
        val fps = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.toList().orEmpty()

        val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 4f

        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            chars.physicalCameraIds?.toList().orEmpty()
        } else {
            emptyList()
        }

        return CameraCapabilities(
            cameraId = id,
            facing = facing,
            hardwareLevelName = hwName,
            isFullOrBetter = isFull,
            maxPhotoResolution = maxPhoto,
            maxVideoResolutionName = videoQualities.firstOrNull() ?: "1080p FHD",
            supportedPhotoResolutions = photoSizes,
            supportedVideoQualities = videoQualities,
            isoRange = isoRange,
            exposureTimeRangeNs = exposureTimeRange,
            exposureCompensationRange = evRange,
            exposureCompensationStep = evStep,
            minimumFocusDistanceDiopters = minFocus,
            focalLengthsMm = focalLengths,
            apertures = apertures,
            opticalStabilizationSupported = opticalOis,
            videoStabilizationSupported = videoStab,
            rawSensorSupported = rawSupported,
            supportedAwbModes = awb,
            supportedAeModes = ae,
            supportedAfModes = af,
            supportedFpsRanges = fps,
            maxZoomRatio = maxZoom,
            physicalCameraIds = physicalIds
        )
    }
}
