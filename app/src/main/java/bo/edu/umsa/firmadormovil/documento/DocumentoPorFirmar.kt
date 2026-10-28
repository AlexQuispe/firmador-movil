package bo.edu.umsa.firmadormovil.documento

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.graphics.createBitmap
import java.io.File
import java.security.MessageDigest

/**
 * PDF elegido por el titular (FM-08). [archivo] es una copia privada: el resumen,
 * la vista previa y la firma se calculan sobre los mismos bytes.
 */
class DocumentoPorFirmar(
    val nombre: String,
    val archivo: File,
    val sha256: String,
    val paginas: Int,
    val vistaPrevia: Bitmap,
) {
    val tamano: Long get() = archivo.length()

    companion object {
        private const val ANCHO_VISTA_PREVIA = 1080

        /**
         * Hace trabajo de E/S: llamar fuera del hilo principal. [nombreInformado] es el que informa
         * firmador-servicio (FM-13); si falta, se usa el nombre visible del archivo elegido.
         */
        fun preparar(contexto: Context, uri: Uri, nombreInformado: String? = null): DocumentoPorFirmar {
            val nombre = nombreInformado ?: nombreVisible(contexto, uri)
            val copia = File(contexto.cacheDir, "por_firmar.pdf")
            contexto.contentResolver.openInputStream(uri).use { entrada ->
                requireNotNull(entrada) { "no se pudo abrir el documento" }
                copia.outputStream().use { entrada.copyTo(it) }
            }
            val resumen = MessageDigest.getInstance("SHA-256").digest(copia.readBytes())
                .joinToString("") { "%02x".format(it) }
            ParcelFileDescriptor.open(copia, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderizador ->
                    val vista = renderizador.openPage(0).use { pagina ->
                        val alto = ANCHO_VISTA_PREVIA * pagina.height / pagina.width
                        createBitmap(ANCHO_VISTA_PREVIA, alto).also {
                            it.eraseColor(Color.WHITE)
                            pagina.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                    return DocumentoPorFirmar(nombre, copia, resumen, renderizador.pageCount, vista)
                }
            }
        }

        private fun nombreVisible(contexto: Context, uri: Uri): String =
            contexto.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: "documento.pdf"
    }
}
