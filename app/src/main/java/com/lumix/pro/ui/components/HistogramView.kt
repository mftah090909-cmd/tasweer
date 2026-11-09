package com.lumix.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lumix.pro.ui.theme.LumixCyan
import kotlin.math.max

@Composable
fun HistogramView(
    histogram: IntArray?,
    modifier: Modifier = Modifier
) {
    val hist = histogram ?: remember { IntArray(256) }
    val maxVal = remember(hist) { max(1, hist.maxOrNull() ?: 1) }

    Box(
        modifier = modifier
            .size(width = 112.dp, height = 54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bins = hist.size
            val barW = size.width / bins
            for (i in 0 until bins) {
                val norm = (hist[i].toFloat() / maxVal).coerceIn(0f, 1f)
                val barH = norm * (size.height - 2)
                val tint = when {
                    i < 85 -> Color(0xFF5D7AFF)
                    i < 170 -> Color(0xFF6DE89D)
                    else -> LumixCyan
                }
                drawRect(
                    color = tint.copy(alpha = 0.85f),
                    topLeft = Offset(i * barW, size.height - barH - 1),
                    size = Size(barW, barH)
                )
            }
        }
    }
}
