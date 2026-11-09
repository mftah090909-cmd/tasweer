package com.lumix.pro.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.sqrt

class SensorsHelper(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var onOrientationChanged: ((rollDeg: Float, pitchDeg: Float) -> Unit)? = null

    private val gravity = FloatArray(3)
    private var lastEmitMs = 0L

    fun start() {
        val s = accelerometer ?: return
        sensorManager?.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Low-pass filter for smooth leveling without jitter
        val alpha = 0.85f
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        val now = System.currentTimeMillis()
        if (now - lastEmitMs < 50) return // ~20 Hz updates
        lastEmitMs = now

        val ax = gravity[0].toDouble()
        val ay = gravity[1].toDouble()
        val az = gravity[2].toDouble()

        val roll = Math.toDegrees(atan2(-ax, ay)).toFloat()
        val pitch = Math.toDegrees(atan2(-ay, sqrt(ax * ax + az * az))).toFloat()

        onOrientationChanged?.invoke(roll, pitch)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
