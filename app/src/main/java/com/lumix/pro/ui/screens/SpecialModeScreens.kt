package com.lumix.pro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumix.pro.camera.CameraController
import com.lumix.pro.camera.CameraState
import com.lumix.pro.camera.PreviewAnalyzer
import com.lumix.pro.enhancer.ImageProcessingUtils
import com.lumix.pro.modes.*
import com.lumix.pro.ui.components.CameraViewfinder
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------- TIMELAPSE ----------------------------------------------------
@Composable
fun TimelapseScreen(
    controller: CameraController,
    isArabic: Boolean,
    onClose: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var intervalSec by remember { mutableStateOf(2f) }
    var running by remember { mutableStateOf(false) }
    var shots by remember { mutableStateOf(0) }
    var loopJob by remember { mutableStateOf<Job?>(null) }
    var lastSavedUri by remember { mutableStateOf<Uri?>(null) }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        running = false
    }

    DisposableEffect(Unit) { onDispose { stop() } }

    Scaffold(
        containerColor = Color(0xFF0A0C10),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { stop(); onClose() }) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = LumixTextPrimary)
                }
                Text(
                    if (isArabic) "تصوير الفاصل الزمني TIME-LAPSE" else "TIME-LAPSE",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = { lastSavedUri?.let(onSaved) ?: onClose() }) {
                    Text(if (isArabic) "تم" else "DONE", color = LumixCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                CameraViewfinder(
                    state = controller.state.collectAsState().value,
                    onBindPreview = { pv -> controller.attachPreview(pv.surfaceProvider) },
                    onTapFocus = { nx, ny -> controller.tapToFocus(nx, ny) },
                    onPinchZoom = { controller.setZoom(it) },
                    onExposureDrag = { }
                )
                if (running) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("⏱ SHOTS: $shots", color = LumixCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("INTERVAL: ${intervalSec.toInt()}s", color = LumixTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Slider(
                        value = intervalSec,
                        onValueChange = { intervalSec = it },
                        valueRange = 1f..30f,
                        steps = 28,
                        enabled = !running
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (running) {
                                stop()
                            } else {
                                running = true
                                shots = 0
                                loopJob = scope.launch(Dispatchers.Default) {
                                    while (running) {
                                        withContext(Dispatchers.Main) {
                                            controller.capturePhoto(onSuccess = { uri, _ ->
                                                lastSavedUri = uri
                                                shots++
                                            })
                                        }
                                        delay((intervalSec * 1000).toLong())
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (running) LumixRed else LumixCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (running) (if (isArabic) "إيقاف" else "STOP") else (if (isArabic) "بدء الفاصل الزمني" else "START TIME-LAPSE"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------- LONG EXPOSURE ----------------------------------------------------
@Composable
fun LongExposureScreen(
    controller: CameraController,
    isArabic: Boolean,
    onClose: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var durationSec by remember { mutableStateOf(3) }
    var style by remember { mutableStateOf(LongExposureStyle.SILKY_WATER) }
    var inProgress by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var resultBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var simulator = remember(style, durationSec) { LongExposureSimulator(style, durationSec) }

    Scaffold(
        containerColor = Color(0xFF0A0C10),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = LumixTextPrimary)
                }
                Text(
                    if (isArabic) "التعريض الطويل LONG EXPOSURE" else "LONG EXPOSURE",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                TextButton(
                    onClick = {
                        val r = resultBmp ?: return@TextButton
                        scope.launch(Dispatchers.IO) {
                            val uri = ImageProcessingUtils.saveToMediaStore(context, r, prefix = "LUMIX_LONGEXP")
                            withContext(Dispatchers.Main) {
                                if (uri != null) {
                                    Toast.makeText(context, if (isArabic) "تم الحفظ في المعرض" else "Saved to gallery", Toast.LENGTH_SHORT).show()
                                    onSaved(uri)
                                }
                            }
                        }
                    },
                    enabled = resultBmp != null
                ) {
                    Text(if (isArabic) "حفظ" else "SAVE", color = if (resultBmp != null) LumixCyan else Color.Gray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
                if (resultBmp != null) {
                    AsyncImage(model = resultBmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } else {
                    CameraViewfinder(
                        state = controller.state.collectAsState().value,
                        onBindPreview = { pv -> controller.attachPreview(pv.surfaceProvider) },
                        onTapFocus = { nx, ny -> controller.tapToFocus(nx, ny) },
                        onPinchZoom = { controller.setZoom(it) },
                        onExposureDrag = { }
                    )
                }
                if (inProgress) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = LumixCyan)
                        Spacer(Modifier.height(10.dp))
                        Text(if (isArabic) "جارٍ محاكاة التعريض الطويل…" else "Simulating long exposure…", color = LumixTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(4.dp))
                        Text("${(progress * 100).toInt()}%", color = LumixCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(if (isArabic) "المدة: ${durationSec} ثوانٍ" else "DURATION: ${durationSec}s", color = LumixTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Slider(value = durationSec.toFloat(), onValueChange = { durationSec = it.toInt() }, valueRange = 1f..10f, steps = 8, enabled = !inProgress)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { style = LongExposureStyle.SILKY_WATER },
                            colors = ButtonDefaults.buttonColors(containerColor = if (style == LongExposureStyle.SILKY_WATER) LumixCyan else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Silky Water", color = if (style == LongExposureStyle.SILKY_WATER) Color.Black else LumixTextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { style = LongExposureStyle.LIGHT_TRAILS },
                            colors = ButtonDefaults.buttonColors(containerColor = if (style == LongExposureStyle.LIGHT_TRAILS) LumixCyan else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Light Trails", color = if (style == LongExposureStyle.LIGHT_TRAILS) Color.Black else LumixTextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (inProgress) return@Button
                            inProgress = true
                            progress = 0f
                            simulator = LongExposureSimulator(style, durationSec)
                            scope.launch(Dispatchers.IO) {
                                val totalMs = durationSec * 1000L
                                val t0 = System.currentTimeMillis()
                                val captured = mutableListOf<android.graphics.Bitmap>()
                                while (System.currentTimeMillis() - t0 < totalMs) {
                                    val uri = withContext(Dispatchers.Main) {
                                        var r: Uri? = null
                                        controller.capturePhoto(onSuccess = { u, _ -> r = u })
                                        delay(350)
                                        r
                                    }
                                    if (uri != null) {
                                        val b = ImageProcessingUtils.decodeOrientedBitmap(context, uri, maxDimension = 1920)
                                        if (b != null) captured += b
                                    }
                                    progress = ((System.currentTimeMillis() - t0).toFloat() / totalMs).coerceIn(0f, 1f)
                                }
                                val out = MultiShotCapture.stackNight(captured.ifEmpty { listOf(ImageProcessingUtils.createBitmap(1920, 1080)) })
                                withContext(Dispatchers.Main) {
                                    resultBmp = out
                                    inProgress = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LumixCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !inProgress
                    ) {
                        Text(if (isArabic) "بدء التعريض" else "CAPTURE LONG EXPOSURE", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------- PANORAMA ----------------------------------------------------
@Composable
fun PanoramaScreen(
    controller: CameraController,
    isArabic: Boolean,
    onClose: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shots by remember { mutableStateOf(0) }
    var inProgress by remember { mutableStateOf(false) }
    var resultBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val captured = remember { mutableListOf<android.graphics.Bitmap>() }

    Scaffold(
        containerColor = Color(0xFF0A0C10),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = LumixTextPrimary)
                }
                Text(
                    if (isArabic) "بانوراما PANORAMA" else "PANORAMA",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                TextButton(
                    onClick = {
                        val r = resultBmp ?: return@TextButton
                        scope.launch(Dispatchers.IO) {
                            val uri = ImageProcessingUtils.saveToMediaStore(context, r, prefix = "LUMIX_PANO")
                            withContext(Dispatchers.Main) {
                                if (uri != null) {
                                    Toast.makeText(context, if (isArabic) "تم الحفظ" else "Saved", Toast.LENGTH_SHORT).show()
                                    onSaved(uri)
                                }
                            }
                        }
                    },
                    enabled = resultBmp != null
                ) {
                    Text(if (isArabic) "حفظ" else "SAVE", color = if (resultBmp != null) LumixCyan else Color.Gray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
                if (resultBmp != null) {
                    AsyncImage(model = resultBmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } else {
                    CameraViewfinder(
                        state = controller.state.collectAsState().value,
                        onBindPreview = { pv -> controller.attachPreview(pv.surfaceProvider) },
                        onTapFocus = { nx, ny -> controller.tapToFocus(nx, ny) },
                        onPinchZoom = { controller.setZoom(it) },
                        onExposureDrag = { }
                    )
                }
                if (inProgress) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = LumixCyan)
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(if (isArabic) "التقط 3-8 لقطات متتالية مع تحريك الهاتف أفقيًا" else "Capture 3–8 consecutive shots sweeping horizontally", color = LumixTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (inProgress) return@Button
                                controller.capturePhoto(onSuccess = { uri, _ ->
                                    scope.launch(Dispatchers.IO) {
                                        val b = ImageProcessingUtils.decodeOrientedBitmap(context, uri, maxDimension = 1600)
                                        if (b != null) captured += b
                                        withContext(Dispatchers.Main) { shots = captured.size }
                                    }
                                })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LumixCyan),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isArabic) "التقط ($shots)" else "CAPTURE ($shots)", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                if (inProgress || captured.size < 2) return@Button
                                inProgress = true
                                scope.launch(Dispatchers.IO) {
                                    val pano = PanoramaStitcher.stitchHorizontal(captured)
                                    withContext(Dispatchers.Main) {
                                        resultBmp = pano
                                        inProgress = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = captured.size >= 2 && !inProgress
                        ) {
                            Text(if (isArabic) "دمج البانوراما" else "STITCH", color = LumixTextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------- DOCUMENT SCAN ----------------------------------------------------
@Composable
fun DocumentScanScreen(
    controller: CameraController,
    isArabic: Boolean,
    onClose: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var resultBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var inProgress by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF0A0C10),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = LumixTextPrimary)
                }
                Text(
                    if (isArabic) "ماسح المستندات DOCUMENT SCANNER" else "DOCUMENT SCANNER",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                TextButton(
                    onClick = {
                        val r = resultBmp ?: return@TextButton
                        scope.launch(Dispatchers.IO) {
                            val uri = ImageProcessingUtils.saveToMediaStore(context, r, prefix = "LUMIX_DOC")
                            withContext(Dispatchers.Main) {
                                if (uri != null) {
                                    Toast.makeText(context, if (isArabic) "تم الحفظ" else "Saved", Toast.LENGTH_SHORT).show()
                                    onSaved(uri)
                                }
                            }
                        }
                    },
                    enabled = resultBmp != null
                ) {
                    Text(if (isArabic) "حفظ" else "SAVE", color = if (resultBmp != null) LumixCyan else Color.Gray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
                if (resultBmp != null) {
                    AsyncImage(model = resultBmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } else {
                    CameraViewfinder(
                        state = controller.state.collectAsState().value,
                        onBindPreview = { pv -> controller.attachPreview(pv.surfaceProvider) },
                        onTapFocus = { nx, ny -> controller.tapToFocus(nx, ny) },
                        onPinchZoom = { controller.setZoom(it) },
                        onExposureDrag = { }
                    )
                }
                if (inProgress) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = LumixCyan)
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "ضع المستند أمام الكاميرا واضغط التقاط — سيتم تعزيز التباين وتحويل النصوص تلقائيًا"
                        else "Place the document in frame and tap scan — contrast will be enhanced automatically.",
                        color = LumixTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (inProgress) return@Button
                            inProgress = true
                            controller.capturePhoto(onSuccess = { uri, _ ->
                                scope.launch(Dispatchers.IO) {
                                    val b = ImageProcessingUtils.decodeOrientedBitmap(context, uri, maxDimension = 2600)
                                    val enhanced = if (b != null) DocumentScanner.enhanceDocument(b) else null
                                    withContext(Dispatchers.Main) {
                                        resultBmp = enhanced
                                        inProgress = false
                                    }
                                }
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LumixCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "مسح المستند" else "SCAN DOCUMENT", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
