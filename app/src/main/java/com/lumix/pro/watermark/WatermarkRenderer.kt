package com.lumix.pro.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.lumix.pro.enhancer.ImageProcessingUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class WatermarkOptions(
    val enabled: Boolean = true,
    val text: String = "SHOT ON LUMIX PRO",
    val showExif: Boolean = true,
    val showDate: Boolean = true,
    val exifInfo: String = "",
    val opacity: Float = 0.9f
)

object WatermarkRenderer {

    fun apply(src: Bitmap, opts: WatermarkOptions): Bitmap {
        if (!opts.enabled) return src
        val out = ImageProcessingUtils.copyBitmap(src)
        val canvas = Canvas(out)
        val w = out.width
        val h = out.height

        val baseSize = max(w, h) * 0.016f
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseSize * 1.05f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            alpha = (opts.opacity * 255).toInt()
            letterSpacing = 0.06f
            setShadowLayer(8f, 2f, 2f, 0xAA000000.toInt())
        }
        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00D1FF.toInt() // cyan accent
            textSize = baseSize * 0.72f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            alpha = (opts.opacity * 240).toInt()
            setShadowLayer(6f, 1f, 1f, 0xAA000000.toInt())
        }
        val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF453A.toInt()
            style = Paint.Style.FILL
            alpha = (opts.opacity * 255).toInt()
        }

        val margin = max(w, h) * 0.028f
        val dotRadius = baseSize * 0.28f
        val x0 = margin
        val y0 = h - margin

        canvas.drawCircle(x0 + dotRadius, y0 - baseSize * 1.4f, dotRadius, paintDot)
        canvas.drawText(opts.text, x0 + dotRadius * 3f + baseSize * 0.35f, y0 - baseSize * 1.25f, paintTitle)

        val subParts = mutableListOf<String>()
        if (opts.showExif && opts.exifInfo.isNotBlank()) subParts += opts.exifInfo
        if (opts.showDate) subParts += SimpleDateFormat("yyyy.MM.dd", Locale.US).format(Date())
        if (subParts.isNotEmpty()) {
            canvas.drawText(subParts.joinToString("   |   "), x0, y0, paintSub)
        }

        return out
    }
}
