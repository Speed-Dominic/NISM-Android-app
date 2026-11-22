package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Bundled local Google Fonts (installed via font-util into res/font/)
val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val DmSansFontFamily = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal)
)

// Semantic brand & status tokens aligned with NISM Exam Prep Studio
object NismColors {
    val CobaltLight = Color(0xFF1D4ED8)
    val CobaltDark = Color(0xFF3B82F6)
    val CobaltDeep = Color(0xFF1E3A8A)
    val CobaltSoftLight = Color(0xFFEFF6FF)
    val CobaltSoftDark = Color(0xFF172554)

    val Emerald = Color(0xFF059669)
    val EmeraldLight = Color(0xFF34D399)
    val EmeraldBgLight = Color(0xFFECFDF5)
    val EmeraldBgDark = Color(0xFF052E16)

    val Rose = Color(0xFFDC2626)
    val RoseLight = Color(0xFFF87171)
    val RoseBgLight = Color(0xFFFEF2F2)
    val RoseBgDark = Color(0xFF450A0A)

    val Amber = Color(0xFFD97706)
    val AmberLight = Color(0xFFFBBF24)
    val AmberBgLight = Color(0xFFFFFBEB)
    val AmberBgDark = Color(0xFF451A03)

    val Indigo = Color(0xFF4F46E5)

    val SlateBg = Color(0xFFF6F8FC)
    val SlateSurfaceVariant = Color(0xFFEEF2F7)
    val SlateBorder = Color(0xFFE2E8F0)
    val SlateText = Color(0xFF0F172A)
    val SlateMuted = Color(0xFF64748B)

    val OledBg = Color(0xFF05070B)
    val OledCard = Color(0xFF0E1117)
    val OledSurfaceVariant = Color(0xFF171C26)
    val OledBorder = Color(0xFF232A38)
    val OledText = Color(0xFFF5F7FA)
    val OledMuted = Color(0xFF94A3B8)
}

private val LightColorScheme = lightColorScheme(
    primary = NismColors.CobaltLight,
    onPrimary = Color.White,
    primaryContainer = NismColors.CobaltSoftLight,
    onPrimaryContainer = NismColors.CobaltDeep,
    secondary = NismColors.Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = NismColors.Emerald,
    onTertiary = Color.White,
    tertiaryContainer = NismColors.EmeraldBgLight,
    onTertiaryContainer = Color(0xFF064E3B),
    background = NismColors.SlateBg,
    onBackground = NismColors.SlateText,
    surface = Color.White,
    onSurface = NismColors.SlateText,
    surfaceVariant = NismColors.SlateSurfaceVariant,
    onSurfaceVariant = NismColors.SlateMuted,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = NismColors.SlateBorder,
    outlineVariant = Color(0xFFCBD5E1),
    error = NismColors.Rose,
    onError = Color.White,
    errorContainer = NismColors.RoseBgLight,
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColorScheme = darkColorScheme(
    primary = NismColors.CobaltDark,
    onPrimary = Color.White,
    primaryContainer = NismColors.CobaltSoftDark,
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF1E1B4B),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = NismColors.EmeraldLight,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = NismColors.EmeraldBgDark,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = NismColors.OledBg,
    onBackground = NismColors.OledText,
    surface = NismColors.OledCard,
    onSurface = NismColors.OledText,
    surfaceVariant = NismColors.OledSurfaceVariant,
    onSurfaceVariant = NismColors.OledMuted,
    surfaceContainerLow = Color(0xFF11151E),
    surfaceContainer = Color(0xFF161B26),
    surfaceContainerHigh = Color(0xFF1E2533),
    outline = NismColors.OledBorder,
    outlineVariant = Color(0xFF334155),
    error = NismColors.RoseLight,
    onError = Color(0xFF450A0A),
    errorContainer = NismColors.RoseBgDark,
    onErrorContainer = Color(0xFFFECACA)
)

val TabularMonoStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontFeatureSettings = "tnum"
)

val NismTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontFeatureSettings = "tnum"
    )
)

val NismShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun NismExamPrepTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = NismTypography,
        shapes = NismShapes,
        content = content
    )
}
