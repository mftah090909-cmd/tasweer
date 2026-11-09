package com.lumix.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.camera.CameraCapabilities
import com.lumix.pro.camera.CameraHardwareManager
import com.lumix.pro.camera.CameraState
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.theme.*

@Composable
fun DiagnosticsScreen(
    state: CameraState,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val inventory = remember { CameraHardwareManager(context).discoverInventory() }

    Scaffold(
        containerColor = Color(0xFF0A0C10),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = LumixTextPrimary)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isArabic) "تشخيص عتاد الكاميرا" else "CAMERA HARDWARE DIAGNOSTICS",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(if (isArabic) "حالة النظام والحرارة" else "SYSTEM & THERMAL", color = LumixCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        DiagRow("Battery", "${state.batteryPercent}%  ·  ${if (state.batteryPercent > 20) "Nominal" else "Low"}")
                        DiagRow("Thermal status", if (state.thermalThrottled) "THROTTLED" else "Nominal", valueColor = if (state.thermalThrottled) LumixRed else LumixGreen)
                        DiagRow("Storage", "${"%.1f".format(state.storageFreeMb / 1024f)} GB free")
                        DiagRow("Active lens", "${state.activeFacing}  ·  hardware level ${state.activeCapabilities?.hardwareLevelName ?: "—"}")
                    }
                }
            }
            items(inventory.allCameras) { c ->
                CameraSensorCard(c, isArabic)
            }
        }
    }
}

@Composable
private fun CameraSensorCard(c: CameraCapabilities, isArabic: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Camera #${c.cameraId}  ·  ${c.facing}", color = LumixTextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                Badge(label = c.hardwareLevelName, color = if (c.isFullOrBetter) LumixGreen else LumixAmber)
            }
            Spacer(Modifier.height(10.dp))
            val mp = (c.maxPhotoResolution.width.toLong() * c.maxPhotoResolution.height) / 1_000_000.0
            DiagRow("Max photo", "${c.maxPhotoResolution.width}×${c.maxPhotoResolution.height}  (%.1f MP)".format(mp))
            DiagRow("Max video", c.maxVideoResolutionName)
            if (c.isoRange != null) DiagRow("ISO range", "${c.isoRange.lower} – ${c.isoRange.upper}")
            if (c.exposureTimeRangeNs != null) {
                val hi = 1_000_000_000.0 / c.exposureTimeRangeNs.lower
                val lo = c.exposureTimeRangeNs.upper / 1_000_000_000.0
                DiagRow("Shutter range", "1/%.0f s – %.0f s".format(hi, lo))
            }
            if (c.focalLengthsMm.isNotEmpty()) DiagRow("Focal lengths", c.focalLengthsMm.joinToString(", ") { "%.1f mm".format(it) })
            if (c.apertures.isNotEmpty()) DiagRow("Apertures", c.apertures.joinToString(", ") { "f/%.1f".format(it) })
            if (c.minimumFocusDistanceDiopters != null) DiagRow("Min focus", "%.1f diopters".format(c.minimumFocusDistanceDiopters))
            DiagRow("Optical stabilization", if (c.opticalStabilizationSupported) "YES" else "No", valueColor = if (c.opticalStabilizationSupported) LumixGreen else LumixTextSecondary)
            DiagRow("RAW capture", if (c.rawSensorSupported) "YES" else "No", valueColor = if (c.rawSensorSupported) LumixGreen else LumixTextSecondary)
            DiagRow("Max zoom", "%.1f×".format(c.maxZoomRatio))
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String, valueColor: Color = LumixTextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LumixTextSecondary, fontSize = 11.5.sp)
        Text(value, color = valueColor, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Badge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
