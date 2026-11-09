package com.lumix.pro.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumix.pro.assistant.AssistantRecommendation
import com.lumix.pro.camera.CameraState
import com.lumix.pro.ui.theme.LumixAmber
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary

@Composable
fun AssistantChip(
    state: CameraState,
    isArabic: Boolean,
    onApplyRecommendation: (AssistantRecommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val rec = state.assistantRecommendation

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Warning banner (shake / overexposure / low light)
        AnimatedVisibility(
            visible = state.shakeWarning || state.overexposedWarning || state.lowLightWarning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val (msg, color) = when {
                state.shakeWarning -> (if (isArabic) "اهتزاز ملحوظ · ارفع سرعة الغالق" else "Shake detected · Increase shutter") to LumixAmber
                state.overexposedWarning -> (if (isArabic) "تعريض مفرط · قلل التعريض" else "Overexposed · Lower EV") to LumixRed
                else -> (if (isArabic) "إضاءة منخفضة · يُنصح بالوضع الليلي" else "Low light · Night mode suggested") to LumixCyan
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "● $msg",
                    color = color,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Scene tip pill
        AnimatedVisibility(
            visible = rec != null && state.sceneDescriptor.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (rec == null) return@AnimatedVisibility
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, LumixCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) rec.tipAr else rec.tip,
                    color = LumixTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LumixCyan)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onApplyRecommendation(rec)
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isArabic) "تطبيق" else "APPLY",
                        color = Color.Black,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
