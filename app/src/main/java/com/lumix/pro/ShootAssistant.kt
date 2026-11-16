package com.lumix.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ShootAdvice(val title: String, val detail: String, val apply: String)

fun recommendShot(mode: String, exposure: Float, flash: Int, grid: Boolean): ShootAdvice = when {
    mode == "VIDEO" -> ShootAdvice("مساعد الفيديو", "ثبّت الهاتف وجرّب FHD لتوازن الجودة والمساحة", "FHD")
    exposure < -.8f -> ShootAdvice("المشهد مظلم", "ارفع التعريض قليلًا أو استخدم الفلاش التلقائي", "+EV")
    exposure > .8f -> ShootAdvice("إضاءة قوية", "خفّض التعريض لحماية تفاصيل السماء والوجه", "-EV")
    !grid -> ShootAdvice("تكوين أفضل", "فعّل شبكة الأثلاث لخط أفق وتكوين أدق", "الشبكة")
    flash == 3 -> ShootAdvice("الفلاش", "الوضع Torch مناسب للإضاءة المستمرة فقط", "Auto")
    else -> ShootAdvice("لقطة جاهزة", "اضغط على العنصر للتركيز ثم التقط الصورة", "AF/AE")
}

@Composable
fun AssistantGlassChip(advice: ShootAdvice, onApply: () -> Unit) {
    Row(Modifier.background(Color(0xB51A2D35), RoundedCornerShape(20.dp)).border(1.dp, Color(0x667CF4D6), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("AI · ${advice.title}\n${advice.detail}", color = Color.White)
        Text(advice.apply, color = Color(0xFF7CF4D6), modifier = Modifier.clickable { onApply() }.padding(4.dp))
    }
}
