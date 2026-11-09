package com.lumix.pro.video

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

class AudioLevelMeter(
    private val scope: CoroutineScope,
    private val onLevel: (Float) -> Unit
) {
    private var record: AudioRecord? = null
    private var job: Job? = null
    private val sampleRate = 44100
    private val bufferSize = max(
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ),
        2048
    )

    @SuppressLint("MissingPermission")
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Default) {
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (record?.state != AudioRecord.STATE_INITIALIZED) return@launch
                record?.startRecording()
                val buffer = ShortArray(1024)
                while (isActive) {
                    val read = record?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) sum += (buffer[i] * buffer[i]).toDouble()
                        val rms = sqrt(sum / read)
                        val db = (20.0 * log10((rms + 1.0) / 32767.0)).coerceIn(-60.0, 0.0)
                        val normalized = ((db + 60.0) / 60.0).toFloat().coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) { onLevel(normalized) }
                    }
                    delay(33)
                }
            } catch (t: Throwable) {
                // permission or device limitation – degrade silently
            } finally {
                stopRecording()
            }
        }
    }

    private fun stopRecording() {
        runCatching {
            record?.stop()
            record?.release()
        }
        record = null
    }

    fun stop() {
        job?.cancel()
        job = null
        stopRecording()
    }
}
