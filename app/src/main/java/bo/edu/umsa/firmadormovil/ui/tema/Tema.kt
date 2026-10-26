package bo.edu.umsa.firmadormovil.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * «Tinta y sello»: índigo para la identidad y las acciones; verde azulado (secondary) solo para lo
 * comprobado; ámbar (tertiary) para la CA de laboratorio. Esquema fijo, sin color dinámico, para
 * que los colores de estado no dependan del fondo de pantalla.
 */
private val Claro = lightColorScheme(
    primary = Color(0xFF33449A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE1FF),
    onPrimaryContainer = Color(0xFF0B1A5C),
    inversePrimary = Color(0xFFBAC3FF),
    secondary = Color(0xFF006A66),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8EEEA),
    onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFF7A5300),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE0B0),
    onTertiaryContainer = Color(0xFF281800),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFAFE),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBFAFE),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E2E9),
    onSurfaceVariant = Color(0xFF46464F),
    surfaceTint = Color(0xFF33449A),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2F0F7),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    surfaceDim = Color(0xFFDBD9E0),
    surfaceBright = Color(0xFFFBFAFE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3F9),
    surfaceContainer = Color(0xFFEEEDF4),
    surfaceContainerHigh = Color(0xFFE8E7EE),
    surfaceContainerHighest = Color(0xFFE3E2E9),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF1A2B7A),
    primaryContainer = Color(0xFF32428F),
    onPrimaryContainer = Color(0xFFDEE1FF),
    inversePrimary = Color(0xFF33449A),
    secondary = Color(0xFF7FD5CF),
    onSecondary = Color(0xFF003735),
    secondaryContainer = Color(0xFF00504C),
    onSecondaryContainer = Color(0xFFA0F2EB),
    tertiary = Color(0xFFFFBA4A),
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5C3F00),
    onTertiaryContainer = Color(0xFFFFDDAE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceTint = Color(0xFFBAC3FF),
    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF46464F),
    surfaceDim = Color(0xFF131318),
    surfaceBright = Color(0xFF39383F),
    surfaceContainerLowest = Color(0xFF0E0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF2A2930),
    surfaceContainerHighest = Color(0xFF34343B),
)

@Composable
fun TemaFirmadorMovil(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, content = contenido)
}
