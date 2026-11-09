package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.modes.CameraMode
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextTertiary

@Composable
fun ModeSelector(
    activeMode: CameraMode,
    isArabic: Boolean,
    onModeSelected: (CameraMode) -> Unit,
    onMoreClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraMode.PRIMARY_MODES.forEach { mode ->
            val selected = mode == activeMode
            val color by animateColorAsState(
                targetValue = if (selected) LumixCyan else LumixTextPrimary.copy(alpha = 0.55f),
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "modeColor"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onModeSelected(mode)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = mode.localizedTitle(isArabic),
                    color = color,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.9.sp
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(if (selected) 16.dp else 0.dp)
                        .height(2.dp)
                        .background(if (selected) LumixCyan else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(1.dp))
                )
            }
        }
        Text(
            text = if (isArabic) "المزيد ▸" else "MORE ▸",
            color = LumixTextTertiary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onMoreClicked()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
