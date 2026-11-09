package com.lumix.pro.ui.screens

import android.location.Location
import android.location.LocationManager
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
import com.lumix.pro.assistant.GoldenHourCalculator
import com.lumix.pro.ui.components.GlassCard
import com.lumix.pro.ui.theme.LumixAmber
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GoldenHourScreen(
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val (lat, lon) = remember {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
        var loc: Location? = null
        try {
            loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: SecurityException) { }
        loc?.let { it.latitude to it.longitude } ?: (24.7136 to 46.6753) // default Riyadh
    }

    val times = remember(lat, lon) { GoldenHourCalculator.compute(lat, lon) }
    val tf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

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
                    if (isArabic) "حاسبة الساعة الذهبية والزرقاء" else "GOLDEN & BLUE HOUR",
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
            // Live status card
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "الحالة الآن" else "STATUS NOW",
                        color = LumixAmber,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) times.statusNowAr else times.statusNow,
                        color = LumixTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GPS: %.3f, %.3f".format(lat, lon),
                        color = LumixTextSecondary,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Timetable card
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "جدول اليوم الفلكي" else "TODAY'S SOLAR TIMETABLE",
                        color = LumixCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    TimeRow("Sunrise", times.sunrise?.let { tf.format(it) } ?: "—")
                    TimeRow("Morning Golden Hour ends", times.goldenHourMorningEnd?.let { tf.format(it) } ?: "—")
                    TimeRow("Solar Noon", times.solarNoon?.let { tf.format(it) } ?: "—")
                    TimeRow("Evening Golden Hour starts", times.goldenHourEveningStart?.let { tf.format(it) } ?: "—")
                    TimeRow("Sunset", times.sunset?.let { tf.format(it) } ?: "—")
                    TimeRow("Blue Hour ends", times.blueHourEnd?.let { tf.format(it) } ?: "—")
                }
            }

            // Photography tips
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "نصائح التصوير في الساعة الذهبية" else "GOLDEN HOUR SHOOTING TIPS",
                        color = LumixCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Tip(if (isArabic) "اضبط التعريض على -0.5 EV لحفظ تدرجات لون السماء." else "Set EV -0.5 to keep warm sky gradient saturation.")
                    Tip(if (isArabic) "وجّه الكاميرا نحو الشمس لظلال دافئة مع تعريض HDR." else "Shoot towards the sun for rim light, with HDR on.")
                    Tip(if (isArabic) "في الساعة الزرقاء استخدم وضع ليلي مع حامل ثابت." else "Blue hour + Night mode + tripod gives dreamy cityscapes.")
                }
            }
        }
    }
}

@Composable
private fun TimeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LumixTextSecondary, fontSize = 12.sp)
        Text(value, color = LumixTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Tip(text: String) {
    Text("• $text", color = LumixTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp))
}
