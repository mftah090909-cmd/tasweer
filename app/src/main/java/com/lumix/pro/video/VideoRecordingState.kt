package com.lumix.pro.video

data class VideoRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationMs: Long = 0L,
    val bytesRecorded: Long = 0L,
    val quality: String = "1080p FHD",
    val fps: Int = 30,
    val stabilization: Boolean = true,
    val audioEnabled: Boolean = true,
    val audioBitrateKbps: Int = 192,
    val slowMotion: Boolean = false,
    val remainingMinutes: Long = 0L
)
