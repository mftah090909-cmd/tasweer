package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.camera.CameraCapabilities
import com.lumix.pro.camera.CameraController
import com.lumix.pro.camera.CameraState
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import kotlin.math.*

private val STANDARD_ISO = listOf(50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400, 12800)
private val STANDARD_SHUTTER = listOf(
    1L / 8000 to "1/8000", 1L / 4000 to "1/4000", 1L / 2000 to "1/2000", 1L / 1000 to "1/1000",
    1L / 500 to "1/500", 1L / 250 to "1/250", 1L / 125 to "1/125", 1L / 60 to "1/60",
    1L / 30 to "1/30", 1L / 15 to "1/15", 1L / 8 to "1/8", 1L / 4 to "1/4", 1L / 2 to "1/2",
    1L to "1s", 2L to "2s", 4L to "4s", 8L to "8s", 15L to "15s", 30L to "30s"
)

@Composable
fun ProControlsPanel(
    controller: CameraController,
    state: CameraState,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    val caps: CameraCapabilities? = state.activeCapabilities
    val isoSupported = caps?.isoRange != null
    val expTimeNs = caps?.exposureTimeRangeNs
    val shutterSupported = expTimeNs != null
    val focusSupported = (caps?.minimumFocusDistanceDiopters ?: 0f) > 0f

    val evSteps = remember(state.exposureRange) {
        state.exposureRange.upper - state.exposureRange.lower
    }
    val evDisplay = "%.1f EV".format(state.exposureIndex * state.exposureStep)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: Pro HUD badges + Locks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "لوحة التحكم اليدوي PRO HUD" else "PRO HUD CONTROLS",
                    color = LumixTextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LockChip(label = "AE-L", active = state.aeLocked) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        controller.setAeLocked(!state.aeLocked)
                    }
                    LockChip(label = "AWB-L", active = state.awbLocked) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        controller.setAwbLocked(!state.awbLocked)
                    }
                    LockChip(label = "RESET", active = false) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        controller.setManualIso(null)
                        controller.setManualShutterNs(null)
                        controller.setExposureIndex(0)
                        controller.setManualFocusDistance(null)
                        controller.setWhiteBalanceKelvin(null)
                        controller.setAeLocked(false)
                        controller.setAwbLocked(false)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 1) ISO
            if (isoSupported && caps != null && caps.isoRange != null) {
                val isoList = remember(caps.isoRange) {
                    STANDARD_ISO.filter { it >= caps.isoRange.lower && it <= caps.isoRange.upper }
                }
                val curIdx = state.manualIso?.let { iso -> isoList.indexOfFirst { v -> v >= iso }.coerceAtLeast(0) }
                RangeSliderPro(
                    label = "ISO",
                    value = curIdx?.toFloat() ?: -1f,
                    displayValue = if (state.manualIso == null) "AUTO" else "${state.manualIso}",
                    range = -1f..(isoList.size - 1).toFloat(),
                    steps = (isoList.size - 1).coerceAtLeast(0),
                    onValueChange = {
                        val i = it.roundToInt()
                        if (i < 0) controller.setManualIso(null) else controller.setManualIso(isoList[i])
                    },
                    hapticThreshold = 1f
                )
            }

            // 2) Shutter
            if (shutterSupported && expTimeNs != null) {
                val shutterList = remember(expTimeNs) {
                    STANDARD_SHUTTER.filter { (sec, _) ->
                        val ns = sec * 1_000_000_000L
                        ns >= expTimeNs.lower && ns <= expTimeNs.upper
                    }
                }
                val curIdx = state.manualShutterNs?.let { ns ->
                    shutterList.indexOfFirst { it.first * 1_000_000_000L >= ns }.coerceAtLeast(0)
                }
                RangeSliderPro(
                    label = if (isArabic) "سرعة الغالق" else "SHUTTER",
                    value = curIdx?.toFloat() ?: -1f,
                    displayValue = if (state.manualShutterNs == null) "AUTO" else shutterList.getOrNull(curIdx ?: 0)?.second ?: "—",
                    range = -1f..(shutterList.size - 1).toFloat(),
                    steps = (shutterList.size - 1).coerceAtLeast(0),
                    onValueChange = {
                        val i = it.roundToInt()
                        if (i < 0) controller.setManualShutterNs(null) else controller.setManualShutterNs(shutterList[i].first * 1_000_000_000L)
                    },
                    hapticThreshold = 1f
                )
            }

            // 3) Exposure compensation
            RangeSliderPro(
                label = if (isArabic) "التعريض" else "EXPOSURE",
                value = state.exposureIndex.toFloat(),
                displayValue = evDisplay,
                range = state.exposureRange.lower.toFloat()..state.exposureRange.upper.toFloat(),
                steps = max(0, evSteps - 1),
                onValueChange = { controller.setExposureIndex(it.roundToInt()) },
                hapticThreshold = 1f
            )

            // 4) White Balance Kelvin
            RangeSliderPro(
                label = if (isArabic) "توازن البياض" else "WHITE BALANCE",
                value = (state.whiteBalanceKelvin ?: 1999).toFloat(),
                displayValue = if (state.whiteBalanceKelvin == null) "AUTO" else "${state.whiteBalanceKelvin}K",
                range = 1999f..9000f,
                steps = 70,
                onValueChange = {
                    val k = it.roundToInt()
                    if (k < 2100) controller.setWhiteBalanceKelvin(null) else controller.setWhiteBalanceKelvin((k / 100) * 100)
                },
                hapticThreshold = 300f
            )

            // 5) Manual focus
            if (focusSupported) {
                val maxFd = caps?.minimumFocusDistanceDiopters ?: 10f
                val fdVal = state.manualFocusDistance ?: -1f
                RangeSliderPro(
                    label = if (isArabic) "التركيز اليدوي" else "MANUAL FOCUS",
                    value = fdVal,
                    displayValue = if (fdVal < 0) "AF" else if (fdVal == 0f) "∞" else "%.1f D".format(fdVal),
                    range = -1f..maxFd,
                    steps = 40,
                    onValueChange = {
                        if (it < 0f) controller.setManualFocusDistance(null) else controller.setManualFocusDistance(it)
                    },
                    hapticThreshold = 0.5f
                )
            }

            // Extra toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LockChip(label = "PEAK", active = state.focusPeakingEnabled) { controller.toggleFocusPeaking() }
                LockChip(label = "ZEBRA", active = state.zebraEnabled) { controller.toggleZebra() }
                LockChip(label = "RAW", active = state.rawEnabled) { controller.toggleRaw() }
            }
        }
    }
}

@Composable
private fun LockChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) LumixCyan else Color.White.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) Color.Black else LumixTextPrimary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
