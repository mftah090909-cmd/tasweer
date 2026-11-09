package com.lumix.pro.gallery

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.text.DecimalFormat
import kotlin.math.roundToInt

data class ExifData(
    val cameraMakeModel: String?,
    val lensModel: String?,
    val focalLength: String?,
    val aperture: String?,
    val shutterSpeed: String?,
    val iso: String?,
    val exposureBias: String?,
    val whiteBalance: String?,
    val flashFired: Boolean?,
    val dateTime: String?,
    val dimensions: String?,
    val megapixels: String?,
    val fileSizeFormatted: String?,
    val colorSpace: String?,
    val latitude: Double?,
    val longitude: Double?
)

object ExifInspector {

    private val df = DecimalFormat("#.##")

    fun inspect(context: Context, uri: Uri, fileSize: Long = 0L): ExifData? {
        val exif = runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        }.getOrNull() ?: return null

        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
        val makeModel = listOfNotNull(make, model).joinToString(" ").ifBlank { null }
        val lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()

        val fl = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
        val focalStr = if (fl > 0) "${df.format(fl)} mm" else null

        val fn = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
        val apStr = if (fn > 0) "f/${df.format(fn)}" else null

        val et = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()
        val shutterStr = when {
            et == null || et <= 0.0 -> null
            et >= 1.0 -> "${df.format(et)}s"
            else -> "1/${(1.0 / et).roundToInt()}s"
        }

        val isoVal = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
        val isoStr = if (isoVal > 0) "ISO $isoVal" else null

        val ev = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, Double.NaN)
        val evStr = if (!ev.isNaN()) "${if (ev > 0) "+" else ""}${df.format(ev)} EV" else null

        val wb = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, ExifInterface.WHITE_BALANCE_AUTO.toInt())
        val wbStr = when (wb) {
            ExifInterface.WHITE_BALANCE_MANUAL.toInt() -> "Manual"
            else -> "Auto"
        }

        val flashAttr = exif.getAttributeInt(ExifInterface.TAG_FLASH, 0)
        val flashFired = (flashAttr and 0x1) == 1

        val dt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

        val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        val dimStr = if (w > 0 && h > 0) "$w × $h" else null
        val mpStr = if (w > 0 && h > 0) "${df.format((w.toLong() * h) / 1_000_000.0)} MP" else null

        val sizeStr = if (fileSize > 0) {
            if (fileSize >= 1_000_000) "${df.format(fileSize / 1_000_000.0)} MB" else "${fileSize / 1000} KB"
        } else null

        val cs = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, 1)
        val csStr = if (cs == 1) "sRGB" else "Uncalibrated"

        val latLong = exif.latLong
        val lat = latLong?.getOrNull(0)
        val lon = latLong?.getOrNull(1)

        return ExifData(
            cameraMakeModel = makeModel,
            lensModel = lens,
            focalLength = focalStr,
            aperture = apStr,
            shutterSpeed = shutterStr,
            iso = isoStr,
            exposureBias = evStr,
            whiteBalance = wbStr,
            flashFired = flashFired,
            dateTime = dt,
            dimensions = dimStr,
            megapixels = mpStr,
            fileSizeFormatted = sizeStr,
            colorSpace = csStr,
            latitude = lat,
            longitude = lon
        )
    }
}
