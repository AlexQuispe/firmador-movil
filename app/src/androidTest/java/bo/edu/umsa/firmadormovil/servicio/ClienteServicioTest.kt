package bo.edu.umsa.firmadormovil.servicio

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bo.edu.umsa.firmadormovil.BuildConfig
import bo.edu.umsa.firmadormovil.cripto.AlmacenClaves
import bo.edu.umsa.firmadormovil.cripto.FirmadorPdf
import bo.edu.umsa.firmadormovil.pruebas.CertificadosPrueba
import bo.edu.umsa.firmadormovil.pruebas.PdfPrueba
import bo.edu.umsa.firmadormovil.pruebas.ServicioPrueba
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.security.Signature

/** FM-13: el enlace solo abre solicitudes con ID UUID y servicio admitido, y las llamadas cumplen el contrato del servicio. */
@RunWith(AndroidJUnit4::class)
class ClienteServicioTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val id = "3f2b8c1e-6a4d-4e7f-9b2a-1c5d8e0f7a3b"

    private fun enlace(texto: String) = Intent(Intent.ACTION_VIEW, Uri.parse(texto))

    private val servicio = Uri.encode("https://firma.ejemplo.bo")

    @Test
    fun aceptaElEnlaceDeFirmaConUnIdUuidYElServicio() {
        val abierto = ClienteServicio.enlace(enlace("firmadormovil://firmar/$id?servicio=$servicio"))!!
        assertEquals(id, abierto.id)
        assertEquals("https://firma.ejemplo.bo", abierto.servicio)
        assertEquals("firma.ejemplo.bo", abierto.origen)
        assertNull(abierto.sistema)
    }

    @Test
    fun ignoraOtrosEsquemasHostsEIdsQueNoSonUuid() {
        listOf(
            "https://firmar/$id?servicio=$servicio",
            "firmadormovil://otra/$id?servicio=$servicio",
            "firmadormovil://firmar/123?servicio=$servicio",
            "firmadormovil://firmar/..%2F$id?servicio=$servicio",
            "firmadormovil://firmar/?servicio=$servicio",
            "firmadormovil://firmar/$id",
        ).forEach { assertNull(it, ClienteServicio.enlace(enlace(it))) }
        assertNull(ClienteServicio.enlace(Intent(Intent.ACTION_MAIN)))
    }

    @Test
    fun iniciaYDescargaElMismoPdfQueSubioLaWeb() {
        val (solicitud, original) = solicitudNueva("fm13_original.pdf")

        val datos = ClienteServicio.iniciar(solicitud)
        assertEquals("fm13_original.pdf", datos.nombre)
        assertEquals(sha256(original), datos.sha256)
        assertEquals("EN_PROCESO", ServicioPrueba.estado(solicitud.id))

        val descargado = File(contexto.cacheDir, "fm13_descargado.pdf")
        ClienteServicio.descargar(solicitud, descargado)
        assertArrayEquals(original.readBytes(), descargado.readBytes())
    }

    @Test
    fun entregaElPdfFirmadoYElServicioLeeAlTitular() {
        val (solicitud, original) = solicitudNueva("fm13_por_firmar.pdf")
        ClienteServicio.iniciar(solicitud)

        // Clave de software: la firma con el Keystore exige autenticación del titular.
        val claves = CertificadosPrueba.parClaves()
        val firmado = File(contexto.cacheDir, "fm13_firmado.pdf")
        val firma = Signature.getInstance(AlmacenClaves.ALGORITMO_FIRMA).apply { initSign(claves.private) }
        FirmadorPdf.firmar(original, firmado, CertificadosPrueba.cadena(claves.public), firma)
        ClienteServicio.entregar(solicitud, firmado)

        assertEquals("FIRMADA", ServicioPrueba.estado(solicitud.id))
        val documento = ServicioPrueba.documento(solicitud.id)
        assertEquals(sha256(firmado), documento.getString("sha256"))
        val firmas = documento.getJSONArray("firmas")
        assertEquals("Titular de pruebas", firmas.getJSONObject(firmas.length() - 1).getString("titular"))
    }

    @Test
    fun registraElRechazo() {
        val (solicitud, _) = solicitudNueva("fm13_rechazado.pdf")
        ClienteServicio.iniciar(solicitud)

        ClienteServicio.rechazar(solicitud)

        assertEquals("RECHAZADA", ServicioPrueba.estado(solicitud.id))
    }

    private fun solicitudNueva(nombre: String): Pair<ClienteServicio.Enlace, File> {
        assumeTrue("requiere firmador-servicio en SERVICIO_URL", ServicioPrueba.disponible())
        val pdf = PdfPrueba.crear(contexto, File(contexto.cacheDir, nombre))
        return ClienteServicio.Enlace(ServicioPrueba.crearSolicitud(pdf), BuildConfig.SERVICIO_URL) to pdf
    }

    private fun sha256(archivo: File): String =
        MessageDigest.getInstance("SHA-256").digest(archivo.readBytes()).joinToString("") { "%02x".format(it) }
}
