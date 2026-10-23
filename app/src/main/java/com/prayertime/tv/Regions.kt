package com.prayertime.tv

class Region(val nameAr: String, val lat: Double, val lng: Double, val zone: String, val method: Method)

object Regions {
    val all = listOf(
        Region("المنامة - البحرين", 26.2285, 50.5860, "Asia/Bahrain", Methods.GULF),
        Region("مكة المكرمة - السعودية", 21.3891, 39.8579, "Asia/Riyadh", Methods.UMM_AL_QURA),
        Region("المدينة المنورة - السعودية", 24.4672, 39.6111, "Asia/Riyadh", Methods.UMM_AL_QURA),
        Region("الرياض - السعودية", 24.7136, 46.6753, "Asia/Riyadh", Methods.UMM_AL_QURA),
        Region("جدة - السعودية", 21.4858, 39.1925, "Asia/Riyadh", Methods.UMM_AL_QURA),
        Region("الدمام - السعودية", 26.4207, 50.0888, "Asia/Riyadh", Methods.UMM_AL_QURA),
        Region("الدوحة - قطر", 25.2854, 51.5310, "Asia/Qatar", Methods.QATAR),
        Region("الكويت - الكويت", 29.3759, 47.9774, "Asia/Kuwait", Methods.GULF),
        Region("دبي - الإمارات", 25.2048, 55.2708, "Asia/Dubai", Methods.UAE),
        Region("أبوظبي - الإمارات", 24.4539, 54.3773, "Asia/Dubai", Methods.UAE),
        Region("مسقط - عُمان", 23.5880, 58.3829, "Asia/Muscat", Methods.UAE),
        Region("صنعاء - اليمن", 15.3694, 44.1910, "Asia/Aden", Methods.UMM_AL_QURA),
        Region("بغداد - العراق", 33.3152, 44.3661, "Asia/Baghdad", Methods.MWL),
        Region("عمّان - الأردن", 31.9454, 35.9284, "Asia/Amman", Methods.MWL),
        Region("القدس - فلسطين", 31.7683, 35.2137, "Asia/Jerusalem", Methods.MWL),
        Region("دمشق - سوريا", 33.5138, 36.2765, "Asia/Damascus", Methods.MWL),
        Region("بيروت - لبنان", 33.8938, 35.5018, "Asia/Beirut", Methods.MWL),
        Region("القاهرة - مصر", 30.0444, 31.2357, "Africa/Cairo", Methods.EGYPT),
        Region("الخرطوم - السودان", 15.5007, 32.5599, "Africa/Khartoum", Methods.EGYPT),
        Region("طرابلس - ليبيا", 32.8872, 13.1913, "Africa/Tripoli", Methods.MWL),
        Region("تونس - تونس", 36.8065, 10.1815, "Africa/Tunis", Methods.MWL),
        Region("الجزائر - الجزائر", 36.7538, 3.0588, "Africa/Algiers", Methods.MWL),
        Region("الرباط - المغرب", 34.0209, -6.8416, "Africa/Casablanca", Methods.MOROCCO),
        Region("كانو - نيجيريا", 12.0022, 8.5919, "Africa/Lagos", Methods.MWL),
        Region("إسطنبول - تركيا", 41.0082, 28.9784, "Europe/Istanbul", Methods.TURKEY),
        Region("كراتشي - باكستان", 24.8607, 67.0011, "Asia/Karachi", Methods.KARACHI),
        Region("لاهور - باكستان", 31.5204, 74.3587, "Asia/Karachi", Methods.KARACHI),
        Region("دكا - بنغلاديش", 23.8103, 90.4125, "Asia/Dhaka", Methods.KARACHI),
        Region("جاكرتا - إندونيسيا", -6.2088, 106.8456, "Asia/Jakarta", Methods.MWL),
        Region("كوالالمبور - ماليزيا", 3.1390, 101.6869, "Asia/Kuala_Lumpur", Methods.SEA),
        Region("لندن - بريطانيا", 51.5072, -0.1276, "Europe/London", Methods.MWL),
        Region("باريس - فرنسا", 48.8566, 2.3522, "Europe/Paris", Methods.MWL),
        Region("نيويورك - أمريكا", 40.7128, -74.0060, "America/New_York", Methods.ISNA),
        Region("تورنتو - كندا", 43.6532, -79.3832, "America/Toronto", Methods.ISNA),
    )
}
