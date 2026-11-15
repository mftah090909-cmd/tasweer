package com.lumix.pro

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StudioEditor(uri: Uri, filter: String, onFilter: (String) -> Unit, onRotate: () -> Unit, brightness: Float, onBrightness: (Float) -> Unit, contrast: Float, onContrast: (Float) -> Unit, sharpness: Float, onSharpness: (Float) -> Unit, shadows: Float, onShadows: (Float) -> Unit, highlights: Float, onHighlights: (Float) -> Unit, saturation: Float, onSaturation: (Float) -> Unit, onClose: () -> Unit, onApply: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xF50B1117)).padding(18.dp)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("STUDIO EDITOR", color = Color(0xFF9BEFFF), style = MaterialTheme.typography.titleLarge)
                Text("إغلاق", color = Color.White, modifier = Modifier.clickable { onClose() })
            }
            Spacer(Modifier.height(14.dp))
            AsyncImage(model = uri, contentDescription = "معاينة الصورة", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(330.dp).background(Color.Black, RoundedCornerShape(22.dp)))
            Spacer(Modifier.height(14.dp))
            Text("عدّل من لوحة Studio ثم احفظ نسخة جديدة بدون حذف الأصل", color = Color.LightGray)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Natural", "Clean", "Cinematic", "Teal Orange", "Golden Hour", "Moody", "Pastel", "Vapor", "Noir").forEach { name ->
                    FilterChip(selected = filter == name, onClick = { onFilter(name) }, label = { Text(name) })
                }
            }
            StudioSlider("الضوء", brightness, -.35f.. .35f, onBrightness)
            StudioSlider("التباين", contrast, .65f..1.45f, onContrast)
            StudioSlider("الحدة", sharpness, 0f..1f, onSharpness)
            StudioSlider("الظلال", shadows, -1f..1f, onShadows)
            StudioSlider("الإضاءات", highlights, -1f..1f, onHighlights)
            StudioSlider("التشبع", saturation, 0f..2f, onSaturation)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onRotate) { Text("تدوير 90°") }
                Button(onClick = onApply, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DE1C1))) { Text("حفظ نسخة معدلة", color = Color(0xFF071412)) }
            }
        }
    }
}

@Composable
private fun StudioSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Text("$label  ${"%.2f".format(value)}", color = Color.White, modifier = Modifier.fillMaxWidth())
    Slider(value = value, onValueChange = onValue, valueRange = range, modifier = Modifier.fillMaxWidth())
}
