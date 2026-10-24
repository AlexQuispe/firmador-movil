package bo.edu.umsa.firmadormovil.cripto

import bo.edu.umsa.firmadormovil.pruebas.CertificadosPrueba
import org.junit.Assert.assertEquals
import org.junit.Test

class ResumenCertificadoTest {

    private val claves = CertificadosPrueba.parClaves()

    @Test
    fun extraeElNombreComunDelTitularYDelEmisor() {
        val titular = CertificadosPrueba.cadena(claves.public).first()
        val resumen = ResumenCertificado.de(titular)
        assertEquals("Titular de pruebas", resumen.titular)
        assertEquals("CA de pruebas", resumen.emisor)
        assertEquals(titular.notAfter, resumen.vigenteHasta)
    }

    @Test
    fun indicaCuandoElNombreNoTieneCn() {
        val sinCn = CertificadosPrueba.emitir("C=BO, O=Sin nombre comun", claves.public, "C=BO, O=Emisor", claves.private)
        assertEquals("(sin CN)", ResumenCertificado.de(sinCn).titular)
    }
}
