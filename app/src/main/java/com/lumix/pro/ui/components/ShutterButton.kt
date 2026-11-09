package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary

@Composable
fun ShutterButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    isRecordingPaused: Boolean,
    isCapturing: Boolean,
    captureProgress: Float?,
    countdown: Int?,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "shutterScale"
    )

    // Pulse animation while recording
    val infinite = rememberInfiniteTransition(label = "recPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val innerColor by animateColorAsState(
        targetValue = when {
            isRecording -> LumixRed
            isVideoMode -> LumixRed.copy(alpha = 0.95f)
            else -> LumixTextPrimary
        },
        label = "shutterColor"
    )

    val outerRingColor by animateColorAsState(
        targetValue = when {
            isRecording -> LumixRed.copy(alpha = 0.7f)
            isVideoMode -> LumixRed.copy(alpha = 0.45f)
            else -> Color.White.copy(alpha = 0.55f)
        },
        label = "ringColor"
    )

    Box(
        modifier = modifier
            .size(94.dp)
            .graphicsLayer {
                val s = if (isRecording && !isRecordingPaused) scale * pulse else scale
                scaleX = s
                scaleY = s
            }
            .pointerInput(isRecording, isVideoMode) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongPress?.invoke()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerRadius = cx - 3.dp.toPx()
            val innerRadius = cx - 14.dp.toPx()

            // Outer metallic bezel
            drawCircle(color = outerRingColor, radius = outerRadius, style = Stroke(width = 3.5.dp.toPx()))

            // Progress arc for burst/timer/long-exposure
            if (captureProgress != null && captureProgress in 0f..1f) {
                drawArc(
                    color = LumixCyan,
                    startAngle = -90f,
                    sweepAngle = captureProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(cx - outerRadius, cy - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner button: circle for photo, morphing rounded rect for video recording
            if (isRecording) {
                val rectSide = innerRadius * 0.9f
                drawRoundRect(
                    color = innerColor,
                    topLeft = Offset(cx - rectSide / 2, cy - rectSide / 2),
                    size = Size(rectSide, rectSide),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            } else {
                drawCircle(color = innerColor, radius = innerRadius)
            }
        }
    }
}
