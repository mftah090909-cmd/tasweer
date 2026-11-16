package com.lumix.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppSettingsSheet(dark: Boolean, language: String, quality: String, onDark: (Boolean) -> Unit, onLanguage: (String) -> Unit, onQuality: (String) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xF21A222B), RoundedCornerShape(26.dp)).border(1.dp, Color.White.copy(alpha = .2f), RoundedCornerShape(26.dp)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(uiText(language, "settings"), color = Color.White)
            LiquidAction(uiText(language, "close"), onClick = onClose)
        }
        Spacer(Modifier.height(10.dp))
        Text(uiText(language, "appearance"), color = Color.LightGray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = dark, onClick = { onDark(true) }, label = { Text(uiText(language, "dark")) })
            FilterChip(selected = !dark, onClick = { onDark(false) }, label = { Text(uiText(language, "light")) })
        }
        Spacer(Modifier.height(8.dp))
        Text(uiText(language, "language"), color = Color.LightGray)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("العربية", "English", "Français", "Español", "Türkçe").forEach { item ->
                FilterChip(selected = language == item, onClick = { onLanguage(item) }, label = { Text(item) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(uiText(language, "quality"), color = Color.LightGray)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("4K", "FHD", "HD", "SD").forEach { item ->
                FilterChip(selected = quality == item, onClick = { onQuality(item) }, label = { Text(item) })
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(uiText(language, "saved"), color = Color(0xFF9BEFFF))
    }
}
