package bo.edu.umsa.firmadormovil.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.edu.umsa.firmadormovil.cripto.ResumenCertificado
import bo.edu.umsa.firmadormovil.documento.DocumentoPorFirmar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date

/** FM-08: la confirmación muestra el documento y solo firma al pulsar Firmar. */
@RunWith(AndroidJUnit4::class)
class PantallasTitularTest {

    @get:Rule
    val compose = createComposeRule()

    private val certificado = ResumenCertificado("Titular de pruebas", "CA de pruebas", Date())
    private val sha256 = "4c6cff03f46c814d27da8e5fc3d1d51ab2d3d66aa191f452b056710b10ca10e9"
    private val documento = DocumentoPorFirmar(
        nombre = "contrato.pdf",
        archivo = File("/no/existe"),
        sha256 = sha256,
        paginas = 3,
        vistaPrevia = Bitmap.createBitmap(10, 14, Bitmap.Config.ARGB_8888),
    )

    @Test
    fun laConfirmacionMuestraNombreResumenCompletoPaginasYCertificado() {
        compose.setContent { PantallaConfirmacion(documento, certificado, firmando = false, alCancelar = {}, alFirmar = {}) }
        compose.onNodeWithText("contrato.pdf", substring = true).assertIsDisplayed()
        compose.onNodeWithText(sha256.chunked(4).joinToString(" ")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Página 1 de 3").assertIsDisplayed()
        compose.onNodeWithText("Titular de pruebas", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun soloFirmaAlPulsarFirmarYCancelarNoFirma() {
        var firmas = 0
        var cancelaciones = 0
        compose.setContent {
            PantallaConfirmacion(documento, certificado, firmando = false, alCancelar = { cancelaciones++ }, alFirmar = { firmas++ })
        }
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(0, firmas)
        assertEquals(1, cancelaciones)
        compose.onNodeWithText("Firmar").performClick()
        assertEquals(1, firmas)
    }

    @Test
    fun mientrasFirmaLosBotonesQuedanDeshabilitados() {
        compose.setContent { PantallaConfirmacion(documento, certificado, firmando = true, alCancelar = {}, alFirmar = {}) }
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
    }

    @Test
    fun elResultadoGuardaCompartirYVuelveConCadaBoton() {
        val acciones = mutableListOf<String>()
        compose.setContent {
            PantallaResultado(
                nombre = "contrato_firmado.pdf",
                tamano = 20_914,
                alGuardar = { acciones += "guardar" },
                alCompartir = { acciones += "compartir" },
                alVolver = { acciones += "volver" },
            )
        }
        compose.onNodeWithText("contrato_firmado.pdf", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Guardar en…").performClick()
        compose.onNodeWithText("Compartir").performClick()
        compose.onNodeWithText("Volver al inicio").performClick()
        assertEquals(listOf("guardar", "compartir", "volver"), acciones)
    }

    @Test
    fun sinCertificadoNoSePuedeElegirUnPdf() {
        compose.setContent { PantallaInicio(certificado = null, preparando = false, alElegirPdf = {}, alAbrirPruebas = {}) }
        compose.onNodeWithText("No hay un certificado cargado", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Elegir PDF para firmar").assertIsNotEnabled()
    }

    @Test
    fun conCertificadoSePuedeElegirUnPdf() {
        var elegido = false
        compose.setContent { PantallaInicio(certificado, preparando = false, alElegirPdf = { elegido = true }, alAbrirPruebas = {}) }
        compose.onNodeWithText("Elegir PDF para firmar").assertIsEnabled().performClick()
        assertEquals(true, elegido)
    }
}
