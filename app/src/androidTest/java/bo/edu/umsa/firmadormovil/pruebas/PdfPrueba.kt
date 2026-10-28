package bo.edu.umsa.firmadormovil.pruebas

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File

/** PDF sin datos reales para las pruebas instrumentadas. */
object PdfPrueba {
    fun crear(contexto: Context, destino: File, paginas: Int = 1): File {
        PDFBoxResourceLoader.init(contexto)
        PDDocument().use { documento ->
            repeat(paginas) { numero ->
                val pagina = PDPage().also(documento::addPage)
                PDPageContentStream(documento, pagina).use {
                    it.beginText()
                    it.setFont(PDType1Font.HELVETICA, 12f)
                    it.newLineAtOffset(72f, 700f)
                    it.showText("Prueba instrumentada, pagina ${numero + 1}")
                    it.endText()
                }
            }
            documento.save(destino)
        }
        return destino
    }
}
