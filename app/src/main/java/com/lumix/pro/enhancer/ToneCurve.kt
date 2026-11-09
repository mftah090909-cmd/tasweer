package com.lumix.pro.enhancer

import kotlin.math.max

data class CurvePoint(val x: Float, val y: Float)

class ToneCurve(points: List<CurvePoint>) {

    val lut: IntArray = IntArray(256)

    init {
        val sorted = points.sortedBy { it.x }.distinctBy { it.x }
        val pts = when {
            sorted.isEmpty() -> listOf(CurvePoint(0f, 0f), CurvePoint(255f, 255f))
            sorted.size == 1 -> listOf(sorted.first(), CurvePoint(255f, sorted.first().y))
            else -> sorted
        }
        val x = pts.map { it.x.coerceIn(0f, 255f) }.toFloatArray()
        val y = pts.map { it.y.coerceIn(0f, 255f) }.toFloatArray()
        val n = pts.size

        // Fritsch-Carlson monotone cubic spline for overshoot-free tone curves
        val d = FloatArray(n - 1)
        val m = FloatArray(n)
        for (i in 0 until n - 1) {
            val dx = max(1e-5f, x[i + 1] - x[i])
            d[i] = (y[i + 1] - y[i]) / dx
        }
        m[0] = d[0]
        m[n - 1] = d[n - 2]
        for (i in 1 until n - 1) {
            m[i] = (d[i - 1] + d[i]) / 2f
        }
        for (i in 0 until n - 1) {
            if (d[i] == 0f) {
                m[i] = 0f
                m[i + 1] = 0f
            } else {
                val a = m[i] / d[i]
                val b = m[i + 1] / d[i]
                val hyp = a * a + b * b
                if (hyp > 9f) {
                    val t = 3f / kotlin.math.sqrt(hyp)
                    m[i] = t * a * d[i]
                    m[i + 1] = t * b * d[i]
                }
            }
        }

        fun eval(xv: Float): Float {
            if (xv <= x[0]) return y[0]
            if (xv >= x[n - 1]) return y[n - 1]
            var i = 0
            while (i < n - 2 && xv > x[i + 1]) i++
            val h = x[i + 1] - x[i]
            val t = (xv - x[i]) / h
            val t2 = t * t
            val t3 = t2 * t
            val h00 = 2 * t3 - 3 * t2 + 1
            val h10 = t3 - 2 * t2 + t
            val h01 = -2 * t3 + 3 * t2
            val h11 = t3 - t2
            return h00 * y[i] + h10 * h * m[i] + h01 * y[i + 1] + h11 * h * m[i + 1]
        }

        for (i in 0..255) {
            lut[i] = eval(i.toFloat()).toInt().coerceIn(0, 255)
        }
    }

    fun apply(v: Int): Int = lut[v.coerceIn(0, 255)]

    companion object {
        val LINEAR = ToneCurve(listOf(CurvePoint(0f, 0f), CurvePoint(255f, 255f)))
        val CONTRAST = ToneCurve(listOf(CurvePoint(0f, 0f), CurvePoint(64f, 48f), CurvePoint(192f, 212f), CurvePoint(255f, 255f)))
        val FILM = ToneCurve(listOf(CurvePoint(0f, 18f), CurvePoint(70f, 48f), CurvePoint(180f, 220f), CurvePoint(255f, 246f)))
        val LIFT_SHADOWS = ToneCurve(listOf(CurvePoint(0f, 28f), CurvePoint(64f, 96f), CurvePoint(192f, 204f), CurvePoint(255f, 255f)))
    }
}
