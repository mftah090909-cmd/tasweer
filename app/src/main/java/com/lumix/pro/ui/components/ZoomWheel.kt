package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import kotlin.math.roundToInt

@Composable
fun ZoomWheel(
    currentZoom: Float,
    maxZoom: Float,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showWheel by remember { mutableStateOf(false) }

    val lensPresets = remember(maxZoom) {
        listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f).filter { it <= maxZoom }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick lens chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            lensPresets.forEach { p ->
                val active = (currentZoom * 10).roundToInt() == (p * 10).roundToInt()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (active) LumixCyan else Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onZoomChanged(p)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (p == 0.5f) ".5" else "${p.toInt()}×",
                        color = if (active) Color.Black else LumixTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            // Expand wheel toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showWheel = !showWheel }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${"%.1f".format(currentZoom)}×",
                    color = LumixCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Draggable smooth wheel
        AnimatedVisibility(visible = showWheel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(44.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            val sensitivity = (maxZoom - 1f) / 1400f
                            onZoomChanged((currentZoom - delta * sensitivity).coerceIn(1f, maxZoom))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val ticks = 41
                    val spacing = size.width / (ticks - 1)
                    val centerIdx = ticks / 2
                    val cy = size.height / 2f
                    for (i in 0 until ticks) {
                        val x = i * spacing
                        val isCenter = i == centerIdx
                        val isMajor = (i - centerIdx) % 5 == 0
                        val tickH = if (isCenter) 22.dp.toPx() else if (isMajor) 14.dp.toPx() else 8.dp.toPx()
                        val color = if (isCenter) LumixCyan else Color.White.copy(alpha = if (isMajor) 0.55f else 0.25f)
                        drawLine(
                            color = color,
                            start = Offset(x, cy - tickH / 2),
                            end = Offset(x, cy + tickH / 2),
                            strokeWidth = if (isCenter) 2.5f else 1.2f
                        )
                    }
                }
            }
        }
    }
}
