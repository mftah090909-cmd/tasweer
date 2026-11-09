package com.lumix.pro.video

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VideoRecorderController {

    private val _state = MutableStateFlow(VideoRecordingState())
    val state = _state.asStateFlow()

    companion object {
        val QUALITIES = listOf("4K UHD", "1080p FHD", "720p HD", "480p SD")
        val FRAME_RATES = listOf(24, 30, 60, 120)

        // Approximate average bitrates (bps) for time-remaining estimates
        fun bitrateFor(quality: String, fps: Int): Long = when (quality) {
            "4K UHD" -> if (fps >= 60) 80_000_000L else 50_000_000L
            "1080p FHD" -> if (fps >= 60) 24_000_000L else 16_000_000L
            "720p HD" -> 8_000_000L
            else -> 4_000_000L
        }
    }

    fun setQuality(q: String) = _state.update { it.copy(quality = q) }
    fun setFps(fps: Int) = _state.update { it.copy(fps = fps) }
    fun setStabilization(on: Boolean) = _state.update { it.copy(stabilization = on) }
    fun setAudioEnabled(on: Boolean) = _state.update { it.copy(audioEnabled = on) }
    fun setSlowMotion(on: Boolean) = _state.update { it.copy(slowMotion = on) }

    fun updateRecording(isRec: Boolean, isPaused: Boolean, durationMs: Long, bytes: Long = 0L) {
        _state.update {
            it.copy(
                isRecording = isRec,
                isPaused = isPaused,
                durationMs = durationMs,
                bytesRecorded = bytes
            )
        }
    }

    fun recalculateRemainingTime(freeBytes: Long) {
        val s = _state.value
        val bps = bitrateFor(s.quality, s.fps)
        val audioBps = if (s.audioEnabled) s.audioBitrateKbps * 1000L else 0L
        val totalBps = (bps + audioBps) / 8L
        val seconds = if (totalBps > 0) freeBytes / totalBps else 0L
        _state.update { it.copy(remainingMinutes = seconds / 60L) }
    }
}
