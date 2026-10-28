package bo.edu.umsa.firmadormovil

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bo.edu.umsa.firmadormovil.BuildConfig
import bo.edu.umsa.firmadormovil.cripto.AlmacenClaves
import bo.edu.umsa.firmadormovil.pruebas.PdfPrueba
import bo.edu.umsa.firmadormovil.pruebas.ServicioPrueba
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * FM-13 con la actividad real: el enlace lleva a la misma confirmación de FM-08 y cancelar
 * registra el rechazo y cierra la app. No firma, así que no pide autenticación ni usa la clave.
 */
@RunWith(AndroidJUnit4::class)
class FlujoSolicitudTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun enlace(id: String) = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("firmadormovil://firmar/$id?servicio=${Uri.encode(BuildConfig.SERVICIO_URL)}"),
    ).setClass(contexto, MainActivity::class.java)

    @Test
    fun elEnlaceMuestraLaConfirmacionYCancelarRegistraElRechazoYCierraLaApp() {
        assumeTrue("requiere firmador-servicio en SERVICIO_URL", ServicioPrueba.disponible())
        assumeTrue("requiere el certificado del titular (FM-07)", AlmacenClaves().run { existe() && cadena().size >= 2 })
        val pdf = PdfPrueba.crear(contexto, File(contexto.cacheDir, "fm13_flujo.pdf"))
        val solicitud = ServicioPrueba.crearSolicitud(pdf)

        ActivityScenario.launch<MainActivity>(enlace(solicitud)).use { escenario ->
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Solicitud de firma").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("fm13_flujo.pdf", substring = true).assertIsDisplayed()
            compose.onNodeWithText(Uri.parse(BuildConfig.SERVICIO_URL).authority!!).assertIsDisplayed()

            compose.onNodeWithText("Rechazar").performClick()

            esperar { ServicioPrueba.estado(solicitud) == "RECHAZADA" }
            esperar { escenario.state == Lifecycle.State.DESTROYED }
        }
    }

    @Test
    fun unEnlaceConIdInvalidoNoAbreNingunaSolicitud() {
        ActivityScenario.launch<MainActivity>(enlace("no-es-un-uuid")).use {
            compose.onNodeWithText("Elegir PDF para firmar").assertIsDisplayed()
            assertTrue(compose.onAllNodesWithText("Confirmar firma").fetchSemanticsNodes().isEmpty())
        }
    }

    private fun esperar(condicion: () -> Boolean) {
        val limite = System.currentTimeMillis() + 10_000
        while (!condicion()) {
            check(System.currentTimeMillis() < limite) { "la condición no se cumplió a tiempo" }
            Thread.sleep(200)
        }
    }
}
