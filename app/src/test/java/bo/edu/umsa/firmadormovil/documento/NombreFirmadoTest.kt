package bo.edu.umsa.firmadormovil.documento

import org.junit.Assert.assertEquals
import org.junit.Test

class NombreFirmadoTest {

    @Test
    fun agregaElSufijoAntesDeLaExtension() {
        assertEquals("contrato_firmado.pdf", ArchivosFirmados.nombreFirmado("contrato.pdf"))
    }

    @Test
    fun agregaLaExtensionSiNoLaTiene() {
        assertEquals("informe_firmado.pdf", ArchivosFirmados.nombreFirmado("informe"))
    }

    @Test
    fun noPermiteSalirDeLaCarpetaDeFirmados() {
        assertEquals(".._.._datos_firmado.pdf", ArchivosFirmados.nombreFirmado("../../datos.pdf"))
    }
}
