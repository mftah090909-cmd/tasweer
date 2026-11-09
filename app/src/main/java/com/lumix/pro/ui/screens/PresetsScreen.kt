package com.lumix.pro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
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
import com.lumix.pro.camera.CameraController
import com.lumix.pro.presets.CameraPreset
import com.lumix.pro.presets.PresetDatabase
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PresetsScreen(
    controller: CameraController,
    isArabic: Boolean,
    onBack: () -> Unit,
    onPresetApplied: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { PresetDatabase.getInstance(context).presetDao() }
    val presets by dao.getAll().collectAsState(initial = emptyList())
    val state by controller.state.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFF0A0C10),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = LumixTextPrimary)
                }
                Text(
                    if (isArabic) "الإعدادات المحفوظة" else "CAMERA PRESETS",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = LumixCyan)
                }
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
                Text(
                    text = if (isArabic) "احفظ إعداداتك اليدوية المفضلة (ISO، الغالق، التعريض) واسترجعها بنقرة واحدة."
                    else "Save your favorite manual settings (ISO, Shutter, EV) and recall them with one tap.",
                    color = LumixTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            items(presets, key = { it.id }) { p ->
                PresetCard(
                    preset = p,
                    isArabic = isArabic,
                    onApply = {
                        p.iso?.let { controller.setManualIso(it) }
                        p.shutterNs?.let { controller.setManualShutterNs(it) }
                        controller.setExposureIndex(p.exposureIndex)
                        controller.setManualFocusDistance(p.focusDistance)
                        controller.setWhiteBalanceKelvin(p.wbKelvin)
                        Toast.makeText(context, if (isArabic) "تم تطبيق «${p.name}»" else "Applied ${p.name}", Toast.LENGTH_SHORT).show()
                        onPresetApplied()
                    },
                    onDelete = {
                        scope.launch(Dispatchers.IO) { dao.delete(p) }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isArabic) "حفظ الإعدادات الحالية" else "Save current settings", color = LumixTextPrimary) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(if (isArabic) "اسم الإعداد المسبق" else "Preset name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val nm = presetName.trim().ifBlank { "Preset" }
                    showAddDialog = false
                    presetName = ""
                    scope.launch(Dispatchers.IO) {
                        dao.insert(
                            CameraPreset(
                                name = nm,
                                iso = state.manualIso,
                                shutterNs = state.manualShutterNs,
                                exposureIndex = state.exposureIndex,
                                wbKelvin = state.whiteBalanceKelvin,
                                focusDistance = state.manualFocusDistance,
                                modeName = state.activeMode.name
                            )
                        )
                    }
                }) {
                    Text(if (isArabic) "حفظ" else "Save", color = LumixCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            },
            containerColor = Color(0xEE121722)
        )
    }
}

@Composable
private fun PresetCard(
    preset: CameraPreset,
    isArabic: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, color = LumixTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                val chips = listOf(
                    preset.iso?.let { "ISO $it" } ?: "ISO AUTO",
                    preset.shutterNs?.let { "1/${1_000_000_000L / it}s" } ?: "S AUTO",
                    "EV %.1f".format(preset.exposureIndex / 3f),
                    preset.wbKelvin?.let { "${it}K" } ?: "AWB"
                )
                Text(chips.joinToString("   ·   "), color = LumixCyan, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = LumixCyan),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text(if (isArabic) "تطبيق" else "APPLY", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = LumixRed)
                }
            }
        }
    }
}
