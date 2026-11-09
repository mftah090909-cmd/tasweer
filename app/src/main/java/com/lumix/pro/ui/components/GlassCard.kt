package com.lumix.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumix.pro.ui.theme.LumixBorder
import com.lumix.pro.ui.theme.LumixGlass

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    bgColor: Color = LumixGlass,
    borderColor: Color = LumixBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(bgColor.copy(alpha = (bgColor.alpha * 1.15f).coerceAtMost(1f)), bgColor)
                )
            )
            .border(borderWidth, borderColor, shape)
            .padding(1.dp)
    ) {
        content()
    }
}
