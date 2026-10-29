package com.socialbreak.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = IndigoActive,
    onPrimary = TextPrimary,
    secondary = LavenderPeriwinkle,
    onSecondary = TextOnCream,
    tertiary = EmeraldSuccess,
    background = PitchBlack,
    onBackground = TextPrimary,
    surface = SlateCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = CrimsonDanger
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoActive,
    onPrimary = Color.White,
    secondary = LavenderPeriwinkle,
    onSecondary = TextOnCream,
    tertiary = EmeraldSuccess,
    background = Color(0xFFF2F2F2),
    onBackground = Color(0xFF111118),
    surface = Color.White,
    onSurface = Color(0xFF111118),
    surfaceVariant = Color(0xFFE5E6EE),
    onSurfaceVariant = Color(0xFF5A5D70),
    outline = Color(0xFFDADCE5),
    error = CrimsonDanger
)

object SocialTheme {
    val colors: SocialColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSocialColors.current
}

fun Modifier.grayscaleLayer(enabled: Boolean): Modifier {
    if (!enabled) return this
    return this.drawWithContent {
        val saturationMatrix = ColorMatrix().apply { setToSaturation(0f) }
        val paint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(saturationMatrix)
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            canvas.restore()
        }
    }
}

@Composable
fun SocialBreakTheme(
    themeMode: String = "dark",
    globalGrayscale: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode.lowercase()) {
        "light" -> false
        "system" -> systemDark
        else -> true
    }
    val socialPalette = if (useDark) DarkSocialPalette else LightSocialPalette
    val materialScheme = if (useDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgArgb = socialPalette.background.toArgb()
            window.statusBarColor = bgArgb
            window.navigationBarColor = bgArgb
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalSocialColors provides socialPalette) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .grayscaleLayer(globalGrayscale)
            ) {
                content()
            }
        }
    }
}
