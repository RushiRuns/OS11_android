package com.os11.app.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * OS11 Typography Fonts.
 *
 * Inter font family definition with system sans-serif fallback.
 * Desktop uses Inter; on Android, FontFamily.SansSerif is wired with corresponding
 * font weights (SemiBold, Medium, Normal, Bold) to ensure consistent typographical metrics
 * without external network downloads or Google Play Services dependencies.
 */
val InterFontFamily: FontFamily = FontFamily.SansSerif
