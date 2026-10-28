package bo.edu.umsa.firmadormovil.cripto

import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.edu.umsa.firmadormovil.pruebas.CertificadosPrueba
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SignatureException
import java.security.cert.CertificateExpiredException
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * FM-01 a FM-05 y FM-07 contra el Keystore real del teléfono.
 * Usa un alias propio: nunca toca la clave de trabajo (firma_poc).
 * La firma autorizada con huella o PIN no se automatiza; consta en la evidencia de I1.
 */
@RunWith(AndroidJUnit4::class)
class AlmacenClavesTest {

    private val claves = AlmacenClaves(ALIAS)

    @Before
    fun generarClave() {
        if (claves.existe()) claves.eliminar()
        claves.generar()
    }

    @After
    fun eliminarClave() {
        if (claves.existe()) claves.eliminar()
    }

    @Test
    fun laClaveSeGeneraEnStrongBoxConLosParametrosRequeridos() {
        val info = claves.keyInfo()
        assertEquals(KeyProperties.SECURITY_LEVEL_STRONGBOX, info.securityLevel)
        assertEquals(AlmacenClaves.TAMANO_CLAVE, info.keySize)
        assertEquals(KeyProperties.ORIGIN_GENERATED, info.origin)
        assertEquals(KeyProperties.PURPOSE_SIGN, info.purposes)
        assertArrayEquals(arrayOf(KeyProperties.DIGEST_SHA256), info.digests)
    }

    @Test
    fun cadaUsoExigeAutenticacionImpuestaPorHardware() {
        val info = claves.keyInfo()
        assertTrue(info.isUserAuthenticationRequired)
        assertEquals(0, info.userAuthenticationValidityDurationSeconds)
        assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL, info.userAuthenticationType)
        assertTrue(info.isUserAuthenticationRequirementEnforcedBySecureHardware)
    }

    @Test
    fun laFirmaSinAutenticacionSeRechaza() {
        val firma = claves.iniciarFirma().apply { update(byteArrayOf(1, 2, 3)) }
        assertThrows(SignatureException::class.java) { firma.sign() }
    }

    @Test
    fun laClavePrivadaNoEsExportable() {
        assertNull(claves.codificacionClavePrivada())
    }

    @Test
    fun strongBoxRechazaParametrosNoAdmitidosSinCrearClaveEnTee() {
        val sonda = AlmacenClaves(ALIAS_SONDA)
        try {
            assertThrows(Exception::class.java) { sonda.generar(tamano = 4096) }
            assertFalse(sonda.existe())
        } finally {
            if (sonda.existe()) sonda.eliminar()
        }
    }

    @Test
    fun guardaLaCadenaCuandoElCertificadoCorrespondeALaClave() {
        val cadena = CertificadosPrueba.cadena(claves.clavePublica())
        claves.guardarCadena(cadena)
        assertEquals(cadena, claves.cadena())
        assertEquals(KeyProperties.SECURITY_LEVEL_STRONGBOX, claves.keyInfo().securityLevel)
    }

    @Test
    fun rechazaUnCertificadoDeOtraClave() {
        val otraClave = CertificadosPrueba.parClaves()
        val error = assertThrows(IllegalArgumentException::class.java) {
            claves.guardarCadena(CertificadosPrueba.cadena(otraClave.public))
        }
        assertTrue(error.message!!.contains("no coincide"))
        assertEquals(1, claves.cadena().size)
    }

    @Test
    fun rechazaUnCertificadoVencido() {
        val ca = CertificadosPrueba.parClaves()
        val dia = TimeUnit.DAYS.toMillis(1)
        val vencido = CertificadosPrueba.emitir(
            CertificadosPrueba.SUJETO_TITULAR, claves.clavePublica(), CertificadosPrueba.SUJETO_CA, ca.private,
            desde = Date(System.currentTimeMillis() - 30 * dia), hasta = Date(System.currentTimeMillis() - dia),
        )
        val certificadoCa = CertificadosPrueba.cadena(claves.clavePublica(), ca).last()
        assertThrows(CertificateExpiredException::class.java) { claves.guardarCadena(listOf(vencido, certificadoCa)) }
        assertEquals(1, claves.cadena().size)
    }

    @Test
    fun rechazaUnaCadenaCuyoEmisorNoFirmoElCertificado() {
        val titular = CertificadosPrueba.cadena(claves.clavePublica()).first()
        val otraCa = CertificadosPrueba.cadena(claves.clavePublica()).last()
        assertThrows(SignatureException::class.java) { claves.guardarCadena(listOf(titular, otraCa)) }
        assertEquals(1, claves.cadena().size)
    }

    @Test
    fun eliminarBorraLaClave() {
        claves.eliminar()
        assertFalse(claves.existe())
    }

    private companion object {
        const val ALIAS = "prueba_instrumentada"
        const val ALIAS_SONDA = "prueba_instrumentada_sonda"
    }
}
