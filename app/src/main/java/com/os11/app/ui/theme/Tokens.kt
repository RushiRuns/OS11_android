package com.os11.app.ui.theme

/**
 * OS11 Android - Design Tokens
 *
 * Port of desktop tokens.css (dark values). Same names where a name still makes sense.
 *
 * Rules:
 *  1. No hardcoded color, dp, sp, radius, elevation, or duration in any composable.
 *  2. Every value comes from here, read through OS11Theme.* inside a composable.
 *  3. If a token does not exist for something, add it here first.
 *  4. This is the ONLY file allowed to touch MaterialTheme (see OS11Theme below).
 *     Feature code never reads MaterialTheme.colorScheme or MaterialTheme.typography.
 *  5. Dark only. No dynamic color.
 *
 * Status: written without compiling. The Phase 1 session compiles it, adds Fonts.kt
 * (InterFamily) and previews. Values marked PARITY must be matched to the desktop
 * app's live shell values once those are final.
 */

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ============================================================
// COLORS (dark palette from tokens.css)
// ============================================================

/** Default accent from tokens.css dark mode. */
val OS11DefaultAccent = Color(0xFF4DA3FF)

@Immutable
data class OS11Colors(
    // Surfaces. surfaceSidebar is the Browse screen, bottom bar and sheets (there is no sidebar on phone).
    val surfaceBase: Color,
    val surfaceSidebar: Color,
    val surfaceRaised: Color,
    val surfaceOverlay: Color,
    val surfaceHover: Color, // pressed / ripple tint
    val surfaceSelected: Color,
    val surfaceActive: Color,
    val surfaceMain: Color,
    val surfaceMainGlow: Color, // PARITY: placeholder, match desktop shell gradient
    val surfaceMainVignette: Color,

    // Glass (translucent surface + border; no backdrop blur assumed, see ADR-A07)
    val surfaceGlass: Color,
    val borderGlass: Color,
    val surfaceProfileHover: Color,
    val borderAvatar: Color,
    val surfaceProfileMenuHover: Color,

    // Pomodoro pill
    val pomodoroPillBorder: Color,
    val pomodoroPillBg: Color,
    val pomodoroPillText: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textPlaceholder: Color,
    val textOnAccent: Color,
    val textLink: Color,
    val textDisabled: Color,

    // Accent
    val accent: Color,
    val accentHover: Color,
    val accentActive: Color,
    val accentMuted: Color,
    val accentBorder: Color,

    // Status
    val success: Color,
    val successMuted: Color,
    val warning: Color,
    val warningMuted: Color,
    val danger: Color,
    val dangerMuted: Color,
    val info: Color,
    val infoMuted: Color,
    val star: Color, // never accent, never danger

    // Priority (left border only)
    val priorityNone: Color,
    val priorityLow: Color,
    val priorityMedium: Color,
    val priorityHigh: Color,
    val priorityCritical: Color,

    // Borders
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val borderAccent: Color,
    val borderDanger: Color,
    val checkboxBorder: Color,

    // Focus ring (keyboard / switch access / TalkBack focus)
    val focusRing: Color,

    // Scrim behind sheets
    val scrim: Color,

    val tags: OS11TagPalette,
)

@Immutable
data class OS11TagPalette(
    val blue: Color,
    val cyan: Color,
    val teal: Color,
    val green: Color,
    val lime: Color,
    val yellow: Color,
    val orange: Color,
    val red: Color,
    val pink: Color,
    val purple: Color,
    val lavender: Color,
    val gray: Color,
) {
    /** Palette order matches desktop's 12-color picker. */
    val all: List<Color> = listOf(blue, cyan, teal, green, lime, yellow, orange, red, pink, purple, lavender, gray)
}

private val White = Color.White

/**
 * Build the dark palette. Pass a custom accent to recolor interactive elements;
 * hover, active, muted and border are derived the same way desktop derives them at runtime.
 */
