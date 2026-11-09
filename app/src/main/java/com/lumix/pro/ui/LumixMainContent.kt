package com.lumix.pro.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.camera.CameraController
import com.lumix.pro.gallery.MediaItem
import com.lumix.pro.gallery.MediaRepository
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.screens.*
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import com.lumix.pro.utils.PermissionHandler
import java.util.Locale

sealed interface Screen {
    object Camera : Screen
    object Gallery : Screen
    data class MediaDetail(val item: MediaItem) : Screen
    data class PhotoEditor(val uri: Uri) : Screen
    data class VideoEditor(val uri: Uri) : Screen
    data class AiEnhancer(val uri: Uri) : Screen
    object Presets : Screen
    object Diagnostics : Screen
    object GoldenHour : Screen
    object Storage : Screen
    object Settings : Screen
    object Timelapse : Screen
    object LongExposure : Screen
    object Panorama : Screen
    object DocumentScan : Screen
}

@Composable
fun LumixMainContent(
    controller: CameraController,
    repository: MediaRepository,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val isArabic = remember { Locale.getDefault().language == "ar" }
    val navStack = remember { mutableStateListOf<Screen>(Screen.Camera) }
    val currentScreen = navStack.lastOrNull() ?: Screen.Camera

    var latestThumb by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(Unit) { latestThumb = repository.getLatestThumbnail() }

    BackHandler(enabled = navStack.size > 1) { navStack.removeLast() }

    if (!hasCameraPermission) {
        PermissionPrompt(isArabic = isArabic, onGrant = onRequestPermission)
        return
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(tween(140)) togetherWith fadeOut(tween(120))
        },
        label = "screen"
    ) { screen ->
        when (screen) {
            Screen.Camera -> CameraScreen(
                controller = controller,
                isArabic = isArabic,
                latestThumb = latestThumb,
                onOpenGallery = { navStack += Screen.Gallery },
                onOpenSettings = { navStack += Screen.Settings },
                onOpenDiagnostics = { navStack += Screen.Diagnostics },
                onOpenGoldenHour = { navStack += Screen.GoldenHour },
                onMediaCaptured = { uri ->
                    latestThumb = uri
                    val item = MediaItem(
                        id = System.currentTimeMillis(),
                        contentUri = uri,
                        displayName = "LUMIX",
                        mimeType = context.contentResolver.getType(uri) ?: "image/jpeg",
                        width = 0,
                        height = 0,
                        sizeBytes = 0,
                        dateAddedMs = System.currentTimeMillis()
                    )
                    navStack += Screen.MediaDetail(item)
                },
                onStartTimelapse = { navStack += Screen.Timelapse },
                onStartLongExposure = { navStack += Screen.LongExposure },
                onStartPanorama = { navStack += Screen.Panorama },
                onStartDocumentScan = { navStack += Screen.DocumentScan }
            )
            Screen.Gallery -> GalleryScreen(
                repository = repository,
                isArabic = isArabic,
                onOpenDetail = { navStack += Screen.MediaDetail(it) },
                onBack = { navStack.removeLast() }
            )
            is Screen.MediaDetail -> MediaDetailScreen(
                item = screen.item,
                repository = repository,
                isArabic = isArabic,
                onBack = { navStack.removeLast() },
                onEnhance = { navStack += Screen.AiEnhancer(screen.item.contentUri) },
                onEdit = {
                    navStack += if (screen.item.isVideo) Screen.VideoEditor(screen.item.contentUri)
                    else Screen.PhotoEditor(screen.item.contentUri)
                },
                onDeleted = { navStack.removeLast() }
            )
            is Screen.PhotoEditor -> PhotoEditorScreen(
                imageUri = screen.uri,
                isArabic = isArabic,
                onBack = { navStack.removeLast() },
                onExported = { uri ->
                    latestThumb = uri
                    navStack.removeLast()
                }
            )
            is Screen.VideoEditor -> VideoEditorScreen(
                videoUri = screen.uri,
                isArabic = isArabic,
                onBack = { navStack.removeLast() },
                onExported = { navStack.removeLast() }
            )
            is Screen.AiEnhancer -> AiEnhancerScreen(
                imageUri = screen.uri,
                isArabic = isArabic,
                onBack = { navStack.removeLast() },
                onSaved = { uri ->
                    latestThumb = uri
                    navStack.removeLast()
                }
            )
            Screen.Presets -> PresetsScreen(
                controller = controller,
                isArabic = isArabic,
                onBack = { navStack.removeLast() },
                onPresetApplied = { navStack.removeLast() }
            )
            Screen.Diagnostics -> DiagnosticsScreen(
                state = controller.state.collectAsState().value,
                isArabic = isArabic,
                onBack = { navStack.removeLast() }
            )
            Screen.GoldenHour -> GoldenHourScreen(
                isArabic = isArabic,
                onBack = { navStack.removeLast() }
            )
            Screen.Storage -> StorageScreen(
                isArabic = isArabic,
                onBack = { navStack.removeLast() }
            )
            Screen.Settings -> SettingsScreen(
                isArabic = isArabic,
                onBack = { navStack.removeLast() }
            )
            Screen.Timelapse -> TimelapseScreen(
                controller = controller,
                isArabic = isArabic,
                onClose = { navStack.removeLast() },
                onSaved = { uri ->
                    latestThumb = uri
                    navStack.removeLast()
                }
            )
            Screen.LongExposure -> LongExposureScreen(
                controller = controller,
                isArabic = isArabic,
                onClose = { navStack.removeLast() },
                onSaved = { uri ->
                    latestThumb = uri
                    navStack.removeLast()
                }
            )
            Screen.Panorama -> PanoramaScreen(
                controller = controller,
                isArabic = isArabic,
                onClose = { navStack.removeLast() },
                onSaved = { uri ->
                    latestThumb = uri
                    navStack.removeLast()
                }
            )
            Screen.DocumentScan -> DocumentScanScreen(
                controller = controller,
                isArabic = isArabic,
                onClose = { navStack.removeLast() },
                onSaved = { uri ->
                    latestThumb = uri
                    navStack.removeLast()
                }
            )
        }
    }
}

@Composable
private fun PermissionPrompt(isArabic: Boolean, onGrant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C10)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.VideocamOff,
                    contentDescription = null,
                    tint = LumixCyan,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (isArabic) "مطلوب إذن الكاميرا" else "Camera permission required",
                    color = LumixTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isArabic) "يحتاج لومكس برو للوصول إلى الكاميرا لالتقاط صور وفيديوهات احترافية بأعلى جودة يدعمها هاتفك."
                    else "LUMIX PRO needs camera access to capture professional photos and videos at the highest quality your device supports.",
                    color = LumixTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = LumixCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isArabic) "منح الإذن" else "GRANT PERMISSION",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
