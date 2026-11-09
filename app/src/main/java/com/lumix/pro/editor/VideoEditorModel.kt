package com.lumix.pro.editor

import com.lumix.pro.videoenhancer.VideoEnhancePreset

data class VideoEditorModel(
    val startMs: Long = 0L,
    val endMs: Long? = null,
    val rotationDegrees: Int = 0,
    val speedMultiplier: Float = 1.0f,
    val preset: VideoEnhancePreset = VideoEnhancePreset.BALANCED,
    val muteAudio: Boolean = false,
    val exportHeight: Int? = null,
    val exportLabel: String = "1080p FHD"
) {
    val isDefault: Boolean
        get() = startMs == 0L && endMs == null && rotationDegrees == 0 &&
            speedMultiplier == 1.0f && preset == VideoEnhancePreset.BALANCED &&
            !muteAudio && exportHeight == null
}
