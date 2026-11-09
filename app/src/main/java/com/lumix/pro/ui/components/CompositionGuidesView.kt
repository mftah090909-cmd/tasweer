package com.lumix.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lumix.pro.camera.GridType

private const val PHI = 0.6180339887f

@Composable
fun CompositionGuidesView(
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    if (gridType == GridType.NONE) return

    val guideColor = Color.White.copy(alpha = 0.38f)
    val accentColor = Color(0x6600D1FF)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.2f)

        when (gridType) {
            GridType.NONE -> Unit

            GridType.RULE_OF_THIRDS -> {
                val x1 = w / 3f
                val x2 = 2f * w / 3f
                val y1 = h / 3f
                val y2 = 2f * h / 3f
                drawLine(guideColor, Offset(x1, 0f), Offset(x1, h), strokeWidth = 1.2f)
                drawLine(guideColor, Offset(x2, 0f), Offset(x2, h), strokeWidth = 1.2f)
                drawLine(guideColor, Offset(0f, y1), Offset(w, y1), strokeWidth = 1.2f)
                drawLine(guideColor, Offset(0f, y2), Offset(w, y2), strokeWidth = 1.2f)
                listOf(Offset(x1, y1), Offset(x2, y1), Offset(x1, y2), Offset(x2, y2)).forEach {
                    drawCircle(accentColor, radius = 5f, center = it)
                }
            }

            GridType.GOLDEN_RATIO -> {
                val x1 = w * (1f - PHI)
                val x2 = w * PHI
                val y1 = h * (1f - PHI)
                val y2 = h * PHI
                drawLine(guideColor, Offset(x1, 0f), Offset(x1, h), strokeWidth = 1.2f)
                drawLine(guideColor, Offset(x2, 0f), Offset(x2, h), strokeWidth = 1.2f)
                drawLine(guideColor, Offset(0f, y1), Offset(w, y1), strokeWidth = 1.2f)
                drawLine(guideColor, Offset(0f, y2), Offset(w, y2), strokeWidth = 1.2f)
                drawGoldenSpiral(guideColor, w, h)
            }

            GridType.DIAGONALS -> {
                drawLine(guideColor, Offset(0f, 0f), Offset(w, h), strokeWidth = 1.2f)
                drawLine(guideColor, Offset(w, 0f), Offset(0f, h), strokeWidth = 1.2f)
                drawLine(guideColor.copy(alpha = 0.22f), Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 1f)
                drawLine(guideColor.copy(alpha = 0.22f), Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
            }

            GridType.KEYSTONE -> {
                val m = 18f
                drawRect(guideColor, Offset(m, m), Size(w - 2 * m, h - 2 * m), style = stroke)
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.25f)
                    lineTo(w * 0.85f, h * 0.25f)
                    lineTo(w * 0.95f, h * 0.88f)
                    lineTo(w * 0.05f, h * 0.88f)
                    close()
                }
                drawPath(path, accentColor, style = stroke)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoldenSpiral(color: Color, w: Float, h: Float) {
    var x = 0f
    var y = 0f
    var sw = w
    var sh = h
    val p = Path()
    var startAngle = 180f
    for (i in 0 until 6) {
        val side = if (sw >= sh) sh else sw
        val nextStartAngle = startAngle - 90f
        p.arcTo(Rect(x, y, x + 2 * side, y + 2 * side), startAngle, -90f, forceMoveTo = (i == 0))
        startAngle = nextStartAngle
        if (sw >= sh) { x += side; sw -= side } else { y += side; sh -= side }
    }
    drawPath(p, color.copy(alpha = 0.5f), style = Stroke(1.4f, cap = StrokeCap.Round))
}
