package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.camera.CameraState
import com.lumix.pro.camera.FlashOption
import com.lumix.pro.camera.GridType
import com.lumix.pro.ui.theme.LumixAmber
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary

@Composable
fun QuickSettingsBar(
    state: CameraState,
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onAspectClick: () -> Unit,
    onHdrClick: () -> Unit,
    onGridClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onGoldenHourClick: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scroll = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash
        QuickIconChip(
            icon = when (state.flashOption) {
                FlashOption.AUTO -> Icons.Rounded.FlashAuto
                FlashOption.ON -> Icons.Rounded.FlashOn
                FlashOption.OFF -> Icons.Rounded.FlashOff
                FlashOption.TORCH -> Icons.Rounded.Highlight
            },
            label = when (state.flashOption) {
                FlashOption.AUTO -> "A"
                FlashOption.TORCH -> "TORCH"
                else -> null
            },
            active = state.flashOption != FlashOption.OFF,
            onClick = onFlashClick
        )

        // Timer
        QuickChip(
            label = if (state.timerSeconds == 0) "⏱" else "⏱ ${state.timerSeconds}s",
            active = state.timerSeconds > 0,
            onClick = onTimerClick
        )

        // Aspect
        QuickChip(label = state.aspectRatio.label, active = false, onClick = onAspectClick)

        // HDR
        QuickChip(label = "HDR", active = state.hdrEnabled, onClick = onHdrClick)

        // Grid
        val gridLabel = when (state.gridType) {
            GridType.NONE -> "GRID"
            GridType.RULE_OF_THIRDS -> "3×3"
            GridType.GOLDEN_RATIO -> "φ"
            GridType.DIAGONALS -> "✕"
            GridType.KEYSTONE -> "DOC"
        }
        QuickChip(label = gridLabel, active = state.gridType != GridType.NONE, onClick = onGridClick)

        // Golden hour shortcut
        QuickChip(
            label = if (state.goldenHourActive) "✦ GOLDEN" else "✦ SUN",
            active = state.goldenHourActive,
            onClick = onGoldenHourClick,
            activeColor = LumixAmber
        )

        // Diagnostics
        QuickIconChip(icon = Icons.Rounded.Info, active = false, onClick = onDiagnosticsClick)

        // Settings
        QuickIconChip(icon = Icons.Rounded.Settings, active = false, onClick = onSettingsClick)

        // Status pill
        StatusPill(state)
    }
}

@Composable
private fun QuickChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    activeColor: Color = LumixCyan
) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) activeColor else Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) Color.Black else LumixTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun QuickIconChip(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    label: String? = null
) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (active) LumixCyan else Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                color = if (active) Color.Black else LumixTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Color.Black else LumixTextPrimary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun StatusPill(state: CameraState) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${state.storageFreeMb / 1000}G",
            color = LumixTextPrimary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "${state.batteryPercent}%",
            color = if (state.batteryPercent <= 15) LumixRed else LumixTextPrimary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
