package com.lumix.pro.camera

import android.content.ContentValues
import android.content.Context
import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.lumix.pro.modes.CameraMode
import com.lumix.pro.utils.PermissionHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalCamera2Interop::class)
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private val tag = "CameraController"
    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var camera: Camera? = null
    @Volatile private var cameraInitializationStarted = false

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var recordingStartTime = 0L

    private var previewSurfaceProvider: Preview.SurfaceProvider? = null
    private val hardwareManager = CameraHardwareManager(context)

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private val inventory by lazy(LazyThreadSafetyMode.NONE) { hardwareManager.discoverInventory() }
    private val analyzer = PreviewAnalyzer { frame -> onFrameAnalyzed(frame) }

    var onZebraFrame: ((android.graphics.Bitmap?) -> Unit)? = null
    var onFocusPeakingFrame: ((android.graphics.Bitmap?) -> Unit)? = null
    var onHistogramReady: ((IntArray) -> Unit)? = null

    init {
        _state.update {
            it.copy(
                activeFacing = LensFacing.BACK,
                exposureRange = Range(-12, 12),
                exposureStep = 1f / 3f,
                maxZoomRatio = 4f
            )
        }
        // CameraX is initialized only after MainActivity has obtained the
        // runtime CAMERA permission. Starting it in the constructor can crash
        // on a cold launch on devices that enforce permission checks.
    }

    private fun initCamera() {
        if (cameraInitializationStarted || !PermissionHandler.hasCameraPermission(context)) return
        cameraInitializationStarted = true
        runCatching {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                runCatching {
                    cameraProvider = providerFuture.get()
                    initExtensionsThenBind()
                }.onFailure { error ->
                    cameraInitializationStarted = false
                    Log.e(tag, "Failed to get ProcessCameraProvider", error)
                }
            }, ContextCompat.getMainExecutor(context))
        }.onFailure { error ->
            cameraInitializationStarted = false
            Log.e(tag, "CameraX initialization failed", error)
        }
    }

    private fun initExtensionsThenBind() {
        val provider = cameraProvider ?: return
        runCatching {
            val extFuture = ExtensionsManager.getInstanceAsync(context, provider)
            extFuture.addListener({
                extensionsManager = runCatching { extFuture.get() }.getOrNull()
                bindUseCases()
            }, ContextCompat.getMainExecutor(context))
        }.onFailure { error ->
            Log.w(tag, "Camera extensions unavailable; using standard CameraX", error)
            extensionsManager = null
            bindUseCases()
        }
    }

    fun attachPreview(surfaceProvider: Preview.SurfaceProvider) {
        previewSurfaceProvider = surfaceProvider
        preview?.setSurfaceProvider(surfaceProvider)
    }

    fun bindUseCases() {
        if (!PermissionHandler.hasCameraPermission(context)) return
        val provider = cameraProvider
        if (provider == null) {
            initCamera()
            return
        }
        runCatching { bindUseCasesInternal(provider) }
            .onFailure { error -> Log.e(tag, "Camera use-case setup failed; keeping UI alive", error) }
    }

    private fun bindUseCasesInternal(provider: ProcessCameraProvider) {
        val discovered = inventory
        val initialCaps = discovered.rearCamera ?: discovered.frontCamera
        if (_state.value.activeCapabilities == null && initialCaps != null) {
            _state.update {
                it.copy(
                    activeCapabilities = initialCaps,
                    exposureRange = initialCaps.exposureCompensationRange,
                    exposureStep = initialCaps.exposureCompensationStep,
                    maxZoomRatio = initialCaps.maxZoomRatio
                )
            }
        }
        val s = _state.value

        val baseSelector = when (s.activeFacing) {
            LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        val finalSelector = runCatching {
            applyExtensionSelector(baseSelector, s.activeMode) ?: baseSelector
        }.getOrDefault(baseSelector)

        // 1) Preview
        val previewBuilder = Preview.Builder()
        applyManualOptionsToPreview(previewBuilder)
        val prev = previewBuilder.build().also {
            previewSurfaceProvider?.let { sp -> it.setSurfaceProvider(sp) }
        }

        // 2) ImageCapture
        val flashMode = when (s.flashOption) {
            FlashOption.ON -> ImageCapture.FLASH_MODE_ON
            FlashOption.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashOption.OFF, FlashOption.TORCH -> ImageCapture.FLASH_MODE_OFF
        }
        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
        applyManualOptionsToCapture(captureBuilder)
        val imgCapture = captureBuilder.build()

        // 3) ImageAnalysis
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analyzer.zebraEnabled = s.zebraEnabled
        analyzer.focusPeakingEnabled = s.focusPeakingEnabled
        analysis.setAnalyzer(executor, analyzer)

        // 4) VideoCapture (Recorder)
        val quality = when (s.videoQuality) {
            "4K UHD" -> QualitySelector.from(Quality.UHD, FallbackStrategy.higherQualityOrLowerThan(Quality.FHD))
            "1080p FHD" -> QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityThan(Quality.FHD))
            "720p HD" -> QualitySelector.from(Quality.HD)
            else -> QualitySelector.from(Quality.SD)
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(quality)
            .setExecutor(executor)
            .build()
        val vidCapture = VideoCapture.withOutput(recorder)

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                finalSelector,
                prev,
                imgCapture,
                analysis,
                vidCapture
            )
            preview = prev
            imageCapture = imgCapture
            imageAnalysis = analysis
            videoCapture = vidCapture
            applyTorchState()
            observeZoomAndExposure()
        } catch (e: Exception) {
            Log.e(tag, "Binding failed, retrying without extensions", e)
            try {
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    baseSelector,
                    prev,
                    imgCapture,
                    analysis
                )
                preview = prev
                imageCapture = imgCapture
                imageAnalysis = analysis
                observeZoomAndExposure()
            } catch (t: Throwable) {
                Log.e(tag, "Camera fallback binding failed", t)
            }
        }
    }

    private fun applyExtensionSelector(base: CameraSelector, mode: CameraMode): CameraSelector? {
        val ext = extensionsManager ?: return null
        val extensionMode = when (mode) {
            CameraMode.HDR -> ExtensionMode.HDR
            CameraMode.NIGHT -> ExtensionMode.NIGHT
            CameraMode.PORTRAIT -> ExtensionMode.BOKEH
            CameraMode.DOCUMENT -> ExtensionMode.FACE_RETOUCH
            else -> null
        } ?: return null
        return if (ext.isExtensionAvailable(base, extensionMode)) {
            ext.getExtensionEnabledCameraSelector(base, extensionMode)
        } else null
    }

    private fun applyManualOptionsToPreview(builder: Preview.Builder) {
        val s = _state.value
        if (s.activeMode != CameraMode.PRO && !s.hdrEnabled) return
        val ext = Camera2Interop.Extender(builder)
        s.manualIso?.let { iso ->
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            ext.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        }
        s.manualShutterNs?.let { ns ->
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            ext.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, ns)
        }
        s.manualFocusDistance?.let { fd ->
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
            ext.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, fd)
        }
        s.whiteBalanceKelvin?.let {
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
        }
        if (s.aeLocked) ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
        if (s.awbLocked) ext.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
    }

    private fun applyManualOptionsToCapture(builder: ImageCapture.Builder) {
        val s = _state.value
        if (s.activeMode != CameraMode.PRO) return
        val ext = Camera2Interop.Extender(builder)
        s.manualIso?.let { iso ->
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            ext.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        }
        s.manualShutterNs?.let { ns ->
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            ext.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, ns)
        }
        s.manualFocusDistance?.let { fd ->
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
            ext.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, fd)
        }
    }

    private fun observeZoomAndExposure() {
        val cam = camera ?: return
        cam.cameraInfo.zoomState.observe(lifecycleOwner) { zs ->
            _state.update {
                it.copy(
                    zoomRatio = zs.zoomRatio,
                    maxZoomRatio = zs.maxZoomRatio
                )
            }
        }
        cam.cameraInfo.exposureState.let { es ->
            _state.update {
                it.copy(
                    exposureIndex = es.exposureCompensationIndex,
                    exposureRange = es.exposureCompensationRange,
                    exposureStep = es.exposureCompensationStep.toFloat()
                )
            }
        }
    }

    private fun applyTorchState() {
        val torch = _state.value.flashOption == FlashOption.TORCH
        camera?.cameraControl?.enableTorch(torch)
    }

    fun setZoom(ratio: Float) {
        val max = _state.value.maxZoomRatio
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(1f, max))
    }

    fun setExposureIndex(index: Int) {
        val r = _state.value.exposureRange
        val clamped = index.coerceIn(r.lower, r.upper)
        camera?.cameraControl?.setExposureCompensationIndex(clamped)
        _state.update { it.copy(exposureIndex = clamped) }
    }

    fun setManualIso(iso: Int?) {
        _state.update { it.copy(manualIso = iso) }
        bindUseCases()
    }

    fun setManualShutterNs(ns: Long?) {
        _state.update { it.copy(manualShutterNs = ns) }
        bindUseCases()
    }

    fun setManualFocusDistance(fd: Float?) {
        _state.update { it.copy(manualFocusDistance = fd) }
        bindUseCases()
    }

    fun setWhiteBalanceKelvin(kelvin: Int?) {
        _state.update { it.copy(whiteBalanceKelvin = kelvin) }
        bindUseCases()
    }

    fun setAeLocked(locked: Boolean) {
        _state.update { it.copy(aeLocked = locked) }
        bindUseCases()
    }

    fun setAwbLocked(locked: Boolean) {
        _state.update { it.copy(awbLocked = locked) }
        bindUseCases()
    }

    fun setMode(mode: CameraMode) {
        _state.update { it.copy(activeMode = mode) }
        bindUseCases()
    }

    fun switchCamera() {
        val nextFacing = when (_state.value.activeFacing) {
            LensFacing.BACK -> LensFacing.FRONT
            LensFacing.FRONT -> LensFacing.BACK
            else -> LensFacing.BACK
        }
        val caps = inventory.bestCameraFor(nextFacing)
        _state.update {
            it.copy(
                activeFacing = nextFacing,
                activeCapabilities = caps,
                zoomRatio = 1f,
                exposureIndex = 0,
                manualIso = null,
                manualShutterNs = null,
                manualFocusDistance = null,
                aeLocked = false,
                awbLocked = false
            )
        }
        bindUseCases()
    }

    fun cycleFlash() {
        val next = when (_state.value.flashOption) {
            FlashOption.AUTO -> FlashOption.ON
            FlashOption.ON -> FlashOption.OFF
            FlashOption.OFF -> FlashOption.TORCH
            FlashOption.TORCH -> FlashOption.AUTO
        }
        _state.update { it.copy(flashOption = next) }
        bindUseCases()
    }

    fun cycleTimer() {
        val next = when (_state.value.timerSeconds) {
            0 -> 3
            3 -> 5
            5 -> 10
            else -> 0
        }
        _state.update { it.copy(timerSeconds = next) }
    }

    fun cycleGrid() {
        val next = when (_state.value.gridType) {
            GridType.NONE -> GridType.RULE_OF_THIRDS
            GridType.RULE_OF_THIRDS -> GridType.GOLDEN_RATIO
            GridType.GOLDEN_RATIO -> GridType.DIAGONALS
            GridType.DIAGONALS -> GridType.KEYSTONE
            GridType.KEYSTONE -> GridType.NONE
        }
        _state.update { it.copy(gridType = next) }
    }

    fun cycleAspectRatio() {
        val next = when (_state.value.aspectRatio) {
            AspectRatioOption.RATIO_4_3 -> AspectRatioOption.RATIO_16_9
            AspectRatioOption.RATIO_16_9 -> AspectRatioOption.RATIO_1_1
            AspectRatioOption.RATIO_1_1 -> AspectRatioOption.FULL
            AspectRatioOption.FULL -> AspectRatioOption.RATIO_4_3
        }
        _state.update { it.copy(aspectRatio = next) }
    }

    fun toggleLevel() = _state.update { it.copy(levelEnabled = !it.levelEnabled) }
    fun toggleHistogram() = _state.update { it.copy(histogramEnabled = !it.histogramEnabled) }

    fun toggleFocusPeaking() {
        _state.update { it.copy(focusPeakingEnabled = !it.focusPeakingEnabled) }
        analyzer.focusPeakingEnabled = _state.value.focusPeakingEnabled
    }

    fun toggleZebra() {
        _state.update { it.copy(zebraEnabled = !it.zebraEnabled) }
        analyzer.zebraEnabled = _state.value.zebraEnabled
    }

    fun toggleHdr() {
        _state.update { it.copy(hdrEnabled = !it.hdrEnabled) }
        bindUseCases()
    }

    fun toggleRaw() = _state.update { it.copy(rawEnabled = !it.rawEnabled) }

    fun setBurstCount(count: Int) = _state.update { it.copy(burstCount = count.coerceIn(2, 30)) }

    fun tapToFocus(normX: Float, normY: Float) {
        val cam = camera ?: return
        val pf = previewSurfaceProvider as? androidx.camera.view.PreviewView
        val factory = pf?.meteringPointFactory ?: return
        val meteringPoint = factory.createPoint(normX, normY)
        val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    fun capturePhoto(onSuccess: (Uri, String) -> Unit = { _, _ -> }, onError: (Throwable) -> Unit = {}) {
        val t = _state.value.timerSeconds
        if (t > 0) {
            scope.launch {
                for (s in t downTo 1) {
                    _state.update { it.copy(timerCountdown = s) }
                    delay(1000)
                }
                _state.update { it.copy(timerCountdown = null) }
                performCapture(onSuccess, onError)
            }
        } else {
            performCapture(onSuccess, onError)
        }
    }

    private fun performCapture(onSuccess: (Uri, String) -> Unit, onError: (Throwable) -> Unit) {
        val ic = imageCapture ?: return onError(IllegalStateException("Camera not ready"))
        _state.update { it.copy(isCapturing = true) }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "LUMIX_$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/LUMIX_PRO")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val options = OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            cv
        ).build()

        ic.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                _state.update { it.copy(isCapturing = false) }
                val uri = output.savedUri ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val finish = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    runCatching { context.contentResolver.update(uri, finish, null, null) }
                }
                val path = resolveRealPath(uri) ?: uri.toString()
                _state.update { it.copy(lastCapturedUri = uri, lastCapturedPath = path) }
                onSuccess(uri, path)
            }

            override fun onError(exception: ImageCaptureException) {
                _state.update { it.copy(isCapturing = false) }
                Log.e(tag, "Capture failed", exception)
                onError(exception)
            }
        })
    }

    fun burstCapture(onBurstDone: (List<Uri>) -> Unit) {
        val total = _state.value.burstCount
        val results = mutableListOf<Uri>()
        _state.update { it.copy(isCapturing = true) }

        fun next(shot: Int) {
            if (shot >= total) {
                _state.update { it.copy(isCapturing = false, captureProgress = null) }
                onBurstDone(results)
                return
            }
            _state.update { it.copy(captureProgress = (shot + 1f) / total) }
            performCapture(onSuccess = { uri, _ ->
                results += uri
                next(shot + 1)
            }, onError = {
                _state.update { it.copy(isCapturing = false, captureProgress = null) }
                onBurstDone(results)
            })
        }
        next(0)
    }

    @SuppressLint("MissingPermission")
    fun startVideoRecording(onSaved: (Uri) -> Unit) {
        val vc = videoCapture ?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cv = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "LUMIX_$name.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LUMIX_PRO")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(cv).build()

        var recBuilder = vc.output.prepareRecording(context, mediaStoreOutput)
        if (_state.value.audioEnabled && PermissionHandler.hasAudioPermission(context)) {
            recBuilder = recBuilder.withAudioEnabled()
        }

        recordingStartTime = SystemClock.elapsedRealtime()
        activeRecording = recBuilder.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _state.update { it.copy(isRecording = true, isRecordingPaused = false) }
                }
                is VideoRecordEvent.Pause -> _state.update { it.copy(isRecordingPaused = true) }
                is VideoRecordEvent.Resume -> _state.update { it.copy(isRecordingPaused = false) }
                is VideoRecordEvent.Status -> {
                    val stats = event.recordingStats
                    _state.update {
                        it.copy(
                            recordingDurationMs = stats.recordedDurationNanos / 1_000_000L
                        )
                    }
                }
                is VideoRecordEvent.Finalize -> {
                    _state.update {
                        it.copy(
                            isRecording = false,
                            isRecordingPaused = false,
                            recordingDurationMs = 0L
                        )
                    }
                    if (!event.hasError()) {
                        val uri = event.outputResults.outputUri
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val finish = ContentValues().apply {
                                put(MediaStore.Video.Media.IS_PENDING, 0)
                            }
                            runCatching { context.contentResolver.update(uri, finish, null, null) }
                        }
                        onSaved(uri)
                    } else {
                        Log.e(tag, "Video finalize error: ${event.error}")
                    }
                    activeRecording = null
                }
            }
        }
    }

    fun pauseVideoRecording() = activeRecording?.pause()
    fun resumeVideoRecording() = activeRecording?.resume()
    fun stopVideoRecording() = activeRecording?.stop()

    private fun resolveRealPath(uri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        return runCatching {
            context.contentResolver.query(uri, proj, null, null, null)?.use { c ->
                val col = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (c.moveToFirst()) c.getString(col) else null
            }
        }.getOrNull()
    }

    private fun onFrameAnalyzed(f: FrameAnalysisResult) {
        onHistogramReady?.invoke(f.histogram)
        onZebraFrame?.invoke(f.zebraBitmap)
        onFocusPeakingFrame?.invoke(f.focusPeakingBitmap)
        _state.update {
            it.copy(
                shakeWarning = f.motionMetric > 28f,
                overexposedWarning = f.overexposedFraction > 0.10f,
                lowLightWarning = f.underexposedFraction > 0.40f,
                goldenHourActive = f.colorTempEstimateK in 2400..3600 && f.meanLuma in 70f..170f
            )
        }
    }

    fun updateRollPitch(roll: Float, pitch: Float) {
        _state.update { it.copy(rollDegrees = roll, pitchDegrees = pitch) }
    }

    fun updateAudioLevel(rms: Float) = _state.update { it.copy(audioLevelRms = rms) }
    fun updateAssistant(desc: String, rec: com.lumix.pro.assistant.AssistantRecommendation?) {
        _state.update { it.copy(sceneDescriptor = desc, assistantRecommendation = rec) }
    }
    fun updateSystem(storageMb: Long, battery: Int, thermal: Boolean) {
        _state.update {
            it.copy(
                storageFreeMb = storageMb,
                batteryPercent = battery,
                thermalThrottled = thermal
            )
        }
    }

    fun release() {
        stopVideoRecording()
        runCatching { cameraProvider?.unbindAll() }
        executor.shutdown()
        scope.cancel()
    }
}
