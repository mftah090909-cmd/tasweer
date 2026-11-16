package com.lumix.pro

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val savedLanguage = getSharedPreferences("lumix_app", 0).getString("language", "العربية") ?: "العربية"
        val tag = mapOf("العربية" to "ar", "English" to "en", "Français" to "fr", "Español" to "es", "Türkçe" to "tr")[savedLanguage] ?: "ar"
        resources.configuration.setLocale(Locale.forLanguageTag(tag))
        setContent { StableCameraApp() }
    }
}

@Composable
private fun StableCameraApp() {
    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences("lumix_app", 0) }
    var darkMode by remember { mutableStateOf(appPrefs.getBoolean("dark", true)) }
    var language by remember { mutableStateOf(appPrefs.getString("language", "العربية") ?: "العربية") }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val glassDark = darkColorScheme(primary = Color.White.copy(alpha = .14f), onPrimary = Color.White, primaryContainer = Color.White.copy(alpha = .10f), onPrimaryContainer = Color.White, secondaryContainer = Color.White.copy(alpha = .10f), onSecondaryContainer = Color.White)
    val glassLight = lightColorScheme(primary = Color(0xFF315C63).copy(alpha = .72f), onPrimary = Color.White, primaryContainer = Color.White.copy(alpha = .34f), onPrimaryContainer = Color(0xFF17343A), secondaryContainer = Color.White.copy(alpha = .34f), onSecondaryContainer = Color(0xFF17343A))
    MaterialTheme(colorScheme = if (darkMode) glassDark else glassLight) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            if (granted) CameraHome(darkMode, language, onDark = { value -> darkMode = value; appPrefs.edit().putBoolean("dark", value).apply() }, onLanguage = { value ->
                language = value
                appPrefs.edit().putString("language", value).apply()
                val tag = mapOf("العربية" to "ar", "English" to "en", "Français" to "fr", "Español" to "es", "Türkçe" to "tr")[value] ?: "ar"
                val config = context.resources.configuration
                config.setLocale(Locale.forLanguageTag(tag))
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
                (context as? ComponentActivity)?.recreate()
            }) else PermissionScreen { request.launch(Manifest.permission.CAMERA) }
        }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF101218)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF66E6FF), modifier = Modifier.size(64.dp))
            Text("لومكس برو", style = MaterialTheme.typography.headlineSmall)
            Text("اسمح بالكاميرا لفتح الاستوديو والتصوير.", color = Color.LightGray)
            Button(onClick = onGrant) { Text("السماح بالكاميرا") }
        }
    }
}

