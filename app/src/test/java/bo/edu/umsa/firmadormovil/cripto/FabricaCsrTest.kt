package bo.edu.umsa.firmadormovil.cripto

import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature

/** Usa una clave de software: el Keystore solo existe en el teléfono. */
class FabricaCsrTest {

    private val parClaves = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private val sujeto = FabricaCsr.sujeto(FabricaCsr.SUJETO_PREDETERMINADO)

    private fun construirCsr() = FabricaCsr.construir(
        sujeto,
        parClaves.public,
        Signature.getInstance(AlmacenClaves.ALGORITMO_FIRMA).apply { initSign(parClaves.private) },
    )

    @Test
    fun laFirmaDeLaCsrVerificaConSuClavePublica() {
        val csr = construirCsr()
        assertTrue(csr.isSignatureValid(JcaContentVerifierProviderBuilder().build(csr.subjectPublicKeyInfo)))
    }

    @Test
    fun laCsrLlevaSujetoClaveYAlgoritmo() {
        val csr = JcaPKCS10CertificationRequest(construirCsr())
        assertEquals(sujeto, csr.subject)
        assertArrayEquals(parClaves.public.encoded, csr.publicKey.encoded)
        assertEquals("1.2.840.113549.1.1.11", csr.signatureAlgorithm.algorithm.id) // sha256WithRSAEncryption
    }

    @Test
    fun elPemTieneEncabezadosDeSolicitud() {
        val pem = FabricaCsr.aPem(construirCsr())
        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE REQUEST-----"))
        assertTrue(pem.trimEnd().endsWith("-----END CERTIFICATE REQUEST-----"))
    }
}
