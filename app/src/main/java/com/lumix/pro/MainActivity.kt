package com.lumix.pro

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StableCameraApp() }
    }
}

@Composable
private fun StableCameraApp() {
    val context = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { cameraGranted = it }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            if (cameraGranted) {
                CameraHome()
            } else {
                PermissionScreen { permissionLauncher.launch(Manifest.permission.CAMERA) }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF101218)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF66E6FF), modifier = Modifier.size(64.dp))
            Text("لومكس برو", style = MaterialTheme.typography.headlineSmall)
            Text("اسمح للتطبيق باستخدام الكاميرا حتى تبدأ التصوير.", color = Color.LightGray)
            Button(onClick = onGrant) { Text("السماح بالكاميرا") }
        }
    }
}

@Composable
private fun CameraHome() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isFront by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("جاري تجهيز الكاميرا…") }
    var lastUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isFront) {
        val future = runCatching { ProcessCameraProvider.getInstance(context) }.getOrNull()
        if (future == null) {
            status = "الكاميرا غير متاحة على هذا الجهاز"
            return@LaunchedEffect
        }
        future.addListener({
            runCatching {
                val provider = future.get()
                val selector = if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.FHD))
                    .build()
                val video = VideoCapture.withOutput(recorder)
                provider.unbindAll()
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture, video)
                imageCapture = capture
                videoCapture = video
                status = if (camera.cameraInfo.hasFlashUnit()) "جاهز للتصوير" else "جاهز للتصوير — بدون فلاش"
            }.onFailure {
                status = "تعذر تشغيل هذه الكاميرا، جرّب تبديل العدسة"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose { recording?.stop() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
            update = { it.implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 28.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LUMIX PRO", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(status, color = Color(0xFF9BEFFF), style = MaterialTheme.typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isFront = !isFront }) {
                        Icon(Icons.Default.Cameraswitch, "تبديل الكاميرا", tint = Color.White)
                    }
                    Button(
                        onClick = {
                            if (isRecording) {
                                recording?.stop()
                                recording = null
                                isRecording = false
                                status = "تم حفظ الفيديو"
                            } else if (videoCapture != null) {
                                val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val values = ContentValues().apply {
                                    put(MediaStore.Video.Media.DISPLAY_NAME, "LUMIX_$name.mp4")
                                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LUMIX_PRO")
                                }
                                val output = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build()
                                recording = videoCapture!!.output.prepareRecording(context, output).start(ContextCompat.getMainExecutor(context)) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> { isRecording = true; status = "جارٍ تسجيل الفيديو" }
                                        is VideoRecordEvent.Finalize -> if (event.hasError()) { isRecording = false; status = "تعذر حفظ الفيديو" } else { lastUri = event.outputResults.outputUri.toString(); status = "تم حفظ الفيديو" }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(78.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color(0xFFE53935) else Color.White)
                    ) { Icon(if (isRecording) Icons.Default.VideocamOff else Icons.Default.Videocam, "فيديو", tint = if (isRecording) Color.White else Color.Black) }
                    IconButton(onClick = {
                        val capture = imageCapture ?: return@IconButton
                        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "LUMIX_$name.jpg")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/LUMIX_PRO")
                        }
                        val output = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
                        capture.takePicture(output, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) { lastUri = result.savedUri?.toString(); status = "تم حفظ الصورة" }
                            override fun onError(exception: ImageCaptureException) { status = "تعذر حفظ الصورة" }
                        })
                    }) { Icon(Icons.Default.CameraAlt, "التقاط صورة", tint = Color.White, modifier = Modifier.size(34.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                Text(if (lastUri != null) "آخر لقطة محفوظة" else "اضغط الكاميرا لالتقاط صورة أو الفيديو للتسجيل", color = Color.White)
            }
        }
    }
}
