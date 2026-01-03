package ui.uiHelpers

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

// 🔑 Одна переменная для управления расстоянием между буквами
val letterSpacingValue = 1.3.sp

// Подключаем шрифты из ресурсов
val sfFontFamily = FontFamily(
    Font("fonts/SFUIDisplay-Light.ttf", weight = FontWeight.Normal),
    Font("fonts/SFUIDisplay-Light.ttf", weight = FontWeight.Bold)
)

// Полный набор Typography
val myTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        letterSpacing = letterSpacingValue
    ),
    displayMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        letterSpacing = letterSpacingValue
    ),
    displaySmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = letterSpacingValue
    ),
    headlineLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = letterSpacingValue
    ),
    headlineMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        letterSpacing = letterSpacingValue
    ),
    headlineSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = letterSpacingValue
    ),
    titleLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = letterSpacingValue
    ),
    titleMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = letterSpacingValue
    ),
    titleSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = letterSpacingValue
    ),
    bodyLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = letterSpacingValue
    ),
    bodyMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = letterSpacingValue
    ),
    bodySmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = letterSpacingValue
    ),
    labelLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = letterSpacingValue
    ),
    labelMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = letterSpacingValue
    ),
    labelSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = letterSpacingValue
    )
)
