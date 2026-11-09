package com.lumix.pro.assistant

import java.util.*
import kotlin.math.*

data class SunTimes(
    val sunrise: Date?,
    val goldenHourMorningEnd: Date?,
    val solarNoon: Date?,
    val goldenHourEveningStart: Date?,
    val sunset: Date?,
    val blueHourEnd: Date?,
    val statusNow: String,
    val statusNowAr: String
)

object GoldenHourCalculator {

    private const val ZENITH_OFFICIAL = 90.83333333333333
    private const val ZENITH_CIVIL = 96.0
    private const val ZENITH_GOLDEN = 96.0 - 6.0 // 6° above horizon

    fun compute(lat: Double, lon: Double, date: Calendar = Calendar.getInstance()): SunTimes {
        val sunrise = riseSet(lat, lon, date, ZENITH_OFFICIAL, isRise = true)
        val sunset = riseSet(lat, lon, date, ZENITH_OFFICIAL, isRise = false)
        val ghMorningEnd = riseSet(lat, lon, date, ZENITH_GOLDEN, isRise = true)
        val ghEveningStart = riseSet(lat, lon, date, ZENITH_GOLDEN, isRise = false)
        val blueHourEnd = riseSet(lat, lon, date, ZENITH_CIVIL, isRise = false)

        val noon = if (sunrise != null && sunset != null) Date((sunrise.time + sunset.time) / 2) else null

        val (st, stAr) = buildStatus(sunrise, ghMorningEnd, ghEveningStart, sunset, blueHourEnd)
        return SunTimes(
            sunrise = sunrise,
            goldenHourMorningEnd = ghMorningEnd,
            solarNoon = noon,
            goldenHourEveningStart = ghEveningStart,
            sunset = sunset,
            blueHourEnd = blueHourEnd,
            statusNow = st,
            statusNowAr = stAr
        )
    }

    private fun riseSet(lat: Double, lon: Double, cal: Calendar, zenith: Double, isRise: Boolean): Date? {
        val doy = cal.get(Calendar.DAY_OF_YEAR)
        val lngHour = lon / 15.0
        val t = if (isRise) doy + (6.0 - lngHour) / 24.0 else doy + (18.0 - lngHour) / 24.0

        val m = Math.toRadians((0.9856 * t) - 3.289)
        var l = Math.toRadians(normalize((0.9856 * t) - 3.289 + 1.916 * sin(m) + 0.020 * sin(2 * m) + 282.634))
        var ra = atan(0.91764 * tan(l))
        ra = Math.toRadians(normalize(Math.toDegrees(ra) + floor(normalize(Math.toDegrees(l)) / 90.0) * 90.0 - floor(normalize(Math.toDegrees(ra)) / 90.0) * 90.0))
        val raHours = Math.toDegrees(ra) / 15.0

        val sinDec = 0.39782 * sin(l)
        val cosDec = cos(asin(sinDec))

        val cosH = (cos(Math.toRadians(zenith)) - sinDec * sin(Math.toRadians(lat))) / (cosDec * cos(Math.toRadians(lat)))
        if (cosH > 1 || cosH < -1) return null

        val h = if (isRise) 360.0 - Math.toDegrees(acos(cosH)) else Math.toDegrees(acos(cosH))
        val hHours = h / 15.0
        val tLocal = hHours + raHours - 0.06571 * t - 6.622
        var ut = tLocal - lngHour
        ut = normalize24(ut)

        val zoneOffsetHours = cal.timeZone.getOffset(cal.timeInMillis) / 3600000.0
        val localHours = normalize24(ut + zoneOffsetHours)

        val res = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, localHours.toInt())
            set(Calendar.MINUTE, ((localHours - localHours.toInt()) * 60).toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return res.time
    }

    private fun normalize(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun normalize24(h: Double): Double {
        var d = h % 24.0
        if (d < 0) d += 24.0
        return d
    }

    private fun buildStatus(
        rise: Date?, ghMEnd: Date?, ghEStart: Date?, set: Date?, blueEnd: Date?
    ): Pair<String, String> {
        val now = System.currentTimeMillis()
        fun diffMin(d: Date?): Long = if (d == null) Long.MAX_VALUE else (d.time - now) / 60000L
        return when {
            rise == null || set == null -> "Polar region – times unavailable" to "منطقة قطبية – الأوقات غير متاحة"
            now < rise.time -> "Golden hour in ${diffMin(rise)} min at sunrise" to "الساعة الذهبية بعد ${diffMin(rise)} د عند الشروق"
            ghMEnd != null && now < ghMEnd.time -> "Golden hour (morning) now · ${diffMin(ghMEnd)} min left" to "الساعة الذهبية (صباحًا) الآن · متبقي ${diffMin(ghMEnd)} د"
            ghEStart != null && now < ghEStart.time -> "Next golden hour at sunset in ${diffMin(ghEStart)} min" to "الساعة الذهبية القادمة عند الغروب بعد ${diffMin(ghEStart)} د"
            now < set.time -> "Golden hour (sunset) now · ${diffMin(set)} min left" to "الساعة الذهبية (غروب) الآن · متبقي ${diffMin(set)} د"
            blueEnd != null && now < blueEnd.time -> "Blue hour now · ${diffMin(blueEnd)} min left" to "الساعة الزرقاء الآن · متبقي ${diffMin(blueEnd)} د"
            else -> "Night · Great time for astrophotography" to "ليل · وقت رائع للتصوير الفلكي"
        }
    }
}
