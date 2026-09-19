package ir.amir.triedgame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Modern dark "fintech" palette: deep navy background, neon green/red for
// bullish/bearish, electric blue accent for primary actions.
val TsBackground = Color(0xFF0B0E14)
val TsSurface = Color(0xFF151A24)
val TsSurfaceElevated = Color(0xFF1C2230)
val TsGreen = Color(0xFF00E6A0)
val TsRed = Color(0xFFFF4D5E)
val TsYellow = Color(0xFFFFC542)
val TsAccent = Color(0xFF4C8DFF)
val TsAccentSoft = Color(0xFF335C99)
val TsTextPrimary = Color(0xFFF2F4F8)
val TsTextSecondary = Color(0xFF8B93A1)
val TsGold = Color(0xFFFFD166)

val TsBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0B0E14), Color(0xFF10151F))
)

private val TraderShowColorScheme = darkColorScheme(
    primary = TsAccent,
    secondary = TsGreen,
    tertiary = TsGold,
    background = TsBackground,
    surface = TsSurface,
    surfaceVariant = TsSurfaceElevated,
    onPrimary = Color.White,
    onBackground = TsTextPrimary,
    onSurface = TsTextPrimary,
    onSurfaceVariant = TsTextSecondary,
    error = TsRed
)

private val TraderShowTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.2.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, color = TsTextSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp)
)

/** Returns green/yellow/red based on a 0..100 stat value, for health/hunger/energy bars. */
fun statColor(value: Int): Color = when {
    value >= 66 -> TsGreen
    value >= 33 -> TsYellow
    else -> TsRed
}

@Composable
fun TraderShowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TraderShowColorScheme,
        typography = TraderShowTypography,
        content = content
    )
}
