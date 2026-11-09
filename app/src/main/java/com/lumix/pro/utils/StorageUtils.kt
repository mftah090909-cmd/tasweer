package com.lumix.pro.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.lumix.pro.video.VideoRecorderController
import java.io.File

data class StorageReport(
    val freeBytes: Long,
    val totalBytes: Long,
    val freeMb: Long,
    val freePercent: Int,
    val estimatedPhotoShotsRemaining: Long,
    val estimatedVideoMinutesRemaining: Long,
    val freeFormatted: String,
    val totalFormatted: String
)

object StorageUtils {

    private const val AVG_JPEG_BYTES = 7_500_000L // ~7.5 MB high-res photo

    fun report(context: Context): StorageReport {
        val path: File = context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
            ?: context.filesDir
        val stat = runCatching { StatFs(path.absolutePath) }.getOrNull()
        val free = stat?.let { it.availableBlocksLong * it.blockSizeLong } ?: 0L
        val total = stat?.let { it.blockCountLong * it.blockSizeLong } ?: 0L
        val percent = if (total > 0) ((free.toDouble() / total.toDouble()) * 100).toInt() else 0

        val photoShots = if (free > 0) free / AVG_JPEG_BYTES else 0L
        val videoBitrateBps = VideoRecorderController.bitrateFor("1080p FHD", 30)
        val videoBytesPerSec = videoBitrateBps / 8L
        val videoMinutes = if (videoBytesPerSec > 0) (free / videoBytesPerSec) / 60L else 0L

        return StorageReport(
            freeBytes = free,
            totalBytes = total,
            freeMb = free / (1024 * 1024),
            freePercent = percent,
            estimatedPhotoShotsRemaining = photoShots,
            estimatedVideoMinutesRemaining = videoMinutes,
            freeFormatted = formatGb(free),
            totalFormatted = formatGb(total)
        )
    }

    private fun formatGb(bytes: Long): String = "%.1f GB".format(bytes / 1_000_000_000.0)
}
