package com.lumix.pro.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumix.pro.editor.PhotoEditorModel
import com.lumix.pro.editor.PhotoEditorProcessor
import com.lumix.pro.enhancer.ColorBand
import com.lumix.pro.enhancer.HslBandAdjustment
import com.lumix.pro.enhancer.ImageProcessingUtils
import com.lumix.pro.enhancer.LutLook
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.components.RangeSliderPro
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TABS = listOf("LIGHT", "COLOR", "DETAIL", "EFFECTS", "LUT", "HSL", "GEOM")
private val TABS_AR = listOf("الإضاءة", "اللون", "التفاصيل", "المؤثرات", "الفلاتر", "HSL", "الأبعاد")

@Composable
fun PhotoEditorScreen(
    imageUri: Uri,
    isArabic: Boolean,
    onBack: () -> Unit,
    onExported: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBmp by remember { mutableStateOf<Bitmap?>(null) }
    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
    var editedPreview by remember { mutableStateOf<Bitmap?>(null) }
    var model by remember { mutableStateOf(PhotoEditorModel()) }
    var activeTab by remember { mutableStateOf(0) }
    var splitPos by remember { mutableStateOf(0.5f) }
    var showSplit by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        val bmp = withContext(Dispatchers.IO) {
            ImageProcessingUtils.decodeOrientedBitmap(context, imageUri, maxDimension = 1920)
        }
        originalBmp = bmp
        previewBmp = bmp
        editedPreview = bmp
    }

    LaunchedEffect(model, previewBmp) {
        val src = previewBmp ?: return@LaunchedEffect
        editedPreview = PhotoEditorProcessor.apply(src, model)
    }

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
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = LumixTextPrimary)
                }
                Text(
                    if (isArabic) "محرر الصور الاحترافي" else "PRO PHOTO EDITOR",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showSplit = !showSplit }) {
                        Icon(
                            Icons.Rounded.Compare,
                            contentDescription = null,
                            tint = if (showSplit) LumixCyan else LumixTextSecondary
                        )
                    }
                    IconButton(onClick = { model = model.reset() }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = LumixTextSecondary)
                    }
                    Button(
                        onClick = {
                            if (exporting) return@Button
                            exporting = true
                            scope.launch {
                                val full = withContext(Dispatchers.IO) {
                                    ImageProcessingUtils.decodeOrientedBitmap(context, imageUri, maxDimension = 8192)
                                } ?: return@launch
                                val out = withContext(Dispatchers.IO) {
                                    PhotoEditorProcessor.export(context, full, model, srcUriForExif = imageUri)
                                }
                                exporting = false
                                if (out != null) {
                                    Toast.makeText(context, if (isArabic) "تم حفظ الصورة المعدّلة في المعرض" else "Edited photo saved", Toast.LENGTH_SHORT).show()
                                    onExported(out)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LumixCyan),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (exporting) "…" else if (isArabic) "حفظ" else "EXPORT",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = editedPreview,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (showSplit && originalBmp != null && editedPreview != null) {
                    SplitCompareOverlay(
                        original = originalBmp!!,
                        edited = editedPreview!!,
                        splitPos = splitPos,
                        onSplitChanged = { splitPos = it }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val titles = if (isArabic) TABS_AR else TABS
                titles.forEachIndexed { i, title ->
                    val active = i == activeTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                            .clickable { activeTab = i }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (active) Color.Black else LumixTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp
                        )
                    }
                }
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeTab) {
                        0 -> LightGroup(model) { model = it }
                        1 -> ColorGroup(model) { model = it }
                        2 -> DetailGroup(model) { model = it }
                        3 -> EffectsGroup(model) { model = it }
                        4 -> LutGroup(model) { model = it }
                        5 -> HslGroup(model) { model = it }
                        6 -> GeomGroup(model) { model = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun LightGroup(m: PhotoEditorModel, onChange: (PhotoEditorModel) -> Unit) {
    RangeSliderPro("EXPOSURE", m.exposure, "%.1f EV".format(m.exposure), -3f..3f, 0, { onChange(m.copy(exposure = it)) })
    RangeSliderPro("CONTRAST", m.contrast, "${m.contrast.toInt()}", -100f..100f, 0, { onChange(m.copy(contrast = it)) })
    RangeSliderPro("HIGHLIGHTS", m.highlights, "${m.highlights.toInt()}", -100f..100f, 0, { onChange(m.copy(highlights = it)) })
    RangeSliderPro("SHADOWS", m.shadows, "${m.shadows.toInt()}", -100f..100f, 0, { onChange(m.copy(shadows = it)) })
    RangeSliderPro("WHITES", m.whites, "${m.whites.toInt()}", -100f..100f, 0, { onChange(m.copy(whites = it)) })
    RangeSliderPro("BLACKS", m.blacks, "${m.blacks.toInt()}", -100f..100f, 0, { onChange(m.copy(blacks = it)) })
}

@Composable
private fun ColorGroup(m: PhotoEditorModel, onChange: (PhotoEditorModel) -> Unit) {
    RangeSliderPro("TEMPERATURE", m.temperature, "${m.temperature.toInt()}", -100f..100f, 0, { onChange(m.copy(temperature = it)) })
    RangeSliderPro("TINT", m.tint, "${m.tint.toInt()}", -100f..100f, 0, { onChange(m.copy(tint = it)) })
    RangeSliderPro("VIBRANCE", m.vibrance, "${m.vibrance.toInt()}", -100f..100f, 0, { onChange(m.copy(vibrance = it)) })
    RangeSliderPro("SATURATION", m.saturation, "${m.saturation.toInt()}", -100f..100f, 0, { onChange(m.copy(saturation = it)) })
}

@Composable
private fun DetailGroup(m: PhotoEditorModel, onChange: (PhotoEditorModel) -> Unit) {
    RangeSliderPro("CLARITY", m.clarity, "${m.clarity.toInt()}", -100f..100f, 0, { onChange(m.copy(clarity = it)) })
    RangeSliderPro("TEXTURE", m.texture, "${m.texture.toInt()}", -100f..100f, 0, { onChange(m.copy(texture = it)) })
    RangeSliderPro("SHARPNESS", m.sharpness, "${m.sharpness.toInt()}", 0f..100f, 0, { onChange(m.copy(sharpness = it)) })
    RangeSliderPro("NOISE REDUCTION", m.noiseReduction, "${m.noiseReduction.toInt()}", 0f..100f, 0, { onChange(m.copy(noiseReduction = it)) })
}

@Composable
private fun EffectsGroup(m: PhotoEditorModel, onChange: (PhotoEditorModel) -> Unit) {
    RangeSliderPro("VIGNETTE", m.vignette, "${m.vignette.toInt()}", 0f..100f, 0, { onChange(m.copy(vignette = it)) })
    RangeSliderPro("GRAIN", m.grain, "${m.grain.toInt()}", 0f..100f, 0, { onChange(m.copy(grain = it)) })
}

@Composable
private fun LutGroup(m: PhotoEditorModel, onChange: (PhotoEditorModel) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LutLook.values().forEach { look ->
            val active = m.lutLook == look
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                    .clickable { onChange(m.copy(lutLook = look)) }
                    .padding(horizontal = 11.dp, vertical = 8.dp)
            ) {
                Text(
                    text = look.title,
                    color = if (active) Color.Black else LumixTextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    RangeSliderPro("LUT STRENGTH", m.lutStrength * 100f, "${(m.lutStrength * 100).toInt()}%", 0f..100f, 0, {
        onChange(m.copy(lutStrength = it / 100f))
    })
}

@Composable
private fun HslGroup(m: PhotoEditorModel, onChange: (PhotoEditorModel) -> Unit) {
    var selectedBand by remember { mutableStateOf(ColorBand.RED) }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorBand.values().forEach { band ->
            val active = band == selectedBand
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                    .clickable { selectedBand = band }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = band.name,
                    color = if (active) Color.Black else LumixTextPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    val cur = m.hslAdjustments[selectedBand] ?: HslBandAdjustment()
    RangeSliderPro("HUE SHIFT", cur.hueShift, "%.0f°".format(cur.hueShift), -30f..30f, 0, {
        onChange(m.copy(hslAdjustments = m.hslAdjustments + (selectedBand to cur.copy(hueShift = it))))
    })
    RangeSliderPro("SATURATION", cur.saturationDelta * 100f, "${(cur.saturationDelta * 100).toInt()}", -100f..100f, 0, {
        onChange(m.copy(hslAdjustments = m.hslAdjustments + (selectedBand to cur.copy(saturationDelta = it / 100f))))
    })
    RangeSliderPro("LUMINANCE", cur.luminanceDelta * 100f, "${(cur.luminanceDelta * 100).toInt()}", -100f..100f, 0, {
        onChange(m.copy(hslAdjustments = m.hslAdjustments + (selectedBand to cur.copy(luminanceDelta = it / 100f))))
    })
}

@Composable
private fun GeomGroup(m: PhotoEditorModel, onChange: (PhotoEditorModel) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { onChange(m.copy(rotationDegrees = (m.rotationDegrees + 90) % 360)) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.RotateRight, contentDescription = null, tint = LumixTextPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Rotate 90°", color = LumixTextPrimary, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("ASPECT RATIO", color = LumixTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    val aspects = listOf("Free" to null, "1:1" to 1f, "4:3" to 4f / 3f, "16:9" to 16f / 9f, "9:16" to 9f / 16f)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        aspects.forEach { (label, value) ->
            val active = m.cropAspect == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                    .clickable { onChange(m.copy(cropAspect = value)) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = label,
                    color = if (active) Color.Black else LumixTextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SplitCompareOverlay(
    original: Bitmap,
    edited: Bitmap,
    splitPos: Float,
    onSplitChanged: (Float) -> Unit
) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount: Float ->
                        onSplitChanged((splitPos + dragAmount / size.width.toFloat()).coerceIn(0.02f, 0.98f))
                    }
                )
            }
    ) {
        val splitX = size.width * splitPos
        val sz = IntSize(size.width.toInt(), size.height.toInt())
        drawImage(image = edited.asImageBitmap(), dstSize = sz)
        clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
            drawImage(image = original.asImageBitmap(), dstSize = sz)
        }
        drawLine(
            Color.White,
            androidx.compose.ui.geometry.Offset(splitX, 0f),
            androidx.compose.ui.geometry.Offset(splitX, size.height),
            strokeWidth = 2.dp.toPx()
        )
        drawCircle(Color.White, radius = 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(splitX, size.height / 2f))
        drawCircle(Color.Black, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(splitX, size.height / 2f))
    }
}