fun os11DarkColors(accent: Color = OS11DefaultAccent): OS11Colors {
    val isDefault = accent == OS11DefaultAccent
    val hover = if (isDefault) Color(0xFF6FB5FF) else lerp(accent, White, 0.15f)
    val active = if (isDefault) Color(0xFF3090EE) else lerp(accent, Color.Black, 0.15f)
    val mutedTint = accent.copy(alpha = 0.15f)

    return OS11Colors(
        surfaceBase = Color(0xFF1C1C1C),
        surfaceSidebar = Color(0xFF2A2A2A),
        surfaceRaised = Color(0xFF252427),
        surfaceOverlay = Color(0xFF2A2930),
        surfaceHover = White.copy(alpha = 0.05f),
        surfaceSelected = mutedTint,
        surfaceActive = accent.copy(alpha = 0.10f),
        surfaceMain = Color(0xFF1C1C1C),
        surfaceMainGlow = accent.copy(alpha = 0.06f),
        surfaceMainVignette = Color.Transparent,

        surfaceGlass = Color(0xBD26252C), // rgba(38,37,44,0.74)
        borderGlass = White.copy(alpha = 0.10f),
        surfaceProfileHover = White.copy(alpha = 0.07f),
        borderAvatar = White.copy(alpha = 0.09f),
        surfaceProfileMenuHover = White.copy(alpha = 0.08f),

        pomodoroPillBorder = accent.copy(alpha = 0.32f),
        pomodoroPillBg = accent.copy(alpha = 0.13f),
        pomodoroPillText = if (isDefault) Color(0xFF8AC4FF) else lerp(accent, White, 0.35f),

        textPrimary = Color(0xFFF0EFF0),
        textSecondary = Color(0xFF8C8B90),
        textTertiary = Color(0xFF4E4D52),
        textPlaceholder = Color(0xFF3A3940),
        textOnAccent = White,
        textLink = accent,
        textDisabled = Color(0xFF3A3940),

        accent = accent,
        accentHover = hover,
        accentActive = active,
        accentMuted = mutedTint,
        accentBorder = accent.copy(alpha = 0.30f),

        success = Color(0xFF36CC6A),
        successMuted = Color(0xFF36CC6A).copy(alpha = 0.12f),
        warning = Color(0xFFFFCA28),
        warningMuted = Color(0xFFFFCA28).copy(alpha = 0.12f),
        danger = Color(0xFFFF5252),
        dangerMuted = Color(0xFFFF5252).copy(alpha = 0.12f),
        info = Color(0xFF3498DB),
        infoMuted = Color(0xFF3498DB).copy(alpha = 0.12f),
        star = Color(0xFFF4B942),

        priorityNone = Color.Transparent,
        priorityLow = Color(0xFF36CC6A),
        priorityMedium = Color(0xFFFFCA28),
        priorityHigh = Color(0xFFFF5252),
        priorityCritical = Color(0xFFFF2D55),

        borderSubtle = White.copy(alpha = 0.05f),
        borderDefault = White.copy(alpha = 0.08f),
        borderStrong = White.copy(alpha = 0.14f),
        borderAccent = accent,
        borderDanger = Color(0xFFFF5252),
        checkboxBorder = White.copy(alpha = 0.32f),

        focusRing = accent.copy(alpha = 0.30f),
        scrim = Color.Black.copy(alpha = 0.55f),

        tags = OS11TagPalette(
            blue = Color(0xFF4DA3FF),
            cyan = Color(0xFF26C6DA),
            teal = Color(0xFF26D0A3),
            green = Color(0xFF36CC6A),
            lime = Color(0xFF9CCC65),
            yellow = Color(0xFFFFCA28),
            orange = Color(0xFFFFA040),
            red = Color(0xFFFF5252),
            pink = Color(0xFFF06292),
            purple = Color(0xFFBA68C8),
            lavender = Color(0xFF9FA8DA),
            gray = Color(0xFFB0BEC5),
        ),
    )
}

// ============================================================
// SPACING (4dp base, names match --space-*)
// ============================================================

@Immutable
data class OS11Spacing(
    val px: Dp = 1.dp,
    val s0_5: Dp = 2.dp,
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 20.dp,
    val s6: Dp = 24.dp,
    val s7: Dp = 28.dp,
    val s8: Dp = 32.dp,
    val s10: Dp = 40.dp,
    val s12: Dp = 48.dp,
    val s16: Dp = 64.dp,
    val s20: Dp = 80.dp,
    val s24: Dp = 96.dp,
)

// ============================================================
// RADIUS
// ============================================================

@Immutable
data class OS11Radius(
    val xs: Dp = 3.dp, // tags, small chips
    val sm: Dp = 5.dp, // buttons
    val md: Dp = 8.dp, // inputs, popovers
    val lg: Dp = 12.dp, // sheets, panels
    val xl: Dp = 16.dp, // quick-add, onboarding cards
    val full: Dp = 9999.dp, // checkboxes, dots, pills
    val avatarTile: Dp = 10.dp,
    val profileRow: Dp = 12.dp,
    val profileMenuItem: Dp = 7.dp,
)

// ============================================================
// ELEVATION (shadow tokens become elevation on Android)
// ============================================================

@Immutable
data class OS11Elevation(
    val xs: Dp = 1.dp,
    val sm: Dp = 2.dp,
    val md: Dp = 4.dp,
    val lg: Dp = 8.dp,
    val xl: Dp = 16.dp,
)

// ============================================================
// LAYOUT
// ============================================================

enum class OS11Density { Compact, Comfortable, Cozy }

