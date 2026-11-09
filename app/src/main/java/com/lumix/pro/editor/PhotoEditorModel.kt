package com.lumix.pro.editor

import com.lumix.pro.enhancer.ColorBand
import com.lumix.pro.enhancer.HslBandAdjustment
import com.lumix.pro.enhancer.LutLook
import com.lumix.pro.enhancer.ToneCurve

data class PhotoEditorModel(
    // Light
    val exposure: Float = 0f,        // -3 .. +3 EV
    val contrast: Float = 0f,        // -100 .. +100
    val highlights: Float = 0f,      // -100 .. +100
    val shadows: Float = 0f,         // -100 .. +100
    val whites: Float = 0f,          // -100 .. +100
    val blacks: Float = 0f,          // -100 .. +100
    // Color
    val temperature: Float = 0f,     // -100 .. +100 (cool .. warm)
    val tint: Float = 0f,            // -100 .. +100 (green .. magenta)
    val vibrance: Float = 0f,        // -100 .. +100
    val saturation: Float = 0f,      // -100 .. +100
    // Detail
    val clarity: Float = 0f,         // -100 .. +100 (midtone microcontrast)
    val texture: Float = 0f,         // -100 .. +100 (high-freq detail)
    val sharpness: Float = 0f,       // 0 .. 100
    val noiseReduction: Float = 0f,  // 0 .. 100
    // Effects
    val vignette: Float = 0f,        // 0 .. 100
    val grain: Float = 0f,           // 0 .. 100
    // LUT & HSL
    val lutLook: LutLook = LutLook.NATURAL,
    val lutStrength: Float = 1f,
    val hslAdjustments: Map<ColorBand, HslBandAdjustment> = emptyMap(),
    // Geometry
    val rotationDegrees: Int = 0,    // 0, 90, 180, 270
    val cropAspect: Float? = null    // null = free / original
) {
    val isDefault: Boolean
        get() = exposure == 0f && contrast == 0f && highlights == 0f && shadows == 0f &&
            whites == 0f && blacks == 0f && temperature == 0f && tint == 0f &&
            vibrance == 0f && saturation == 0f && clarity == 0f && texture == 0f &&
            sharpness == 0f && noiseReduction == 0f && vignette == 0f && grain == 0f &&
            lutLook == LutLook.NATURAL && hslAdjustments.isEmpty() && rotationDegrees == 0 &&
            cropAspect == null

    fun reset(): PhotoEditorModel = PhotoEditorModel()
}
