package com.lumix.pro.enhancer

enum class EnhancementLevel(
    val title: String,
    val titleAr: String,
    val description: String,
    val descriptionAr: String,
    val denoiseStrength: Float,
    val sharpeningStrength: Float,
    val dynamicRangeStrength: Float,
    val upscalingFactor: Float,
    val colorCorrectionStrength: Float,
    val skinSmoothingStrength: Float,
    val deblurStrength: Float,
    val lutLook: LutLook
) {
    NATURAL(
        title = "Natural",
        titleAr = "طبيعي",
        description = "Subtle noise reduction and color balance; preserves the scene exactly as it looked.",
        descriptionAr = "تقليل ناعم للتشويش وضبط توازن الألوان مع الحفاظ على المنظر كما هو.",
        denoiseStrength = 0.25f,
        sharpeningStrength = 0.15f,
        dynamicRangeStrength = 0.2f,
        upscalingFactor = 1.0f,
        colorCorrectionStrength = 0.3f,
        skinSmoothingStrength = 0.15f,
        deblurStrength = 0.1f,
        lutLook = LutLook.NATURAL
    ),
    PROFESSIONAL(
        title = "Professional",
        titleAr = "احترافي",
        description = "Balanced dynamic range, controlled sharpening, natural skin texture, accurate colors.",
        descriptionAr = "مدى ديناميكي متوازن، حدة محسوبة، ملمس بشرة طبيعي وألوان دقيقة.",
        denoiseStrength = 0.45f,
        sharpeningStrength = 0.35f,
        dynamicRangeStrength = 0.5f,
        upscalingFactor = 1.0f,
        colorCorrectionStrength = 0.5f,
        skinSmoothingStrength = 0.3f,
        deblurStrength = 0.3f,
        lutLook = LutLook.PRO_COLOR
    ),
    ULTRA_DETAIL(
        title = "Ultra Detail",
        titleAr = "فائق التفاصيل",
        description = "Super-resolution detail synthesis, micro-contrast, edge clarity and texture boost.",
        descriptionAr = "إعادة بناء التفاصيل فائقة الدقة، تباين دقيق، ووضوح حواف معزز.",
        denoiseStrength = 0.4f,
        sharpeningStrength = 0.7f,
        dynamicRangeStrength = 0.6f,
        upscalingFactor = 1.4f,
        colorCorrectionStrength = 0.4f,
        skinSmoothingStrength = 0.2f,
        deblurStrength = 0.5f,
        lutLook = LutLook.PRO_COLOR
    ),
    AI_MAXIMUM(
        title = "AI Maximum",
        titleAr = "أقصى ذكاء اصطناعي",
        description = "Full pipeline: super-resolution upscale, low-light HDR fusion, deblur and noise removal.",
        descriptionAr = "أقصى قوة: رفع الدقة، دمج HDR للإضاءة المنخفضة، إزالة التغبيش والتشويش.",
        denoiseStrength = 0.75f,
        sharpeningStrength = 0.6f,
        dynamicRangeStrength = 0.85f,
        upscalingFactor = 1.6f,
        colorCorrectionStrength = 0.75f,
        skinSmoothingStrength = 0.5f,
        deblurStrength = 0.7f,
        lutLook = LutLook.AI_VIVID
    ),
    CINEMATIC(
        title = "Cinematic",
        titleAr = "سينمائي",
        description = "Film-look color science, soft highlight roll-off, warm shadow tones and rich contrast.",
        descriptionAr = "علم ألوان سينمائي، تلاشي إضاءة ناعم، ظلال دافئة وتباين ثري.",
        denoiseStrength = 0.35f,
        sharpeningStrength = 0.3f,
        dynamicRangeStrength = 0.65f,
        upscalingFactor = 1.0f,
        colorCorrectionStrength = 0.6f,
        skinSmoothingStrength = 0.35f,
        deblurStrength = 0.25f,
        lutLook = LutLook.CINEMATIC
    );

    fun localizedTitle(isArabic: Boolean): String = if (isArabic) titleAr else title
    fun localizedDescription(isArabic: Boolean): String = if (isArabic) descriptionAr else description
}
