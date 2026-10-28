package bo.edu.umsa.firmadormovil.documento

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Ubicación, guardado y envío de los PDF firmados (FM-10). */
object ArchivosFirmados {

    /** Única carpeta expuesta por el FileProvider (res/xml/rutas_compartidas.xml). */
    const val CARPETA = "firmados"

    fun destino(contexto: Context, nombreOriginal: String): File =
        File(contexto.getExternalFilesDir(CARPETA), nombreFirmado(nombreOriginal))

    fun nombreFirmado(nombreOriginal: String): String =
        nombreOriginal.replace('/', '_').removeSuffix(".pdf") + "_firmado.pdf"

    /** Copia [origen] sin cambios al destino que eligió el titular. Hace E/S: fuera del hilo principal. */
    fun guardarEn(contexto: Context, origen: File, destino: Uri) {
        contexto.contentResolver.openOutputStream(destino).use { salida ->
            requireNotNull(salida) { "no se pudo abrir el destino" }
            origen.inputStream().use { it.copyTo(salida) }
        }
    }

    /** URI de contenido para compartir; falla si [archivo] está fuera de [CARPETA]. */
    fun uriCompartible(contexto: Context, archivo: File): Uri =
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
}
