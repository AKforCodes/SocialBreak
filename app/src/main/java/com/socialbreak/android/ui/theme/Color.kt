package com.socialbreak.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Pitch-Black & Indigo-Slate Surfaces (Dark Mode Default)
val PitchBlack = Color(0xFF000000)
val DeepObsidian = Color(0xFF0A0A0F)
val SlateCardSurface = Color(0xFF14141C)
val SlateCardElevated = Color(0xFF1C1D29)
val SlateDialTrack = Color(0xFF282A3A)
val SlateBorder = Color(0xFF262837)

// Primary Brand & Sleep / Focus Accents
val LavenderPeriwinkle = Color(0xFFB4A5F8)
val WarmCream = Color(0xFFF3EFE6)
val IndigoActive = Color(0xFF6366F1)
val IndigoMuted = Color(0xFF4F46E5)
val SkyBlueSelected = Color(0xFF7AA2F7)

// Semantic & Alert Accents
val AmberGold = Color(0xFFF59E0B)
val PeachOrange = Color(0xFFEDA158)
val EmeraldSuccess = Color(0xFF22C55E)
val CrimsonDanger = Color(0xFFEF4444)

// Typography Hierarchy
val TextPrimary = Color(0xFFF4F4F6)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
val TextOnCream = Color(0xFF111118)

// Legacy Compatibility Aliases
val ObsidianBlack = PitchBlack
val SurfaceSlate = SlateCardSurface
val SurfaceElevated = SlateCardElevated
val SurfaceGlassBorder = SlateBorder
val ElectricCrimson = CrimsonDanger
val AmberWarning = AmberGold
val NeonCyan = IndigoActive
val EmeraldSafe = EmeraldSuccess
val TextHighlight = TextPrimary

@Immutable
data class SocialColorPalette(
    val isDark: Boolean,
    val background: Color,
    val cardBackground: Color,
    val cardElevated: Color,
    val segmentedTrack: Color,
    val segmentedThumb: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val navBarBackground: Color,
    val navBarSelectedText: Color,
    val navBarUnselectedText: Color,
    val switchTrackOff: Color
)

val DarkSocialPalette = SocialColorPalette(
    isDark = true,
    background = Color(0xFF000000),
    cardBackground = Color(0xFF14141C),
    cardElevated = Color(0xFF1E1F2B),
    segmentedTrack = Color(0xFF252634),
    segmentedThumb = Color(0xFF151620),
    border = Color(0xFF262837),
    textPrimary = Color(0xFFF4F4F6),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF6E7185),
    navBarBackground = Color(0xFF17171F),
    navBarSelectedText = Color(0xFFFFFFFF),
    navBarUnselectedText = Color(0xFF7B7D92),
    switchTrackOff = Color(0xFF323446)
)

val LightSocialPalette = SocialColorPalette(
    isDark = false,
    background = Color(0xFFF2F2F2),
    cardBackground = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF4F4F8),
    segmentedTrack = Color(0xFFE0E0E0),
    segmentedThumb = Color(0xFFFFFFFF),
    border = Color(0xFFDADCE5),
    textPrimary = Color(0xFF111118),
    textSecondary = Color(0xFF5A5D70),
    textMuted = Color(0xFF787B8F),
    navBarBackground = Color(0xFFFFFFFF),
    navBarSelectedText = Color(0xFF111118),
    navBarUnselectedText = Color(0xFF8A8D9F),
    switchTrackOff = Color(0xFFD8DAE3)
)

val LocalSocialColors = staticCompositionLocalOf { DarkSocialPalette }
