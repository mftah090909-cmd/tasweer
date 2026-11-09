package com.lumix.pro.editor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(UnstableApi::class)
class VideoEditorProcessor(private val context: Context) {

    private var transformer: Transformer? = null

    suspend fun export(
        inputUri: Uri,
        model: VideoEditorModel,
        onProgress: (Float) -> Unit,
        onDone: (Uri) -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.Main) {
        val outDir = context.cacheDir.resolve("lumix_editor_out").apply { mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tmpFile = File(outDir, "LUMIX_EDIT_$name.mp4")

        val clipBuilder = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(model.startMs)
        model.endMs?.let { clipBuilder.setEndPositionMs(it) }
        val mediaItem = MediaItem.Builder()
            .setUri(inputUri)
            .setClippingConfiguration(clipBuilder.build())
            .build()

        val effects = buildList {
            if (model.rotationDegrees % 360 != 0) {
                add(
                    ScaleAndRotateTransformation.Builder()
                        .setRotationDegrees(model.rotationDegrees.toFloat())
                        .build()
                )
            }
            if (model.speedMultiplier != 1.0f) {
                add(SpeedChangeEffect(model.speedMultiplier))
            }
            add(Contrast(1f + model.preset.contrastDelta))
            add(
                HslAdjustment.Builder()
                    .adjustSaturation(model.preset.saturationDelta)
                    .adjustHue(model.preset.warmShiftDegrees)
                    .build()
            )
            model.exportHeight?.let { add(Presentation.createForHeight(it)) }
        }

        val edited = EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(model.muteAudio)
            .setEffects(Effects(emptyList(), effects))
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                transformer = null
                onProgress(1f)
                val finalUri = copyToMediaStore(tmpFile)
                onDone(finalUri ?: Uri.fromFile(tmpFile))
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exception: ExportException) {
                transformer = null
                onError(exception)
            }
        }

        val t = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()
        transformer = t

        t.start(edited, tmpFile.absolutePath)

        val handler = Handler(Looper.getMainLooper())
        val holder = ProgressHolder()
        val poller = object : Runnable {
            override fun run() {
                val cur = transformer ?: return
                if (cur.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    onProgress(holder.progress / 100f)
                }
                handler.postDelayed(this, 120)
            }
        }
        handler.post(poller)
    }

    private fun copyToMediaStore(tmp: File): Uri? {
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cv = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "LUMIX_EDIT_$name.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LUMIX_PRO")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(tmp).use { it.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val finish = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            runCatching { context.contentResolver.update(uri, finish, null, null) }
        }
        tmp.delete()
        return uri
    }

    fun cancel() {
        transformer?.cancel()
        transformer = null
    }
}
