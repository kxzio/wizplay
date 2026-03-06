package ui.uiHelpers

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// 🔑 Одна переменная для управления расстоянием между буквами
val letterSpacingValue = 1.3.sp

val factor: Float = 0.08f

fun relativeLetterSpacing(fontSize: TextUnit): TextUnit {
    return (fontSize.value * factor).sp
}

// Подключаем шрифты из ресурсов
val sfFontFamily = FontFamily(
    Font("fonts/SFUIDisplay-Light.ttf", weight = FontWeight.Normal),
    Font("fonts/SFUIDisplay-Light.ttf", weight = FontWeight.Bold)
)

val consoleFont = FontFamily(
    Font("fonts/TerminalVector.ttf", weight = FontWeight.Normal),
    Font("fonts/TerminalVector.ttf", weight = FontWeight.Bold)
)



// Полный набор Typography
val myTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        letterSpacing = relativeLetterSpacing(57.sp)
    ),
    displayMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        letterSpacing = relativeLetterSpacing(45.sp)
    ),
    displaySmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = relativeLetterSpacing(36.sp)
    ),
    headlineLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = relativeLetterSpacing(32.sp)
    ),
    headlineMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        letterSpacing = relativeLetterSpacing(28.sp)
    ),
    headlineSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = relativeLetterSpacing(24.sp)
    ),
    titleLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = relativeLetterSpacing(22.sp)
    ),
    titleMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = relativeLetterSpacing(16.sp)
    ),
    titleSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = relativeLetterSpacing(14.sp)
    ),
    bodyLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = relativeLetterSpacing(16.sp)
    ),
    bodyMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = relativeLetterSpacing(14.sp)
    ),
    bodySmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = relativeLetterSpacing(12.sp)
    ),
    labelLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = relativeLetterSpacing(14.sp)
    ),
    labelMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = relativeLetterSpacing(12.sp)
    ),
    labelSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = relativeLetterSpacing(11.sp)
    )
)