@Immutable
data class OS11Layout(
    /** Hard minimum for anything tappable, regardless of visual size. */
    val minTouchTarget: Dp = 48.dp,
    val screenGutter: Dp = 16.dp,

    // Desktop --toolbar-height and --quick-add-height carried over
    val topBarHeight: Dp = 52.dp,
    val quickAddHeight: Dp = 52.dp,
    // Android-only
    val bottomBarHeight: Dp = 64.dp,
    val fabSize: Dp = 56.dp,

    // Task row heights. ANDROID-ADAPTED: desktop is 34 / 44 / 56 px for a mouse.
    // Touch needs a 48dp hit area, so rows are 44 / 52 / 60dp.
    val taskHeightCompact: Dp = 44.dp,
    val taskHeightComfortable: Dp = 52.dp,
    val taskHeightCozy: Dp = 60.dp,

    val checkboxSize: Dp = 20.dp,
    val checkboxBorderWidth: Dp = 1.5.dp,
    val priorityBorderWidth: Dp = 2.5.dp,
    val tagDot: Dp = 18.dp,
    val projectDot: Dp = 10.dp,
    val pillDot: Dp = 6.dp,

    // Day Plan page (Android-specific)
    val dayPlanHourHeight: Dp = 80.dp, // 15 minutes = 20dp
    val dayPlanSnapMinutes: Int = 15,
    val dayPlanMinBlockMinutes: Int = 15,
    val dayPlanMaxBlockMinutes: Int = 480,
    val dayPlanGutterWidth: Dp = 56.dp,
) {
    fun taskHeight(density: OS11Density): Dp = when (density) {
        OS11Density.Compact -> taskHeightCompact
        OS11Density.Comfortable -> taskHeightComfortable
        OS11Density.Cozy -> taskHeightCozy
    }
}

// ============================================================
// TYPOGRAPHY (sizes match --text-*, in sp so system font scale applies)
// ============================================================

@Immutable
data class OS11TextSizes(
    val xs: Float = 11f, // section labels (ALL CAPS), badge counts
    val sm: Float = 12f, // metadata, dates, tag chips
    val base: Float = 13f, // body copy, browse rows
    val md: Float = 14f, // task titles
    val lg: Float = 15f, // panel titles, project names
    val xl: Float = 20f, // sub-view headers
    val x2l: Float = 24f, // view titles
    val x3l: Float = 30f, // dashboard stat numbers
)

/**
 * App font-size setting (sm / md / lg / xl) as a multiplier on top of the system font scale.
 * PARITY: desktop's px values for these four steps are not in the docs I had; confirm.
 */
enum class OS11FontStep(val multiplier: Float) { Sm(0.9f), Md(1.0f), Lg(1.1f), Xl(1.25f) }

@Immutable
data class OS11Type(
    val viewTitle: TextStyle,
    val subViewTitle: TextStyle,
    val panelTitle: TextStyle,
    val taskTitle: TextStyle,
    val body: TextStyle,
    val meta: TextStyle,
    val chip: TextStyle,
    val sectionLabel: TextStyle,
    val statNumber: TextStyle,
    val mono: TextStyle,
) {
    val sectionHeader: TextStyle get() = panelTitle
}

private fun style(
    family: FontFamily,
    size: Float,
    weight: FontWeight,
    leading: Float,
    tracking: Float = 0f,
    color: Color = Color.Unspecified,
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = (size * leading).sp,
    letterSpacing = tracking.em,
    color = color,
)

private const val LeadingTight = 1.2f
private const val LeadingSnug = 1.35f
private const val LeadingNormal = 1.5f
private const val TrackingTight = -0.015f
private const val TrackingWide = 0.03f
private const val TrackingWidest = 0.08f

fun os11Type(
    family: FontFamily,
    mono: FontFamily = FontFamily.Monospace,
    step: OS11FontStep = OS11FontStep.Md,
    sizes: OS11TextSizes = OS11TextSizes(),
): OS11Type {
    val m = step.multiplier
    return OS11Type(
        viewTitle = style(family, sizes.x2l * m, FontWeight.SemiBold, LeadingTight, TrackingTight),
        subViewTitle = style(family, sizes.xl * m, FontWeight.SemiBold, LeadingTight, TrackingTight),
        panelTitle = style(family, sizes.lg * m, FontWeight.SemiBold, LeadingSnug),
        taskTitle = style(family, sizes.md * m, FontWeight.Normal, LeadingSnug),
        body = style(family, sizes.base * m, FontWeight.Normal, LeadingNormal),
        meta = style(family, sizes.sm * m, FontWeight.Normal, LeadingSnug),
        chip = style(family, sizes.sm * m, FontWeight.Medium, LeadingTight, TrackingWide),
        sectionLabel = style(family, sizes.xs * m, FontWeight.Medium, LeadingTight, TrackingWidest),
        statNumber = style(family, sizes.x3l * m, FontWeight.Bold, LeadingTight, TrackingTight),
        mono = style(mono, sizes.sm * m, FontWeight.Normal, LeadingNormal),
    )
}

