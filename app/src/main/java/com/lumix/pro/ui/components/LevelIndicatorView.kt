package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.ui.theme.LumixGreen
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LevelIndicatorView(
    rollDegrees: Float,
    pitchDegrees: Float,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isLevel = abs(rollDegrees) <= 1.0f
    var wasLevel by remember { mutableStateOf(false) }

    LaunchedEffect(isLevel) {
        if (isLevel && !wasLevel) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        wasLevel = isLevel
    }

    val tint by animateColorAsState(
        targetValue = if (isLevel) LumixGreen else Color.White,
        label = "levelColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(width = 110.dp, height = 30.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Fixed target reticle
            drawLine(Color.White.copy(alpha = 0.3f), Offset(cx - 36, cy), Offset(cx - 10, cy), strokeWidth = 1.5f)
            drawLine(Color.White.copy(alpha = 0.3f), Offset(cx + 10, cy), Offset(cx + 36, cy), strokeWidth = 1.5f)
            drawCircle(Color.White.copy(alpha = 0.25f), radius = 3f, center = Offset(cx, cy))

            // Horizon line rotates with roll
            rotate(-rollDegrees, pivot = Offset(cx, cy)) {
                drawLine(tint, Offset(cx - 30, cy), Offset(cx - 8, cy), strokeWidth = 2.4f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx + 8, cy), Offset(cx + 30, cy), strokeWidth = 2.4f, cap = StrokeCap.Round)
            }

            // Bubble moves with pitch (±25 px range)
            val bubbleY = (cy + (pitchDegrees.coerceIn(-30f, 30f) / 30f) * 11f)
            drawCircle(color = tint, radius = 5f, center = Offset(cx, bubbleY))
            drawCircle(color = tint, radius = 7f, center = Offset(cx, bubbleY), style = Stroke(1.2f))
        }
        Text(
            text = "%.1f°".format(rollDegrees),
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
