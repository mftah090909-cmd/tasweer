package com.lumix.pro.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
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
import com.lumix.pro.enhancer.AiPhotoEnhancer
import com.lumix.pro.enhancer.EnhancementLevel
import com.lumix.pro.enhancer.ImageProcessingUtils
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiEnhancerScreen(
    imageUri: Uri,
    isArabic: Boolean,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBmp by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBmp by remember { mutableStateOf<Bitmap?>(null) }
    var level by remember { mutableStateOf(EnhancementLevel.PROFESSIONAL) }
    var progress by remember { mutableStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var enhanceJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(imageUri) {
        originalBmp = withContext(Dispatchers.IO) {
            ImageProcessingUtils.decodeOrientedBitmap(context, imageUri, maxDimension = 3000)
        }
    }

    fun runEnhancement(l: EnhancementLevel) {
        val src = originalBmp ?: return
        enhanceJob?.cancel()
        processing = true
        progress = 0f
        progressLabel = "Starting…"
        enhanceJob = scope.launch(Dispatchers.IO) {
            val res = AiPhotoEnhancer.enhance(src, l) { p, lbl ->
                progress = p
                progressLabel = lbl
            }
            withContext(Dispatchers.Main) {
                enhancedBmp = res
                processing = false
            }
        }
    }

    LaunchedEffect(originalBmp, level) {
        if (originalBmp != null) runEnhancement(level)
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
                IconButton(onClick = { enhanceJob?.cancel(); onBack() }) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = LumixTextPrimary)
                }
                Text(
                    if (isArabic) "محرك التحسين بالذكاء الاصطناعي" else "AI PHOTO ENHANCER",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = {
                        val res = enhancedBmp ?: return@Button
                        if (saving) return@Button
                        saving = true
                        scope.launch(Dispatchers.IO) {
                            val uri = ImageProcessingUtils.saveToMediaStore(context, res, prefix = "LUMIX_AI")
                            withContext(Dispatchers.Main) {
                                saving = false
                                if (uri != null) {
                                    Toast.makeText(context, if (isArabic) "تم حفظ النسخة المحسّنة في المعرض" else "Enhanced copy saved", Toast.LENGTH_SHORT).show()
                                    onSaved(uri)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LumixCyan),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (saving) "…" else if (isArabic) "حفظ نسخة" else "SAVE COPY",
                        color = Color.Black,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = enhancedBmp ?: originalBmp,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (processing) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = LumixCyan)
                        Spacer(Modifier.height(10.dp))
                        Text(progressLabel, color = LumixTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(4.dp))
                        Text("${(progress * 100).toInt()}%", color = LumixCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Level chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnhancementLevel.values().forEach { l ->
                    val active = l == level
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                            .clickable { level = l }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = l.localizedTitle(isArabic),
                            color = if (active) Color.Black else LumixTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }

            // Description card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(level.localizedTitle(isArabic), color = LumixCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(level.localizedDescription(isArabic), color = LumixTextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
