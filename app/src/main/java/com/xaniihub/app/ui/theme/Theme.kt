package com.xaniihub.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemePalette(val title: String) {
    MONO("Mono"),
    CYAN("Cyber Cyan"),
    VIOLET("Violet"),
    EMERALD("Emerald"),
    SUNSET("Sunset")
}

object AppThemeController {
    private const val PREFS = "ringwalk_theme"
    private const val KEY_PALETTE = "palette"

    var palette by mutableStateOf(AppThemePalette.VIOLET)
        private set

    fun init(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_PALETTE, AppThemePalette.VIOLET.name) ?: AppThemePalette.VIOLET.name
        palette = runCatching { AppThemePalette.valueOf(name) }.getOrDefault(AppThemePalette.VIOLET)
    }

    fun setPalette(context: android.content.Context, value: AppThemePalette) {
        palette = value
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PALETTE, value.name)
            .commit()
        // Widgets mirror the palette from the same preferences – repaint them right away.
        com.xaniihub.app.widget.RingWalkWidgets.refreshAllAsync(context)
    }
}

/**
 * Each palette carries a primary + two companion accents so that rings, charts and
 * ambient glows can use a rich multi-stop gradient instead of a single flat colour.
 */
private fun darkPalette(palette: AppThemePalette) = when (palette) {
    AppThemePalette.MONO -> darkColorScheme(
        primary = White,
        onPrimary = Black,
        primaryContainer = Gray700,
        onPrimaryContainer = White,
        secondary = Color(0xFFBFC4CC),
        onSecondary = Black,
        tertiary = Color(0xFF8A9BB0),
        onTertiary = Black,
        background = Color(0xFF050506),
        onBackground = White,
        surface = Color(0xFF0E0E10),
        onSurface = White,
        surfaceVariant = Gray800,
        onSurfaceVariant = Gray300,
        outline = Gray500,
    )
    AppThemePalette.CYAN -> darkColorScheme(
        primary = Color(0xFF00D9FF),
        onPrimary = Black,
        primaryContainer = Color(0xFF083846),
        onPrimaryContainer = White,
        secondary = Color(0xFF2EFFC2),
        onSecondary = Black,
        tertiary = Color(0xFF7C6BFF),
        onTertiary = White,
        background = Color(0xFF03070A),
        onBackground = White,
        surface = Color(0xFF0A1218),
        onSurface = White,
        surfaceVariant = Color(0xFF14232B),
        onSurfaceVariant = Gray300,
        outline = Color(0xFF5A7A84),
    )
    AppThemePalette.VIOLET -> darkColorScheme(
        primary = Color(0xFFA78BFA),
        onPrimary = Black,
        primaryContainer = Color(0xFF33235E),
        onPrimaryContainer = White,
        secondary = Color(0xFFF472B6),
        onSecondary = Black,
        tertiary = Color(0xFF60A5FA),
        onTertiary = Black,
        background = Color(0xFF07050F),
        onBackground = White,
        surface = Color(0xFF110D1D),
        onSurface = White,
        surfaceVariant = Color(0xFF211A33),
        onSurfaceVariant = Gray300,
        outline = Color(0xFF7B6AA8),
    )
    AppThemePalette.EMERALD -> darkColorScheme(
        primary = Color(0xFF5EF05D),
        onPrimary = Black,
        primaryContainer = Color(0xFF1D4C28),
        onPrimaryContainer = White,
        secondary = Color(0xFF2DD4BF),
        onSecondary = Black,
        tertiary = Color(0xFFFDE047),
        onTertiary = Black,
        background = Color(0xFF040A05),
        onBackground = White,
        surface = Color(0xFF0C170E),
        onSurface = White,
        surfaceVariant = Color(0xFF17281A),
        onSurfaceVariant = Gray300,
        outline = Color(0xFF6B8F70),
    )
    AppThemePalette.SUNSET -> darkColorScheme(
        primary = Color(0xFFFF8A50),
        onPrimary = Black,
        primaryContainer = Color(0xFF5C2A1B),
        onPrimaryContainer = White,
        secondary = Color(0xFFFF4D8D),
        onSecondary = White,
        tertiary = Color(0xFFFFD166),
        onTertiary = Black,
        background = Color(0xFF0E0504),
        onBackground = White,
        surface = Color(0xFF1A0E0B),
        onSurface = White,
        surfaceVariant = Color(0xFF301D16),
        onSurfaceVariant = Gray300,
        outline = Color(0xFFA8755A),
    )
}

