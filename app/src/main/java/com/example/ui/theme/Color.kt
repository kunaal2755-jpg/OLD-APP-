package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Exact Dark Navy Blue from User Screenshots
val Navy900 = Color(0xFF0B192C) // Deepest TopBar Navy
val Navy800 = Color(0xFF112338) // Hero Banner Navy
val Navy700 = Color(0xFF1E3A5F)
val Navy600 = Color(0xFF2E4C6D)

// Gold / Yellow Accent
val AmberGold = Color(0xFFF59E0B) // Accent & Monogram
val GoldBright = Color(0xFFEAB308) // New Sale Order button
val GoldLight = Color(0xFFFEF3C7)
val AmberLight = Color(0xFFFEF3C7)
val BlueLight = Color(0xFF0077B6)
val BlueCyan = Color(0xFF0096C7)

// Status & Badge Colors
val GreenActive = Color(0xFF10B981) // Active dot
val StatusSuccess = Color(0xFF16A34A)
val StatusSuccessBg = Color(0xFFDCFCE7)
val StatusPending = Color(0xFFEA580C)
val StatusPendingBg = Color(0xFFFFEDD5)
val StatusError = Color(0xFFDC2626)
val StatusErrorBg = Color(0xFFFEE2E2)

// Category / Action Colors
val BlueTile = Color(0xFF0284C7)
val PurpleCustomer = Color(0xFF8B5CF6)
val TealStock = Color(0xFF0D9488)
val OrangeOrders = Color(0xFFF97316)
val BlueReports = Color(0xFF3B82F6)

// Neutral Colors
val BlueSoft = Color(0xFFE0F2FE)
val BlueAccent = Color(0xFF0284C7)
val Neutral900 = Color(0xFF0F172A)
val Neutral800 = Color(0xFF1E293B)
val Neutral700 = Color(0xFF334155)
val Neutral500 = Color(0xFF64748B)
val Neutral400 = Color(0xFF94A3B8)
val Neutral200 = Color(0xFFE2E8F0)
val Neutral100 = Color(0xFFF1F5F9)
val Neutral50 = Color(0xFFF8FAFC)
val PureWhite = Color(0xFFFFFFFF)

data class AppThemeOption(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val primary: Color,
    val primaryContainer: Color,
    val accent: Color,
    val accentText: Color
)

val AppThemeOptions = listOf(
    AppThemeOption(
        id = "NAVY_GOLD",
        nameEn = "Royal Navy & Gold",
        nameHi = "रॉयल नेवी और गोल्ड",
        primary = Color(0xFF0B192C),
        primaryContainer = Color(0xFF112338),
        accent = Color(0xFFEAB308),
        accentText = Color(0xFF0B192C)
    ),
    AppThemeOption(
        id = "EMERALD_GOLD",
        nameEn = "Forest Emerald & Gold",
        nameHi = "एमराल्ड ग्रीन और गोल्ड",
        primary = Color(0xFF064E3B),
        primaryContainer = Color(0xFF065F46),
        accent = Color(0xFFF59E0B),
        accentText = Color(0xFF064E3B)
    ),
    AppThemeOption(
        id = "MAROON_AMBER",
        nameEn = "Royal Maroon & Amber",
        nameHi = "रॉयल महरून और अम्बर",
        primary = Color(0xFF4A0E17),
        primaryContainer = Color(0xFF5B111D),
        accent = Color(0xFFF59E0B),
        accentText = Color(0xFF4A0E17)
    ),
    AppThemeOption(
        id = "SLATE_CYAN",
        nameEn = "Modern Slate & Cyan",
        nameHi = "मॉडर्न स्लेट और सायन",
        primary = Color(0xFF0F172A),
        primaryContainer = Color(0xFF1E293B),
        accent = Color(0xFF06B6D4),
        accentText = Color(0xFF0F172A)
    ),
    AppThemeOption(
        id = "CHARCOAL_GOLD",
        nameEn = "Charcoal Onyx & Yellow",
        nameHi = "चारकोल ब्लैक और यलो",
        primary = Color(0xFF18181B),
        primaryContainer = Color(0xFF27272A),
        accent = Color(0xFFFACC15),
        accentText = Color(0xFF18181B)
    )
)

fun getAppThemeOption(id: String): AppThemeOption {
    return AppThemeOptions.find { it.id.equals(id, ignoreCase = true) } ?: AppThemeOptions[0]
}
