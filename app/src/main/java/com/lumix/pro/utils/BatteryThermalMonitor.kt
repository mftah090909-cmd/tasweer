package com.lumix.pro.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float
)

data class ThermalInfo(
    val statusLabel: String,
    val isThrottled: Boolean
)

class BatteryThermalMonitor(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    var onThermalChanged: ((ThermalInfo) -> Unit)? = null

    private val thermalListener: PowerManager.OnThermalStatusChangedListener? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PowerManager.OnThermalStatusChangedListener { status ->
                val (label, throttled) = when (status) {
                    PowerManager.THERMAL_STATUS_NONE -> "Nominal" to false
                    PowerManager.THERMAL_STATUS_LIGHT -> "Light warm" to false
                    PowerManager.THERMAL_STATUS_MODERATE -> "Moderate" to false
                    PowerManager.THERMAL_STATUS_SEVERE -> "Severe · Throttling" to true
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Critical · Throttling" to true
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency" to true
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown imminent" to true
                    else -> "Unknown" to false
                }
                onThermalChanged?.invoke(ThermalInfo(label, throttled))
            }
        } else null
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            thermalListener?.let { powerManager?.addThermalStatusListener(ContextCompat.getMainExecutor(context), it) }
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            thermalListener?.let { powerManager?.removeThermalStatusListener(it) }
        }
    }

    fun readBattery(): BatteryInfo {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val battery = context.registerReceiver(null, filter)
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = ((level.toFloat() / scale.toFloat()) * 100).toInt()
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val tempRaw = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return BatteryInfo(
            levelPercent = percent.coerceIn(0, 100),
            isCharging = plugged != 0,
            temperatureCelsius = tempRaw / 10f
        )
    }
}
