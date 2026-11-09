package com.lumix.pro.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import com.lumix.pro.utils.PermissionHandler

@Composable
fun SettingsScreen(
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lumix_settings", Context.MODE_PRIVATE) }

    var watermarkEnabled by remember { mutableStateOf(prefs.getBoolean("wm_enabled", true)) }
    var watermarkText by remember { mutableStateOf(prefs.getString("wm_text", "SHOT ON LUMIX PRO") ?: "SHOT ON LUMIX PRO") }
    var wmExif by remember { mutableStateOf(prefs.getBoolean("wm_exif", true)) }
    var wmDate by remember { mutableStateOf(prefs.getBoolean("wm_date", true)) }
    var stabilization by remember { mutableStateOf(prefs.getBoolean("vid_stab", true)) }
    var audioOn by remember { mutableStateOf(prefs.getBoolean("vid_audio", true)) }
    var burstCount by remember { mutableStateOf(prefs.getInt("burst_count", 5)) }

    fun save(key: String, block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
    }

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
                    if (isArabic) "الإعدادات" else "SETTINGS",
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
            // Watermark card
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "العلامة المائية" else "WATERMARK",
                        color = LumixCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingSwitch(if (isArabic) "طباعة العلامة المائية على الصور" else "Stamp watermark on exports", watermarkEnabled) {
                        watermarkEnabled = it
                        save("wm_enabled") { putBoolean("wm_enabled", it) }
                    }
                    if (watermarkEnabled) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = {
                                watermarkText = it
                                save("wm_text") { putString("wm_text", it) }
                            },
                            label = { Text(if (isArabic) "نص العلامة المائية" else "Watermark text") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        SettingSwitch(if (isArabic) "عرض التاريخ" else "Show capture date", wmDate) {
                            wmDate = it
                            save("wm_date") { putBoolean("wm_date", it) }
                        }
                        SettingSwitch(if (isArabic) "عرض بيانات الكاميرا EXIF" else "Show camera EXIF details", wmExif) {
                            wmExif = it
                            save("wm_exif") { putBoolean("wm_exif", it) }
                        }
                    }
                }
            }

            // Video defaults
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "إعدادات الفيديو" else "VIDEO SETTINGS",
                        color = LumixCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingSwitch(if (isArabic) "تثبيت الفيديو الإلكتروني" else "Electronic video stabilization", stabilization) {
                        stabilization = it
                        save("vid_stab") { putBoolean("vid_stab", it) }
                    }
                    SettingSwitch(if (isArabic) "تسجيل الصوت" else "Record audio", audioOn) {
                        audioOn = it
                        save("vid_audio") { putBoolean("vid_audio", it) }
                    }
                }
            }

            // Burst defaults
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "اللقطات المتتابعة (Burst)" else "BURST PHOTOGRAPHY",
                        color = LumixCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(if (isArabic) "عدد اللقطات بالضغطة الطويلة: $burstCount" else "Shots per long-press: $burstCount", color = LumixTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = burstCount.toFloat(),
                        onValueChange = {
                            val v = it.toInt()
                            burstCount = v
                            save("burst_count") { putInt("burst_count", v) }
                        },
                        valueRange = 2f..20f,
                        steps = 8
                    )
                }
            }

            // Privacy & permissions
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "الخصوصية والأذونات" else "PRIVACY & PERMISSIONS",
                        color = LumixCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isArabic) "الألبوم الخاص يخزن داخل المساحة المحمية للتطبيق دون ظهور في المعارض العامة. يُحفظ كل محتوى محليًا ولا يُرفع لأي خادم."
                        else "The private album is stored in the app's protected sandbox and never appears in public galleries. All media stays on-device.",
                        color = LumixTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { PermissionHandler.openAppSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "أذونات التطبيق في إعدادات النظام" else "App permissions in system settings", color = LumixTextPrimary, fontSize = 12.sp)
                    }
                }
            }

            // About
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("LUMIX PRO  1.0.0", color = LumixCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isArabic) "كاميرا ذكية وتصوير حسابي لنظام أندرويد · Camera2/CameraX · Jetpack Compose"
                        else "Professional AI Camera & Computational Photography for Android · Camera2/CameraX · Jetpack Compose",
                        color = LumixTextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = LumixTextPrimary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = LumixCyan, checkedThumbColor = Color.Black)
        )
    }
}
