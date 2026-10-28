package bo.edu.umsa.firmadormovil.documento

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bo.edu.umsa.firmadormovil.pruebas.PdfPrueba
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** FM-08: lo que se muestra al titular corresponde a los bytes que se firmarán. */
@RunWith(AndroidJUnit4::class)
class DocumentoPorFirmarTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun laCopiaElResumenYLasPaginasCorrespondenAlPdfElegido() {
        val origen = PdfPrueba.crear(contexto, File(contexto.cacheDir, "elegido.pdf"), paginas = 3)
        val documento = DocumentoPorFirmar.preparar(contexto, Uri.fromFile(origen))

        assertArrayEquals(origen.readBytes(), documento.archivo.readBytes())
        val esperado = MessageDigest.getInstance("SHA-256").digest(origen.readBytes()).joinToString("") { "%02x".format(it) }
        assertEquals(esperado, documento.sha256)
        assertEquals(3, documento.paginas)
        assertEquals(origen.length(), documento.tamano)
    }

    @Test
    fun generaLaVistaPreviaDeLaPrimeraPagina() {
        val origen = PdfPrueba.crear(contexto, File(contexto.cacheDir, "elegido.pdf"))
        val vista = DocumentoPorFirmar.preparar(contexto, Uri.fromFile(origen)).vistaPrevia
        assertEquals(1080, vista.width)
        assertTrue("Carta vertical: más alta que ancha", vista.height > vista.width)
    }

    @Test
    fun usaElNombreInformadoPorElServicio() {
        val descargado = PdfPrueba.crear(contexto, File(contexto.cacheDir, "descargado.pdf"))
        assertEquals("informe.pdf", DocumentoPorFirmar.preparar(contexto, Uri.fromFile(descargado), "informe.pdf").nombre)
    }
}