/** Section labels are rendered uppercase by the component, not by the token. */
val OS11SectionLabelOverflow = TextOverflow.Ellipsis

// ============================================================
// MOTION
// ============================================================

object OS11Motion {
    const val Fast = 120
    const val Default = 180
    const val Slow = 280

    const val Popover = 150
    const val MenuHover = 120
    const val PillPulse = 2000
    const val CriticalPulse = 1500

    val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val EaseIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Slight overshoot. Permitted only at the four spring sites in ARCHITECTURE.md. */
    val EaseSpring = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    // Spring parameters for the four permitted sites. Tuned in Phase 1.
    const val SpringDamping = 0.6f
    const val SpringStiffness = 400f
}

// ============================================================
// HAPTICS (event names only; wrapper in platform/haptics maps them)
// ============================================================

enum class OS11Haptic { Complete, DragStart, DragDrop, TimerEnd, Error }

// ============================================================
// MAIN PANE BACKGROUND (dark gradient with faint ambient glow at top)
// PARITY: values are placeholders until the desktop shell redesign is final.
// ============================================================

fun Modifier.os11MainPaneBackground(colors: OS11Colors): Modifier = this
    .drawBehind { drawRect(colors.surfaceMain) }
    .drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.surfaceMainGlow, Color.Transparent),
                center = Offset(size.width / 2f, 0f),
                radius = size.width * 0.9f,
            ),
        )
    }

// ============================================================
// COMPOSITION LOCALS AND THEME ENTRY
// ============================================================

val LocalOS11Colors = staticCompositionLocalOf { os11DarkColors() }
val LocalOS11Spacing = staticCompositionLocalOf { OS11Spacing() }
val LocalOS11Radius = staticCompositionLocalOf { OS11Radius() }
val LocalOS11Elevation = staticCompositionLocalOf { OS11Elevation() }
val LocalOS11Layout = staticCompositionLocalOf { OS11Layout() }
val LocalOS11Density = staticCompositionLocalOf { OS11Density.Comfortable }
val LocalOS11Type = staticCompositionLocalOf<OS11Type> {
    error("OS11Theme is not provided. Wrap content in OS11Theme { }.")
}

/** True when the system animator scale is 0 or the app setting reduce_motion is on. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Read tokens through this object. Never through MaterialTheme. */
object OS11Theme {
    val colors: OS11Colors
        @Composable @ReadOnlyComposable
        get() = LocalOS11Colors.current
    val spacing: OS11Spacing
        @Composable @ReadOnlyComposable
        get() = LocalOS11Spacing.current
    val radius: OS11Radius
        @Composable @ReadOnlyComposable
        get() = LocalOS11Radius.current
    val elevation: OS11Elevation
        @Composable @ReadOnlyComposable
        get() = LocalOS11Elevation.current
    val layout: OS11Layout
        @Composable @ReadOnlyComposable
        get() = LocalOS11Layout.current
    val density: OS11Density
        @Composable @ReadOnlyComposable
        get() = LocalOS11Density.current
    val type: OS11Type
        @Composable @ReadOnlyComposable
        get() = LocalOS11Type.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current
}

/**
 * Theme entry point. Also the one place MaterialTheme is configured, so Material3
 * primitives pick up token colors. Feature code never touches MaterialTheme.
 */
@Composable
fun OS11Theme(
    accent: Color = OS11DefaultAccent,
    fontFamily: FontFamily = InterFontFamily,
    fontStep: OS11FontStep = OS11FontStep.Md,
    density: OS11Density = OS11Density.Comfortable,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = remember(accent) { os11DarkColors(accent) }
    val type = remember(fontFamily, fontStep) { os11Type(fontFamily, step = fontStep) }

    val materialScheme = remember(colors) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.textOnAccent,
            background = colors.surfaceBase,
            onBackground = colors.textPrimary,
            surface = colors.surfaceBase,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surfaceOverlay,
            surfaceContainerHigh = colors.surfaceOverlay,
            outline = colors.borderDefault,
            outlineVariant = colors.borderSubtle,
            error = colors.danger,
            onError = colors.textOnAccent,
            scrim = colors.scrim,
        )
    }

    CompositionLocalProvider(
        LocalOS11Colors provides colors,
        LocalOS11Spacing provides OS11Spacing(),
        LocalOS11Radius provides OS11Radius(),
        LocalOS11Elevation provides OS11Elevation(),
        LocalOS11Layout provides OS11Layout(),
        LocalOS11Density provides density,
        LocalOS11Type provides type,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}
