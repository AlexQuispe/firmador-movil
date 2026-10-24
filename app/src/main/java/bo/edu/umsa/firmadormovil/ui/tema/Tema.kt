package bo.edu.umsa.firmadormovil.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun TemaFirmadorMovil(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    val contexto = LocalContext.current
    val colores = if (oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
    MaterialTheme(colorScheme = colores, content = contenido)
}
