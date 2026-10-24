package bo.edu.umsa.firmadormovil.documento

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

/** FM-10: el PDF firmado sale sin cambios y solo desde la carpeta de firmados. */
@RunWith(AndroidJUnit4::class)
class ArchivosFirmadosTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val contenido = Random(7).nextBytes(4096)
    private val firmado = ArchivosFirmados.destino(contexto, "prueba_instrumentada.pdf").apply { writeBytes(contenido) }

    @After
    fun limpiar() {
        firmado.delete()
    }

    @Test
    fun elDestinoEstaEnLaCarpetaDeFirmados() {
        assertEquals(contexto.getExternalFilesDir(ArchivosFirmados.CARPETA), firmado.parentFile)
        assertEquals("prueba_instrumentada_firmado.pdf", firmado.name)
    }

    @Test
    fun guardarCopiaElArchivoSinCambios() {
        val copia = File(contexto.cacheDir, "copia_guardada.pdf")
        ArchivosFirmados.guardarEn(contexto, firmado, Uri.fromFile(copia))
        assertArrayEquals(contenido, copia.readBytes())
        copia.delete()
    }

    @Test
    fun elUriCompartidoEntregaElMismoContenido() {
        val uri = ArchivosFirmados.uriCompartible(contexto, firmado)
        assertEquals("content", uri.scheme)
        assertEquals("${contexto.packageName}.archivos", uri.authority)
        val leido = contexto.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertArrayEquals(contenido, leido)
    }

    @Test
    fun noComparteArchivosFueraDeLaCarpetaDeFirmados() {
        val privado = File(contexto.filesDir, "privado.pdf").apply { writeBytes(contenido) }
        assertThrows(IllegalArgumentException::class.java) { ArchivosFirmados.uriCompartible(contexto, privado) }
        privado.delete()
    }
}
