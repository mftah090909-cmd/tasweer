package com.lumix.pro.modes

enum class CameraMode(
    val title: String,
    val titleAr: String,
    val isPhotoMode: Boolean = true,
    val isVideoMode: Boolean = false,
    val requiresExtension: String? = null
) {
    AUTO("AUTO", "تلقائي"),
    PRO("PRO", "احترافي"),
    PORTRAIT("PORTRAIT", "بورتريه", requiresExtension = "bokeh"),
    NIGHT("NIGHT", "ليلي", requiresExtension = "night"),
    HDR("HDR", "HDR", requiresExtension = "hdr"),
    MACRO("MACRO", "ماكرو"),
    LANDSCAPE("LANDSCAPE", "طبيعة"),
    STREET("STREET", "شوارع"),
    SPORTS("SPORTS", "رياضة"),
    FOOD("FOOD", "أطعمة"),
    ARCHITECTURE("ARCHITECTURE", "معماري"),
    LONG_EXPOSURE("LONG EXP", "تعريض طويل"),
    PANORAMA("PANORAMA", "بانوراما"),
    TIMELAPSE("TIME-LAPSE", "فاصل زمني"),
    DOCUMENT("DOC SCAN", "مستند"),
    HIGH_RES("HIGH-RES", "دقة فائقة"),
    RAW("RAW", "خام"),
    VIDEO("VIDEO", "فيديو", isPhotoMode = false, isVideoMode = true);

    fun localizedTitle(isArabic: Boolean): String = if (isArabic) titleAr else title

    companion object {
        val PRIMARY_MODES = listOf(AUTO, PRO, PORTRAIT, NIGHT, HDR, VIDEO)
        val MORE_MODES = listOf(
            MACRO, LANDSCAPE, STREET, SPORTS, FOOD, ARCHITECTURE,
            LONG_EXPOSURE, PANORAMA, TIMELAPSE, DOCUMENT, HIGH_RES, RAW
        )
    }
}
