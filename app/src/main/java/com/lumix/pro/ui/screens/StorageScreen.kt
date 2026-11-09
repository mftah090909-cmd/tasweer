package com.lumix.pro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import com.lumix.pro.utils.StorageUtils

@Composable
fun StorageScreen(
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rep = remember { StorageUtils.report(context) }

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
                    if (isArabic) "إدارة مساحة التخزين" else "STORAGE MANAGEMENT",
                    color = LumixTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Donut card
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.width / 2f - 12.dp.toPx()
                            val c = center
                            drawCircle(Color.White.copy(alpha = 0.12f), radius = r, style = Stroke(20.dp.toPx()))
                            drawArc(
                                color = LumixCyan,
                                startAngle = -90f,
                                sweepAngle = (rep.freePercent / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(20.dp.toPx(), cap = StrokeCap.Round),
                                topLeft = androidx.compose.ui.geometry.Offset(c.x - r, c.y - r),
                                size = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${rep.freePercent}%", color = LumixTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(if (isArabic) "متاح" else "free", color = LumixTextSecondary, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("${rep.freeFormatted} free of ${rep.totalFormatted}", color = LumixTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Estimation card
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "تقديرات السعة المتبقية" else "ESTIMATED REMAINING CAPACITY",
                        color = LumixCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    EstRow("Photos (high-res)", "≈ ${"%,d".format(rep.estimatedPhotoShotsRemaining)} shots")
                    val vh = rep.estimatedVideoMinutesRemaining / 60
                    val vm = rep.estimatedVideoMinutesRemaining % 60
                    EstRow("Video (1080p FHD)", "≈ ${vh}h ${vm}m")
                    EstRow("Average photo size", "≈ 7.5 MB (JPEG)")
                    EstRow("Storage folder", "DCIM/LUMIX_PRO")
                }
            }
        }
    }
}

@Composable
private fun EstRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LumixTextSecondary, fontSize = 12.sp)
        Text(value, color = LumixTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
