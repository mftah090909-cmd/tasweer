package com.lumix.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.liquidGlass(shape: RoundedCornerShape = RoundedCornerShape(22.dp)): Modifier =
    clip(shape)
        .background(Color(0x70232D38))
        .border(1.dp, Color.White.copy(alpha = .18f), shape)

@Composable
fun GlassLabel(text: String, active: Boolean = false) {
    Text(
        text,
        color = if (active) Color(0xFF79F2D5) else Color.White.copy(alpha = .86f),
        style = MaterialTheme.typography.labelMedium
    )
}
