package com.lumix.pro.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumix.pro.camera.CameraController
import com.lumix.pro.camera.CameraState
import com.lumix.pro.camera.LensFacing
import com.lumix.pro.modes.CameraMode
import com.lumix.pro.ui.components.*
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    controller: CameraController,
    isArabic: Boolean,
    latestThumb: Uri?,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenGoldenHour: () -> Unit,
    onMediaCaptured: (Uri) -> Unit,
    onStartTimelapse: () -> Unit,
    onStartLongExposure: () -> Unit,
    onStartPanorama: () -> Unit,
    onStartDocumentScan: () -> Unit
) {
    val state by controller.state.collectAsState()
    val view = LocalView.current

    var zebraBmp by remember { mutableStateOf<Bitmap?>(null) }
    var peakingBmp by remember { mutableStateOf<Bitmap?>(null) }
    var histogram by remember { mutableStateOf<IntArray?>(null) }
    var showMoreModesSheet by remember { mutableStateOf(false) }

    DisposableEffect(controller) {
        controller.onZebraFrame = { zebraBmp = it }
        controller.onFocusPeakingFrame = { peakingBmp = it }
        controller.onHistogramReady = { histogram = it }
        onDispose {
            controller.onZebraFrame = null
            controller.onFocusPeakingFrame = null
            controller.onHistogramReady = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1) Viewfinder + gesture layer
        CameraViewfinder(
            state = state,
            onBindPreview = { pv -> controller.attachPreview(pv.surfaceProvider) },
            onTapFocus = { nx, ny -> controller.tapToFocus(nx, ny) },
            onPinchZoom = { controller.setZoom(it) },
            onExposureDrag = { delta ->
                controller.setExposureIndex(state.exposureIndex + delta)
            }
        )

        // 2) Overlays
        CompositionGuidesView(state.gridType)
        if (state.focusPeakingEnabled) FocusPeakingOverlay(peakingBmp)
        if (state.zebraEnabled) ZebraOverlay(zebraBmp)

        // 3) Top bar
        QuickSettingsBar(
            state = state,
            onFlashClick = { controller.cycleFlash() },
            onTimerClick = { controller.cycleTimer() },
            onAspectClick = { controller.cycleAspectRatio() },
            onHdrClick = { controller.toggleHdr() },
            onGridClick = { controller.cycleGrid() },
            onSettingsClick = onOpenSettings,
            onGoldenHourClick = onOpenGoldenHour,
            onDiagnosticsClick = onOpenDiagnostics,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 4) Top HUD chips: Level + Histogram
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 58.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (state.histogramEnabled) HistogramView(histogram)
            if (state.levelEnabled) {
                LevelIndicatorView(rollDegrees = state.rollDegrees, pitchDegrees = state.pitchDegrees)
            }
        }

        // 5) Assistant chip
        AssistantChip(
            state = state,
            isArabic = isArabic,
            onApplyRecommendation = { rec ->
                com.lumix.pro.assistant.AiPhotographyAssistant().applyRecommendation(controller, rec)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 62.dp)
        )

        // 6) Bottom cluster: Pro controls + Zoom + Mode selector + Bottom bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pro HUD panel
            AnimatedVisibility(
                visible = state.activeMode == CameraMode.PRO,
                enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 2 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 2 }
            ) {
                ProControlsPanel(
                    controller = controller,
                    state = state,
                    isArabic = isArabic,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Zoom wheel
            ZoomWheel(
                currentZoom = state.zoomRatio,
                maxZoom = state.maxZoomRatio,
                onZoomChanged = { controller.setZoom(it) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(4.dp))

            // Mode selector
            ModeSelector(
                activeMode = state.activeMode,
                isArabic = isArabic,
                onModeSelected = { mode ->
                    when (mode) {
                        CameraMode.TIMELAPSE -> { controller.setMode(mode); onStartTimelapse() }
                        CameraMode.LONG_EXPOSURE -> { controller.setMode(mode); onStartLongExposure() }
                        CameraMode.PANORAMA -> { controller.setMode(mode); onStartPanorama() }
                        CameraMode.DOCUMENT -> { controller.setMode(mode); onStartDocumentScan() }
                        else -> controller.setMode(mode)
                    }
                },
                onMoreClicked = { showMoreModesSheet = true }
            )

            Spacer(Modifier.height(6.dp))

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery thumb button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { onOpenGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    if (latestThumb != null) {
                        AsyncImage(
                            model = latestThumb,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = LumixTextPrimary, modifier = Modifier.size(24.dp))
                    }
                }

                // Shutter
                ShutterButton(
                    isVideoMode = state.isVideoMode,
                    isRecording = state.isRecording,
                    isRecordingPaused = state.isRecordingPaused,
                    isCapturing = state.isCapturing,
                    captureProgress = state.captureProgress,
                    countdown = state.timerCountdown,
                    onClick = {
                        if (state.isVideoMode) {
                            if (state.isRecording) controller.stopVideoRecording()
                            else controller.startVideoRecording { uri -> onMediaCaptured(uri) }
                        } else {
                            if (state.activeMode == CameraMode.SPORTS) {
                                controller.burstCapture { uris -> uris.lastOrNull()?.let(onMediaCaptured) }
                            } else {
                                controller.capturePhoto(onSuccess = { uri, _ -> onMediaCaptured(uri) })
                            }
                        }
                    },
                    onLongPress = {
                        if (!state.isVideoMode && !state.isRecording) {
                            controller.burstCapture { uris -> uris.lastOrNull()?.let(onMediaCaptured) }
                        }
                    }
                )

                // Lens flip button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            controller.switchCamera()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cameraswitch,
                        contentDescription = null,
                        tint = LumixTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // More modes bottom sheet
        if (showMoreModesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreModesSheet = false },
                containerColor = Color(0xEE121722),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isArabic) "جميع أوضاع التصوير الاحترافية" else "ALL PROFESSIONAL MODES",
                        color = LumixCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        items(CameraMode.values()) { mode ->
                            val active = mode == state.activeMode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (active) LumixCyan else Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        showMoreModesSheet = false
                                        when (mode) {
                                            CameraMode.TIMELAPSE -> { controller.setMode(mode); onStartTimelapse() }
                                            CameraMode.LONG_EXPOSURE -> { controller.setMode(mode); onStartLongExposure() }
                                            CameraMode.PANORAMA -> { controller.setMode(mode); onStartPanorama() }
                                            CameraMode.DOCUMENT -> { controller.setMode(mode); onStartDocumentScan() }
                                            else -> controller.setMode(mode)
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.localizedTitle(isArabic),
                                    color = if (active) Color.Black else LumixTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
