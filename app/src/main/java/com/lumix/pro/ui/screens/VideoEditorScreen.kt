package com.lumix.pro.ui.screens

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lumix.pro.editor.VideoEditorModel
import com.lumix.pro.editor.VideoEditorProcessor
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.components.RangeSliderPro
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import com.lumix.pro.videoenhancer.VideoEnhancePreset
import kotlinx.coroutines.launch

private val SPEEDS = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 4f)
private val RESOLUTIONS = listOf(
    "Original" to null,
    "4K 2160p" to 2160,
    "1080p FHD" to 1080,
    "720p HD" to 720,
    "480p SD" to 480
)

@Composable
fun VideoEditorScreen(
    videoUri: Uri,
    isArabic: Boolean,
    onBack: () -> Unit,
    onExported: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { VideoEditorProcessor(context) }
    var model by remember { mutableStateOf(VideoEditorModel()) }
    var progress by remember { mutableStateOf(0f) }
    var exporting by remember { mutableStateOf(false) }

    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose {
            processor.cancel()
            player.release()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0C10),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = LumixTextPrimary)
                }
                Text(
                    if (isArabic) "محرر الفيديو" else "VIDEO EDITOR",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = {
                        if (exporting) return@Button
                        exporting = true
                        progress = 0f
                        scope.launch {
                            processor.export(
                                videoUri, model,
                                onProgress = { progress = it },
                                onDone = {
                                    exporting = false
                                    Toast.makeText(context, if (isArabic) "تم حفظ الفيديو المعدّل" else "Edited video saved", Toast.LENGTH_SHORT).show()
                                    onExported(it)
                                },
                                onError = {
                                    exporting = false
                                    Toast.makeText(context, it.message ?: "Export failed", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LumixCyan),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "حفظ" else "EXPORT", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Player
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            )

            // Export progress dialog
            if (exporting) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Processing ${(progress * 100).toInt()}%", color = LumixCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = LumixCyan)
                }
            }

            // Controls
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Speed chips
                    Text("SPEED", color = LumixTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SPEEDS.forEach { s ->
                            val active = model.speedMultiplier == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        model = model.copy(speedMultiplier = s)
                                        player.setPlaybackSpeed(s)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text("${s}×", color = if (active) Color.Black else LumixTextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Preset chips
                    Text("LOOK", color = LumixTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoEnhancePreset.values().forEach { p ->
                            val active = model.preset == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                                    .clickable { model = model.copy(preset = p) }
                                    .padding(horizontal = 11.dp, vertical = 7.dp)
                            ) {
                                Text(p.localized(isArabic), color = if (active) Color.Black else LumixTextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Resolution chips
                    Text("EXPORT QUALITY", color = LumixTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RESOLUTIONS.forEach { (label, h) ->
                            val active = model.exportHeight == h
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                                    .clickable { model = model.copy(exportHeight = h, exportLabel = label) }
                                    .padding(horizontal = 11.dp, vertical = 7.dp)
                            ) {
                                Text(label, color = if (active) Color.Black else LumixTextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