@Composable
private fun CameraHome(darkMode: Boolean, language: String, onDark: (Boolean) -> Unit, onLanguage: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var editorOpen by remember { mutableStateOf(false) }
    var lastUri by remember { mutableStateOf<Uri?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { lastUri = uri; editorOpen = true }
    }
    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isFront by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("PHOTO") }
    var flash by remember { mutableStateOf(0) }
    var timer by remember { mutableStateOf(0) }
    var grid by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(1f) }
    var exposure by remember { mutableStateOf(0f) }
    var quality by remember { mutableStateOf("FHD") }
    var showControls by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Natural") }
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var sharpness by remember { mutableStateOf(.18f) }
    var shadows by remember { mutableStateOf(0f) }
    var highlights by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var temperature by remember { mutableStateOf(0f) }
    var grain by remember { mutableStateOf(0f) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("جاري تجهيز الكاميرا…") }
    var rotationDegrees by remember { mutableStateOf(0) }
    var focusLocked by remember { mutableStateOf(false) }
    var lastFocusPoint by remember { mutableStateOf("تلقائي") }
    var settingsOpen by remember { mutableStateOf(false) }
    var assistantVisible by remember { mutableStateOf(true) }
    var aiOpen by remember { mutableStateOf(false) }
    var aiKey by remember { mutableStateOf("") }
    val prefs = remember { context.getSharedPreferences("lumix_preset", 0) }

    LaunchedEffect(isFront, quality, flash) {
        val future = runCatching { ProcessCameraProvider.getInstance(context) }.getOrNull() ?: run {
            status = "الكاميرا غير متاحة"; return@LaunchedEffect
        }
        future.addListener({
            runCatching {
                val provider = future.get()
                val selector = if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val image = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(when (flash) { 1 -> ImageCapture.FLASH_MODE_ON; 2 -> ImageCapture.FLASH_MODE_OFF; else -> ImageCapture.FLASH_MODE_AUTO })
                    .build()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val q = when (quality) { "4K" -> Quality.UHD; "HD" -> Quality.HD; "SD" -> Quality.SD; else -> Quality.FHD }
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(q, FallbackStrategy.lowerQualityThan(q))).build()
                val video = VideoCapture.withOutput(recorder)
                provider.unbindAll()
                val bound = provider.bindToLifecycle(lifecycleOwner, selector, preview, image, video)
                camera = bound; imageCapture = image; videoCapture = video
                zoom = 1f
                status = if (bound.cameraInfo.hasFlashUnit()) "جاهز — ${if (isFront) "أمامية" else "خلفية"}" else "جاهز — بدون فلاش"
            }.onFailure { status = "تعذر تشغيل العدسة؛ جرّب التبديل" }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(zoom, exposure) {
        camera?.cameraControl?.setZoomRatio(zoom.coerceIn(1f, camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 4f))
        camera?.cameraControl?.setExposureCompensationIndex((exposure * 3f).roundToInt())
    }
    LaunchedEffect(flash, camera) { camera?.cameraControl?.enableTorch(flash == 3) }
    DisposableEffect(Unit) { onDispose { recording?.stop() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            { previewView },
            Modifier.fillMaxSize().pointerInput(camera, focusLocked) {
                detectTapGestures { offset ->
                    val activeCamera = camera ?: return@detectTapGestures
                    val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                    val action = androidx.camera.core.FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(if (focusLocked) 1 else 4, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    activeCamera.cameraControl.startFocusAndMetering(action)
                    lastFocusPoint = "تركيز ${offset.x.toInt()}×${offset.y.toInt()}"
                }
            }
        )
        LiveFilterOverlay(selectedFilter, brightness + shadows * .18f - highlights * .12f)
        if (grid) CompositionGrid(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(if (language == "English") "LUMIX PRO" else "لومكس برو", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(status, color = Color(0xFF9BEFFF), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiquidAction("AI", active = aiOpen, onClick = { aiOpen = !aiOpen })
                    LiquidAction("⚙", active = settingsOpen, onClick = { settingsOpen = !settingsOpen })
                    IconButton(onClick = { isFront = !isFront }) { Icon(Icons.Default.Cameraswitch, "تبديل العدسة", tint = Color.White) }
                }
            }
            if (assistantVisible) {
                val advice = recommendShot(mode, exposure, flash, grid)
                AssistantGlassChip(advice) {
                    when (advice.apply) { "الشبكة" -> grid = true; "+EV" -> exposure = (exposure + .7f).coerceAtMost(2f); "-EV" -> exposure = (exposure - .7f).coerceAtLeast(-2f); "Auto" -> flash = 0 }
                    assistantVisible = false
                }
            }
            if (showControls) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).liquidGlass(RoundedCornerShape(28.dp)).padding(12.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("PHOTO" to uiText(language, "photo"), "VIDEO" to uiText(language, "video"), "PRO" to "Pro").forEach { (key, label) ->
                            FilterChip(selected = mode == key, onClick = { mode = key }, label = { Text(label) })
                        }
                    }
                    if (mode != "VIDEO") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("فلتر المعاينة", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            FilterChip(selected = showFilterPanel, onClick = { showFilterPanel = !showFilterPanel }, label = { Text(if (showFilterPanel) uiText(language, "hide") else uiText(language, "tune")) })
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Natural", "Clean", "Cinematic", "Teal Orange", "Golden Hour", "Moody", "Pastel", "Vapor", "Warm", "Cool", "Noir", "Neon", "Faded").forEach { name ->
                                FilterChip(selected = selectedFilter == name, onClick = { selectedFilter = name }, label = { Text(name) })
                            }
                        }
                        if (showFilterPanel) {
                            Text("سطوع ${"%.2f".format(brightness)}", color = Color.White, modifier = Modifier.fillMaxWidth())
                            Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -.35f.. .35f, modifier = Modifier.fillMaxWidth())
                            Text("تباين ${"%.2f".format(contrast)}", color = Color.White, modifier = Modifier.fillMaxWidth())
                            Slider(value = contrast, onValueChange = { contrast = it }, valueRange = .65f..1.45f, modifier = Modifier.fillMaxWidth())
                            Text("حدة ${"%.2f".format(sharpness)}", color = Color.White, modifier = Modifier.fillMaxWidth())
                            Slider(value = sharpness, onValueChange = { sharpness = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
                            Text("ظلال ${"%.2f".format(shadows)}", color = Color.White, modifier = Modifier.fillMaxWidth())
                            Slider(value = shadows, onValueChange = { shadows = it }, valueRange = -1f..1f, modifier = Modifier.fillMaxWidth())
                            Text("إضاءات قوية ${"%.2f".format(highlights)}", color = Color.White, modifier = Modifier.fillMaxWidth())
                            Slider(value = highlights, onValueChange = { highlights = it }, valueRange = -1f..1f, modifier = Modifier.fillMaxWidth())
                            Text("تشبع ${"%.2f".format(saturation)}", color = Color.White, modifier = Modifier.fillMaxWidth())
                            Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..2f, modifier = Modifier.fillMaxWidth())
                            Text("حرارة اللون ${"%.2f".format(temperature)}", color = Color.White, modifier = Modifier.fillMaxWidth())
                            Slider(value = temperature, onValueChange = { temperature = it }, valueRange = -1f..1f, modifier = Modifier.fillMaxWidth())
                            Text("حبيبات سينمائية ${"%.2f".format(grain)}", color = Color.White, modifier = Modifier.fillMaxWidth())
                            Slider(value = grain, onValueChange = { grain = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { selectedFilter = "Clean"; brightness = .05f; contrast = 1.1f; shadows = .14f; highlights = -.12f; saturation = 1.08f; sharpness = .35f }) { Text("تحسين تلقائي") }
                                Button(onClick = { brightness = 0f; contrast = 1f; sharpness = .18f; shadows = 0f; highlights = 0f; saturation = 1f; temperature = 0f; grain = 0f; selectedFilter = "Natural"; rotationDegrees = 0 }) { Text("إعادة ضبط") }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    prefs.edit().putString("filter", selectedFilter).putFloat("brightness", brightness).putFloat("contrast", contrast).putFloat("sharpness", sharpness).putFloat("shadows", shadows).putFloat("highlights", highlights).putFloat("saturation", saturation).putFloat("temperature", temperature).putFloat("grain", grain).apply()
                                    status = "تم حفظ الوصفة"
                                }) { Text("حفظ الوصفة") }
                                Button(onClick = {
                                    selectedFilter = prefs.getString("filter", selectedFilter) ?: selectedFilter
                                    brightness = prefs.getFloat("brightness", brightness); contrast = prefs.getFloat("contrast", contrast); sharpness = prefs.getFloat("sharpness", sharpness); shadows = prefs.getFloat("shadows", shadows); highlights = prefs.getFloat("highlights", highlights); saturation = prefs.getFloat("saturation", saturation); temperature = prefs.getFloat("temperature", temperature); grain = prefs.getFloat("grain", grain)
                                    status = "تم استدعاء الوصفة"
                                }) { Text("استدعاء الوصفة") }
                            }
                            if (lastUri != null && !processing) {
                                Button(onClick = {
                                    processing = true
                                    status = "تطبيق التعديلات…"
                                    scope.launch {
                                        val edited = runCatching { PhotoFilterEngine.processAndSave(context, lastUri, selectedFilter, brightness, contrast, sharpness, shadows, highlights, saturation, temperature, grain, rotationDegrees) }.getOrNull()
                                        if (edited != null) lastUri = edited
                                        processing = false
                                        status = if (edited != null) "تم حفظ تعديل جديد" else "تعذر تطبيق التعديل"
                                    }
                                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DE1C1))) { Text("تطبيق على آخر لقطة", color = Color(0xFF071412)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { flash = (flash + 1) % 4 }) { Icon(when (flash) { 1 -> Icons.Default.FlashOn; 2 -> Icons.Default.FlashOff; 3 -> Icons.Default.FlashOn; else -> Icons.Default.FlashAuto }, "الفلاش", tint = Color.White) }
                        IconButton(onClick = { grid = !grid }) { Icon(Icons.Default.GridOn, "شبكة التكوين", tint = if (grid) Color(0xFF66E6FF) else Color.White) }
                        IconButton(onClick = { timer = when (timer) { 0 -> 3; 3 -> 5; 5 -> 10; else -> 0 } }) { Icon(Icons.Default.Timer, "المؤقت", tint = if (timer > 0) Color(0xFF66E6FF) else Color.White) }
                        Text(if (timer == 0) "مؤقت: off" else "مؤقت: ${timer}s", color = Color.White)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        GlassLabel("${if (focusLocked) "قفل" else "AF/AE"} · $lastFocusPoint", focusLocked)
                        Button(onClick = { focusLocked = !focusLocked; status = if (focusLocked) "تم قفل التركيز والتعريض" else "التركيز تلقائي" }) { Text(if (focusLocked) "فتح القفل" else "قفل AF/AE") }
                    }
                    Text("زوم ${"%.1f".format(zoom)}x", color = Color.White, modifier = Modifier.fillMaxWidth())
                    Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 1f..(camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 4f), modifier = Modifier.fillMaxWidth())
                    if (mode == "PRO") {
                        Text("التعريض ${if (exposure >= 0) "+" else ""}${"%.1f".format(exposure)} EV", color = Color.White, modifier = Modifier.fillMaxWidth())
                        Slider(value = exposure, onValueChange = { exposure = it }, valueRange = -2f..2f, modifier = Modifier.fillMaxWidth())
                    }
                    if (mode == "VIDEO") {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("4K", "FHD", "HD", "SD").forEach { q -> FilterChip(selected = quality == q, onClick = { quality = q }, label = { Text(q) }) } }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    IconButton(onClick = { lastUri?.let { uri -> context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, context.contentResolver.getType(uri) ?: "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } }) { Icon(Icons.Default.PhotoLibrary, "فتح آخر لقطة", tint = Color.White, modifier = Modifier.size(30.dp)) }
                    Button(onClick = { pickImage.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0x6628D7C0))) { Text(uiText(language, "import")) }
                    Button(
                        onClick = {
                            if (mode == "VIDEO") {
                                if (isRecording) { recording?.stop(); recording = null; isRecording = false; status = "تم حفظ الفيديو" }
                                else videoCapture?.let { vc ->
                                    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    val values = ContentValues().apply { put(MediaStore.Video.Media.DISPLAY_NAME, "LUMIX_$name.mp4"); put(MediaStore.Video.Media.MIME_TYPE, "video/mp4"); if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LUMIX_PRO") }
                                    val output = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build()
                                    recording = vc.output.prepareRecording(context, output).start(ContextCompat.getMainExecutor(context)) { event -> when (event) { is VideoRecordEvent.Start -> { isRecording = true; status = "جارٍ تسجيل الفيديو" }; is VideoRecordEvent.Finalize -> { isRecording = false; if (!event.hasError()) { lastUri = event.outputResults.outputUri; status = "تم حفظ الفيديو" } else status = "فشل حفظ الفيديو" } } }
                                }
                            } else {
                                val capture = imageCapture ?: return@Button
                                scope.launch { if (timer > 0) delay(timer * 1000L); val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()); val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "LUMIX_$name.jpg"); put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/LUMIX_PRO") }; val output = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build(); capture.takePicture(output, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback { override fun onImageSaved(result: ImageCapture.OutputFileResults) { lastUri = result.savedUri; status = "تتم معالجة ${selectedFilter}…"; processing = true; scope.launch { val edited = PhotoFilterEngine.processAndSave(context, result.savedUri, selectedFilter, brightness, contrast, sharpness, shadows, highlights, saturation, temperature, grain); if (edited != null) lastUri = edited; processing = false; status = if (edited != null) "تم حفظ الصورة والنسخة المعدلة" else "تم حفظ الصورة الأصلية" } }; override fun onError(exception: ImageCaptureException) { status = "فشل حفظ الصورة" } }) }
                            }
                        }, modifier = Modifier.size(82.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(if (isRecording) Color(0xFFE53935) else Color.White)
                    ) { Icon(if (isRecording) Icons.Default.VideocamOff else if (mode == "VIDEO") Icons.Default.Videocam else Icons.Default.CameraAlt, "التقاط", tint = if (isRecording || mode == "VIDEO") Color.White else Color.Black, modifier = Modifier.size(34.dp)) }
                    IconButton(onClick = { showControls = !showControls }) { Text(if (showControls) "−" else "+", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (processing) "جاري إنشاء النسخة المعدلة…" else if (lastUri == null) "جاهز للتصوير" else "تم الحفظ في المعرض", color = Color.White)
                    if (lastUri != null && !processing) {
                        Text("تعديل آخر لقطة", color = Color(0xFF9BEFFF), modifier = Modifier.clickable {
                            showControls = true; showFilterPanel = true; editorOpen = true
                        })
                    }
                }
            }
        }
        if (editorOpen && lastUri != null) {
            StudioEditor(
                uri = lastUri!!,
                filter = selectedFilter,
                onFilter = { selectedFilter = it },
                onRotate = { rotationDegrees = (rotationDegrees + 90) % 360 },
                brightness = brightness, onBrightness = { brightness = it },
                contrast = contrast, onContrast = { contrast = it },
                sharpness = sharpness, onSharpness = { sharpness = it },
                shadows = shadows, onShadows = { shadows = it },
                highlights = highlights, onHighlights = { highlights = it },
                saturation = saturation, onSaturation = { saturation = it },
                onClose = { editorOpen = false },
                onApply = {
                    processing = true
                    status = "تطبيق تعديلات Studio…"
                    scope.launch {
                        val edited = runCatching { PhotoFilterEngine.processAndSave(context, lastUri, selectedFilter, brightness, contrast, sharpness, shadows, highlights, saturation, temperature, grain, rotationDegrees) }.getOrNull()
                        if (edited != null) lastUri = edited
                        processing = false
                        status = if (edited != null) "تم حفظ الصورة المعدلة" else "تعذر حفظ التعديل"
                        editorOpen = false
                    }
                }
            )
        }
        if (settingsOpen) {
            AppSettingsSheet(darkMode, language, quality, onDark = onDark, onLanguage = onLanguage, onQuality = { quality = it }, onClose = { settingsOpen = false })
        }
        if (aiOpen) {
            AiPhotographyChat(aiKey, onApiKey = { aiKey = it }, onClose = { aiOpen = false })
        }
    }
}

