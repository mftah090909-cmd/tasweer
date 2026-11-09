package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextSecondary

@Composable
fun RangeSliderPro(
    label: String,
    value: Float,
    displayValue: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticThreshold: Float = 0f
) {
    val view = LocalView.current
    var lastHapticValue by remember { mutableStateOf(value) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = LumixTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = displayValue,
                color = if (enabled) LumixCyan else LumixTextSecondary.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = {
                onValueChange(it)
                if (hapticThreshold > 0f && kotlin.math.abs(it - lastHapticValue) >= hapticThreshold) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    lastHapticValue = it
                }
            },
            valueRange = range,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = LumixCyan,
                activeTrackColor = LumixCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                disabledThumbColor = Color.Gray,
                disabledActiveTrackColor = Color.DarkGray
            )
        )
    }
}
