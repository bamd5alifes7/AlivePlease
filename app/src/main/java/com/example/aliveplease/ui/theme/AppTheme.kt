package com.example.aliveplease.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 統一的主題顏色定義
 * 深靛青 + 翡翠綠 + 琥珀金點綴，現代深色主題
 */
object AppColors {
    // === 背景層次 ===
    val Background       = Color(0xFF0D1B2A)   // 深海藍黑
    val SurfaceDark      = Color(0xFF112233)   // 深藍卡片底
    val SurfaceMid       = Color(0xFF1A3045)   // 中深藍
    val SurfaceLight     = Color(0xFF1E3A52)   // 較亮的藍

    // === 主色系 - 翡翠綠 ===
    val PrimaryGreen     = Color(0xFF00C896)   // 鮮翠綠
    val PrimaryGreenDim  = Color(0xFF00A87A)   // 深翠綠
    val PrimaryGlow      = Color(0x4000C896)   // 翠綠發光效果

    // === 強調色 - 琥珀金 ===
    val AccentAmber      = Color(0xFFFFB830)   // 琥珀金
    val AccentAmberDim   = Color(0xFFC88A00)   // 深琥珀
    val AccentAmberGlow  = Color(0x30FFB830)   // 琥珀發光

    // === 文字色 ===
    val TextPrimary      = Color(0xFFECF4FF)   // 主文字（近白帶藍）
    val TextSecondary    = Color(0xFF8BAAC5)   // 次文字（霧藍）
    val TextHint         = Color(0xFF4E728E)   // 提示文字

    // === 功能色 ===
    val Success          = Color(0xFF00C896)
    val Error            = Color(0xFFFF6B6B)
    val Warning          = Color(0xFFFFB830)

    // === 漸層定義 ===
    val GradientBackground = listOf(
        Color(0xFF0D1B2A),
        Color(0xFF112233),
        Color(0xFF0D1B2A)
    )

    val GradientButton = listOf(
        Color(0xFF00C896),
        Color(0xFF00A87A)
    )

    val GradientTopBar = listOf(
        Color(0xFF112233),
        Color(0xFF1A3045)
    )
}