@Composable
private fun CompositionGrid(modifier: Modifier) {
    Canvas(modifier) {
        val thirdW = size.width / 3f; val thirdH = size.height / 3f
        drawLine(Color.White.copy(alpha = .35f), Offset(thirdW, 0f), Offset(thirdW, size.height), strokeWidth = 1f)
        drawLine(Color.White.copy(alpha = .35f), Offset(thirdW * 2, 0f), Offset(thirdW * 2, size.height), strokeWidth = 1f)
        drawLine(Color.White.copy(alpha = .35f), Offset(0f, thirdH), Offset(size.width, thirdH), strokeWidth = 1f)
        drawLine(Color.White.copy(alpha = .35f), Offset(0f, thirdH * 2), Offset(size.width, thirdH * 2), strokeWidth = 1f)
    }
}

@Composable
private fun LiveFilterOverlay(filter: String, brightness: Float) {
    val tint = when (filter) {
        "Warm" -> Color(0xFFFF9D5C)
        "Cool" -> Color(0xFF5CA9FF)
        "Cinematic" -> Color(0xFFB87555)
        "Teal Orange" -> Color(0xFF2AAFA0)
        "Golden Hour" -> Color(0xFFFFB347)
        "Moody" -> Color(0xFF35404A)
        "Pastel" -> Color(0xFFF2B6C6)
        "Vapor" -> Color(0xFF9C5CFF)
        "Noir" -> Color.Black
        "Neon" -> Color(0xFFB44CFF)
        "Faded" -> Color(0xFFD9C3A5)
        "Clean" -> Color.White
        else -> Color.Transparent
    }
    val alpha = when (filter) { "Noir" -> .22f; "Natural" -> 0f; else -> .10f } + (brightness * .10f)
    if (alpha > 0f) Box(Modifier.fillMaxSize().background(tint.copy(alpha = alpha.coerceIn(0f, .28f))))
}
