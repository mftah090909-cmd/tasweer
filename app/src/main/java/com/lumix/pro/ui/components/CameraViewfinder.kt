package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lumix.pro.camera.CameraState
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CameraViewfinder(
    state: CameraState,
    onBindPreview: (PreviewView) -> Unit,
    onTapFocus: (normX: Float, normY: Float) -> Unit,
    onPinchZoom: (Float) -> Unit,
    onExposureDrag: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusEpoch by remember { mutableStateOf(0L) }
    var zoomAccum by remember { mutableStateOf(state.zoomRatio) }
    LaunchedEffect(state.zoomRatio) { zoomAccum = state.zoomRatio }

    // Focus ring fade out
    LaunchedEffect(focusEpoch) {
        if (focusEpoch == 0L) return@LaunchedEffect
        delay(2600)
        focusPoint = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    zoomAccum = (zoomAccum * zoom).coerceIn(1f, 100f)
                    onPinchZoom(zoomAccum)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { pos ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        focusPoint = pos
                        focusEpoch = System.currentTimeMillis()
                        val nx = (pos.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val ny = (pos.y / size.height.toFloat()).coerceIn(0f, 1f)
                        onTapFocus(nx, ny)
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    onBindPreview(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Focus ring with sun-exposure slider beside it
        focusPoint?.let { pt ->
            FocusRing(
                x = pt.x,
                y = pt.y,
                onExposureDrag = onExposureDrag
            )
        }

        // Timer countdown big number overlay
        state.timerCountdown?.let { c ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$c",
                    color = LumixTextPrimary,
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Recording HUD
        if (state.isRecording) {
            RecordingHud(
                durationMs = state.recordingDurationMs,
                isPaused = state.isRecordingPaused,
                audioRms = state.audioLevelRms,
                quality = state.videoQuality,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun FocusRing(
    x: Float,
    y: Float,
    onExposureDrag: (Int) -> Unit
) {
    val inf = rememberInfiniteTransition(label = "focusPulse")
    val pulse by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "fp"
    )

    // Vertical drag next to focus ring adjusts exposure
    Box(
        modifier = Modifier
            .offset { IntOffset((x - 42.dp.roundToPx()).roundToInt(), (y - 42.dp.roundToPx()).roundToInt()) }
            .size(84.dp)
            .pointerInput(Unit) {
                var accum = 0f
                detectVerticalDragGestures { _, dy ->
                    accum += dy
                    if (kotlin.math.abs(accum) > 18f) {
                        onExposureDrag(if (accum < 0) 1 else -1)
                        accum = 0f
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = (size.width / 2f) * pulse
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawCircle(LumixCyan, radius = r, center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
            // crosshairs
            drawLine(LumixCyan, Offset(cx - r - 6, cy), Offset(cx - r + 4, cy), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(LumixCyan, Offset(cx + r - 4, cy), Offset(cx + r + 6, cy), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(LumixCyan, Offset(cx, cy - r - 6), Offset(cx, cy - r + 4), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(LumixCyan, Offset(cx, cy + r - 4), Offset(cx, cy + r + 6), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun RecordingHud(
    durationMs: Long,
    isPaused: Boolean,
    audioRms: Float,
    quality: String,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "recDot")
    val dotAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "dot"
    )
    val s = (durationMs / 1000).toInt()
    val m = s / 60
    val h = m / 60
    val time = if (h > 0) "%d:%02d:%02d".format(h, m % 60, s % 60) else "%02d:%02d".format(m, s % 60)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .border(1.dp, LumixRed.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulsing red recording dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(LumixRed.copy(alpha = if (isPaused) 0.35f else dotAlpha))
        )
        Text(
            text = if (isPaused) "PAUSED $time" else "REC $time",
            color = LumixTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = quality,
            color = LumixCyan,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        // VU level bar
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(audioRms.coerceIn(0f, 1f))
                    .background(if (audioRms > 0.85f) LumixRed else LumixCyan)
            )
        }
    }
}
