package bo.edu.umsa.firmadormovil.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Iconos de trazo (24×24, trazo 2) dibujados para la guía «Tinta y sello»; `Icon` les aplica el color. */
object Iconos {
    val Escudo = icono("escudo", "M12 3l7 3v5c0 4.5-3 8.3-7 10-4-1.7-7-5.5-7-10V6l7-3z", "M9 12l2 2 4-4")
    val Matraz = icono("matraz", "M9 3h6", "M10 3v6l-5 9a2 2 0 0 0 1.7 3h10.6a2 2 0 0 0 1.7-3l-5-9V3", "M7.5 14h9")
    val Persona = icono("persona", "M8 8a4 4 0 1 0 8 0a4 4 0 1 0 -8 0", "M4 21a8 8 0 0 1 16 0")
    val Candado = icono(
        "candado",
        "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z",
        "M8 11V7a4 4 0 0 1 8 0v4",
    )
    val Documento = icono("documento", HOJA, PLIEGUE)
    val DocumentoNuevo = icono("documento_nuevo", HOJA, PLIEGUE, "M12 12v6", "M9 15h6")
    val DocumentoFirmado = icono("documento_firmado", HOJA, PLIEGUE, "M9 15l2 2 4-4")
    val Huella = icono(
        "huella",
        "M12 11c0 3-1 6-3 8",
        "M7.5 7.5A6 6 0 0 1 18 11c0 2-.3 4-1 6",
        "M5 10a7 7 0 0 1 .6-2.6",
        "M15 12c0 3-.6 5.4-1.6 7.4",
        "M9 11a3 3 0 0 1 6 0",
    )
    val Atras = icono("atras", "M19 12H5", "M12 19l-7-7 7-7")
    val Cerrar = icono("cerrar", "M18 6L6 18", "M6 6l12 12")
    val Copiar = icono(
        "copiar",
        "M11 9h7a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z",
        "M5 15V5a2 2 0 0 1 2-2h8",
    )
    val Enlace = icono(
        "enlace",
        "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1",
        "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1",
    )
    val Hecho = icono("hecho", "M5 12l5 5 9-10")
    val Info = icono("info", CIRCULO, "M12 11v5", "M12 8h.01")
    val Alerta = icono("alerta", CIRCULO, "M12 7.5v5.5", "M12 16.5h.01")
    val Descargar = icono("descargar", "M12 4v11", "M7 10l5 5 5-5", "M5 20h14")
    val Compartir = icono(
        "compartir",
        "M15.5 5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0",
        "M3.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0",
        "M15.5 19a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0",
        "M8.2 10.8l7.6-4.4",
        "M8.2 13.2l7.6 4.4",
    )
    val Siguiente = icono("siguiente", "M5 12h14", "M12 5l7 7-7 7")
    val Reintentar = icono(
        "reintentar",
        "M4 12a8 8 0 0 1 14-5.3L20 9",
        "M20 4v5h-5",
        "M20 12a8 8 0 0 1-14 5.3L4 15",
        "M4 20v-5h5",
    )
    val Llave = icono("llave", "M4 15a4 4 0 1 0 8 0a4 4 0 1 0 -8 0", "M11 12l9-9", "M17 6l3 3", "M15 8l2 2")
}

private const val HOJA = "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5z"
private const val PLIEGUE = "M14 3v5h5"
private const val CIRCULO = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0"

private fun icono(nombre: String, vararg trazos: String): ImageVector =
    ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
        trazos.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
