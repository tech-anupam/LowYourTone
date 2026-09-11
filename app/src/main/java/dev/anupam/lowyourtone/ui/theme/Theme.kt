package dev.anupam.lowyourtone.ui.theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = RoyalPrimary,
    onPrimary = RoyalOnPrimary,
    secondary = RoyalSecondary,
    tertiary = RoyalTertiary,
    background = RoyalBackground,
    onBackground = RoyalOnBackground,
    surface = RoyalSurface,
    onSurface = RoyalOnSurface,
    surfaceVariant = RoyalSurfaceVariant,
    outline = RoyalOutline,
    error = RoyalSecondary,
    onError = RoyalOnPrimary
)

@Composable
fun LowYourToneTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? ComponentActivity)?.enableEdgeToEdge()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
