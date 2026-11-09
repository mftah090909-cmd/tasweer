package com.lumix.pro.presets

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "camera_presets")
data class CameraPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iso: Int? = null,
    val shutterNs: Long? = null,
    val exposureIndex: Int = 0,
    val wbKelvin: Int? = null,
    val focusDistance: Float? = null,
    val modeName: String = "PRO",
    val lensFacingName: String = "BACK",
    val createdMs: Long = System.currentTimeMillis()
)
