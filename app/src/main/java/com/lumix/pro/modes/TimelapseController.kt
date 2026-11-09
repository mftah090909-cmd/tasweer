package com.lumix.pro.modes

import android.graphics.Bitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TimelapseState(
    val isRunning: Boolean = false,
    val shotsTaken: Int = 0,
    val intervalSeconds: Float = 2.0f,
    val maxShots: Int? = null,
    val elapsedSeconds: Long = 0L
)

class TimelapseController(
    private val scope: CoroutineScope,
    private val takeSnapshot: suspend () -> Bitmap?
) {
    private val _state = MutableStateFlow(TimelapseState())
    val state = _state.asStateFlow()

    private var job: Job? = null
    var onFrameCaptured: ((Bitmap, Int) -> Unit)? = null
    var onFinished: (() -> Unit)? = null

    fun setInterval(seconds: Float) {
        _state.update { it.copy(intervalSeconds = seconds.coerceIn(0.5f, 60f)) }
    }

    fun setMaxShots(max: Int?) = _state.update { it.copy(maxShots = max) }

    fun start() {
        if (_state.value.isRunning) return
        _state.update { it.copy(isRunning = true, shotsTaken = 0, elapsedSeconds = 0) }
        val startTime = System.currentTimeMillis()
        job = scope.launch(Dispatchers.Default) {
            var shots = 0
            while (isActive && _state.value.isRunning) {
                val loopStart = System.currentTimeMillis()
                val frame = takeSnapshot()
                if (frame != null) {
                    shots++
                    _state.update {
                        it.copy(
                            shotsTaken = shots,
                            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000L
                        )
                    }
                    withContext(Dispatchers.Main) { onFrameCaptured?.invoke(frame, shots) }
                }
                val limit = _state.value.maxShots
                if (limit != null && shots >= limit) break
                val intervalMs = (_state.value.intervalSeconds * 1000).toLong()
                val workMs = System.currentTimeMillis() - loopStart
                delay((intervalMs - workMs).coerceAtLeast(100L))
            }
            _state.update { it.copy(isRunning = false) }
            withContext(Dispatchers.Main) { onFinished?.invoke() }
        }
    }

    fun stop() {
        _state.update { it.copy(isRunning = false) }
        job?.cancel()
    }
}