private val LightTextPrimary = Color(0xFF15171A)
private val LightTextSecondary = Color(0xFF5B6066)

private fun lightPalette(palette: AppThemePalette) = when (palette) {
    AppThemePalette.MONO -> lightColorScheme(
        primary = Color(0xFF1A1A1A),
        onPrimary = White,
        primaryContainer = Color(0xFFE2E2E5),
        onPrimaryContainer = Color(0xFF101010),
        background = Color(0xFFF4F4F6),
        onBackground = LightTextPrimary,
        surface = White,
        onSurface = LightTextPrimary,
        surfaceVariant = Color(0xFFECECEE),
        onSurfaceVariant = LightTextSecondary,
        outline = Color(0xFFC2C2C7),
    )
    AppThemePalette.CYAN -> lightColorScheme(
        primary = Color(0xFF0091AD),
        onPrimary = White,
        primaryContainer = Color(0xFFC3E9F1),
        onPrimaryContainer = Color(0xFF02323D),
        background = Color(0xFFEDF8FB),
        onBackground = LightTextPrimary,
        surface = White,
        onSurface = LightTextPrimary,
        surfaceVariant = Color(0xFFDBEEF3),
        onSurfaceVariant = LightTextSecondary,
        outline = Color(0xFF8FC3D0),
    )
    AppThemePalette.VIOLET -> lightColorScheme(
        primary = Color(0xFF6D45D9),
        onPrimary = White,
        primaryContainer = Color(0xFFD9CCF8),
        onPrimaryContainer = Color(0xFF26104F),
        background = Color(0xFFF4F0FE),
        onBackground = LightTextPrimary,
        surface = White,
        onSurface = LightTextPrimary,
        surfaceVariant = Color(0xFFE7DEFA),
        onSurfaceVariant = LightTextSecondary,
        outline = Color(0xFFB7A6E6),
    )
    AppThemePalette.EMERALD -> lightColorScheme(
        primary = Color(0xFF12A03A),
        onPrimary = White,
        primaryContainer = Color(0xFFC2EBC9),
        onPrimaryContainer = Color(0xFF0A3C18),
        background = Color(0xFFECFAEF),
        onBackground = LightTextPrimary,
        surface = White,
        onSurface = LightTextPrimary,
        surfaceVariant = Color(0xFFD9F2DD),
        onSurfaceVariant = LightTextSecondary,
        outline = Color(0xFF8FCF9C),
    )
    AppThemePalette.SUNSET -> lightColorScheme(
        primary = Color(0xFFE8612A),
        onPrimary = White,
        primaryContainer = Color(0xFFFBD3BF),
        onPrimaryContainer = Color(0xFF5A2410),
        background = Color(0xFFFFF4EE),
        onBackground = LightTextPrimary,
        surface = White,
        onSurface = LightTextPrimary,
        surfaceVariant = Color(0xFFFBE2D5),
        onSurfaceVariant = LightTextSecondary,
        outline = Color(0xFFEBA988),
    )
}

@Composable
fun XaniiHubTheme(
    // The UI currently uses a dark glass design; keep it consistent until a complete light variant exists.
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkPalette(AppThemeController.palette)
        else -> lightPalette(AppThemeController.palette)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp)
        ),
        typography = XaniiTypography,
        content = content
    )
}
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}