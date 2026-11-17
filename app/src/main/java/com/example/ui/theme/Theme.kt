package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = IndigoPrimary,
  onPrimary = Color.White,
  primaryContainer = IndigoContainer,
  onPrimaryContainer = OnIndigoContainer,
  secondary = EmeraldSuccess,
  onSecondary = Color.White,
  secondaryContainer = EmeraldContainer,
  onSecondaryContainer = OnEmeraldContainer,
  tertiary = AmberWarning,
  onTertiary = Color.White,
  tertiaryContainer = AmberContainer,
  onTertiaryContainer = OnAmberContainer,
  error = CrimsonError,
  onError = Color.White,
  errorContainer = CrimsonContainer,
  onErrorContainer = OnCrimsonContainer,
  background = IvoryBackground,
  onBackground = InkPrimary,
  surface = IvorySurface,
  onSurface = InkPrimary,
  surfaceVariant = IvorySurfaceVariant,
  onSurfaceVariant = InkSecondary,
  outline = SlateOutline,
  outlineVariant = SlateOutline,
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkIndigoPrimary,
  onPrimary = Color(0xFF0A1B30),
  primaryContainer = DarkIndigoContainer,
  onPrimaryContainer = Color(0xFFD9E4F2),
  secondary = Color(0xFF5EEAD4),
  onSecondary = Color(0xFF052E25),
  secondaryContainer = Color(0xFF134E41),
  onSecondaryContainer = Color(0xFFD4F2E7),
  tertiary = Color(0xFFFBBF24),
  onTertiary = Color(0xFF3F2400),
  tertiaryContainer = Color(0xFF5C3A06),
  onTertiaryContainer = Color(0xFFFEF0D1),
  error = Color(0xFFFCA5A5),
  onError = Color(0xFF450A0A),
  errorContainer = Color(0xFF7F1D1D),
  onErrorContainer = Color(0xFFFEE2E2),
  background = DarkBackground,
  onBackground = DarkInkPrimary,
  surface = DarkSurface,
  onSurface = DarkInkPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkInkSecondary,
  outline = DarkOutline,
  outlineVariant = DarkOutline,
)

val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun WaliKelasTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
