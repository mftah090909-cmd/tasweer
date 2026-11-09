package com.lumix.pro.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.lumix.pro.gallery.ExifInspector
import com.lumix.pro.gallery.MediaItem
import com.lumix.pro.gallery.MediaRepository
import com.lumix.pro.gallery.MediaShareHelper
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary

@Composable
fun MediaDetailScreen(
    item: MediaItem,
    repository: MediaRepository,
    isArabic: Boolean,
    onBack: () -> Unit,
    onEnhance: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    var isFav by remember { mutableStateOf(item.isFavorite) }
    var showExifSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val exif = remember(item) {
        if (!item.isVideo) ExifInspector.inspect(context, item.contentUri, item.sizeBytes) else null
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Row {
                    if (exif != null) {
                        IconButton(onClick = { showExifSheet = !showExifSheet }) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = if (showExifSheet) LumixCyan else Color.White)
                        }
                    }
                    IconButton(onClick = {
                        isFav = repository.toggleFavorite(item.id)
                    }) {
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) LumixRed else Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.isVideo) {
                        ActionButton(
                            icon = Icons.Rounded.AutoFixHigh,
                            label = if (isArabic) "تحسين بالذكاء الاصطناعي" else "Enhance AI",
                            tint = LumixCyan,
                            onClick = onEnhance
                        )
                    }
                    ActionButton(
                        icon = Icons.Rounded.Tune,
                        label = if (isArabic) "تعديل" else "Edit",
                        tint = LumixTextPrimary,
                        onClick = onEdit
                    )
                    ActionButton(
                        icon = Icons.Rounded.Share,
                        label = if (isArabic) "مشاركة" else "Share",
                        tint = LumixTextPrimary,
                        onClick = { MediaShareHelper.shareItem(context, item) }
                    )
                    ActionButton(
                        icon = Icons.Rounded.Delete,
                        label = if (isArabic) "حذف" else "Delete",
                        tint = LumixRed,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (item.isVideo) {
                ExoVideoPlayer(uri = item.contentUri, context = context, modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 8f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.contentUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    )
                }
            }

            if (showExifSheet && exif != null) {
                ExifPanel(
                    exif = exif,
                    isArabic = isArabic,
                    onClose = { showExifSheet = false },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (isArabic) "حذف هذا العنصر؟" else "Delete this item?", color = LumixTextPrimary) },
            text = { Text(if (isArabic) "لا يمكن التراجع عن هذه العملية." else "This action cannot be undone.", color = LumixTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    repository.deleteItem(item)
                    onDeleted()
                }) {
                    Text(if (isArabic) "حذف" else "Delete", color = LumixRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = LumixTextPrimary)
                }
            },
            containerColor = Color(0xEE121722)
        )
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = LumixTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExoVideoPlayer(uri: android.net.Uri, context: Context, modifier: Modifier) {
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                keepScreenOn = true
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ExifPanel(exif: com.lumix.pro.gallery.ExifData, isArabic: Boolean, onClose: () -> Unit, modifier: Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (isArabic) "بيانات الصورة (EXIF)" else "PHOTO DETAILS (EXIF)",
                    color = LumixCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text("✕", color = LumixTextSecondary, modifier = Modifier.clickable { onClose() })
            }
            Spacer(Modifier.height(10.dp))
            exif.cameraMakeModel?.let { ExifRow("Camera", it) }
            exif.lensModel?.let { ExifRow("Lens", it) }
            val exposureParts = listOfNotNull(exif.shutterSpeed, exif.aperture, exif.iso, exif.exposureBias, exif.focalLength)
            if (exposureParts.isNotEmpty()) ExifRow("Exposure", exposureParts.joinToString("   ·   "))
            exif.dimensions?.let { ExifRow("Resolution", "$it  (${exif.megapixels})") }
            exif.fileSizeFormatted?.let { ExifRow("File size", it) }
            exif.dateTime?.let { ExifRow("Date", it) }
            exif.colorSpace?.let { ExifRow("Color space", it) }
            if (exif.latitude != null && exif.longitude != null) {
                ExifRow("GPS", "%.4f, %.4f".format(exif.latitude, exif.longitude))
            }
        }
    }
}

@Composable
private fun ExifRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label.uppercase(), color = LumixTextSecondary, fontSize = 9.5.sp, letterSpacing = 0.8.sp)
        Text(value, color = LumixTextPrimary, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace)
    }
}
