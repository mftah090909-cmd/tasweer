package com.lumix.pro.videoenhancer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class VideoEnhancePreset(
    val title: String,
    val titleAr: String,
    val contrastDelta: Float,
    val saturationDelta: Float,
    val brightnessLift: Float,
    val warmShiftDegrees: Float
) {
    BALANCED("Balanced", "متوازن", 0.08f, 0.05f, 0.04f, 2f),
    LOW_LIGHT("Low-Light Boost", "تعزيز الإضاءة المنخفضة", 0.15f, 0.10f, 0.20f, 4f),
    CINEMATIC("Cinematic", "سينمائي", 0.20f, 0.02f, -0.02f, 10f),
    VIVID("Vivid Pop", "ألوان حية", 0.12f, 0.30f, 0.02f, 0f),
    SHADOW_RECOVERY("Shadow Recovery", "استعادة الظلال", 0.10f, 0.08f, 0.14f, 6f);

    fun localized(isArabic: Boolean): String = if (isArabic) titleAr else title
}

data class VideoEnhanceOptions(
    val preset: VideoEnhancePreset = VideoEnhancePreset.BALANCED,
    val targetHeight: Int? = null,
    val exportQualityLabel: String = "1080p FHD"
)

@OptIn(UnstableApi::class)
class VideoPostProcessor(private val context: Context) {

    private var currentTransformer: Transformer? = null

    suspend fun process(
        inputUri: Uri,
        options: VideoEnhanceOptions = VideoEnhanceOptions(),
        onProgress: (Float) -> Unit,
        onDone: (Uri) -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.Main) {
        val outDir = context.cacheDir.resolve("lumix_video_enhancer").apply { mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(outDir, "LUMIX_VIDEO_$name.mp4")

        val p = options.preset
        val effects = buildList {
            add(Contrast(1f + p.contrastDelta))
            val hslBuilder = HslAdjustment.Builder()
                .adjustSaturation(p.saturationDelta)
            if (p.warmShiftDegrees != 0f) hslBuilder.adjustHue(p.warmShiftDegrees)
            add(hslBuilder.build())
            options.targetHeight?.let { h ->
                add(Presentation.createForHeight(h))
            }
        }

        val edited = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
            .setEffects(Effects(emptyList(), effects))
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                currentTransformer = null
                onProgress(1f)
                onDone(Uri.fromFile(outFile))
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exception: ExportException) {
                currentTransformer = null
                onError(exception)
            }
        }

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()
        currentTransformer = transformer

        transformer.start(edited, outFile.absolutePath)

        val handler = Handler(Looper.getMainLooper())
        val holder = ProgressHolder()
        val poller = object : Runnable {
            override fun run() {
                val t = currentTransformer ?: return
                if (t.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    onProgress(holder.progress / 100f)
                }
                handler.postDelayed(this, 100)
            }
        }
        handler.post(poller)
    }

    fun cancel() {
        currentTransformer?.cancel()
        currentTransformer = null
    }
}
