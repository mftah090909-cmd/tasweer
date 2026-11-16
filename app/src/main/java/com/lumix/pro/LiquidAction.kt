package com.lumix.pro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LiquidAction(text: String, modifier: Modifier = Modifier, active: Boolean = false, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) .94f else 1f, label = "liquidPress")
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.scale(scale).background(Color.White.copy(alpha = if (active) .20f else .08f), shape).border(1.dp, Color.White.copy(alpha = .28f), shape).clickable {
            pressed = true
            onClick()
            pressed = false
        }.then(modifier).padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (active) Color(0xFF7CF4D6) else Color.White) }
}
