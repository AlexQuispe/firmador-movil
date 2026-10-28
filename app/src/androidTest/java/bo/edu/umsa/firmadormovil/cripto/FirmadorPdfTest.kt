package bo.edu.umsa.firmadormovil.cripto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bo.edu.umsa.firmadormovil.pruebas.CertificadosPrueba
import bo.edu.umsa.firmadormovil.pruebas.PdfPrueba
import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.cms.CMSAttributes
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.asn1.ess.SigningCertificateV2
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.security.Signature

/**
 * FM-09 y R2 con PDFBox-Android real. La clave es de software: la delegación en el
 * Keystore usa el mismo FirmanteDelegado y consta en la evidencia de I3.
 */
@RunWith(AndroidJUnit4::class)
class FirmadorPdfTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val claves = CertificadosPrueba.parClaves()
    private val cadena = CertificadosPrueba.cadena(claves.public)
    private lateinit var original: File
    private lateinit var firmado: File

    @Before
    fun firmarPdf() {
        original = PdfPrueba.crear(contexto, File(contexto.cacheDir, "prueba_original.pdf"), paginas = 2)
        firmado = File(contexto.cacheDir, "prueba_firmada.pdf")
        val firma = Signature.getInstance(AlmacenClaves.ALGORITMO_FIRMA).apply { initSign(claves.private) }
        FirmadorPdf.firmar(original, firmado, cadena, firma)
    }

    @Test
    fun usaElSubFiltroDePades() {
        val diccionario = diccionarioFirma()
        assertEquals("Adobe.PPKLite", diccionario.filter)
        assertEquals("ETSI.CAdES.detached", diccionario.subFilter)
        assertNotNull(diccionario.signDate)
    }

    @Test
    fun conservaLosBytesDelOriginalComoActualizacionIncremental() {
        val bytesOriginal = original.readBytes()
        assertArrayEquals(bytesOriginal, firmado.readBytes().copyOf(bytesOriginal.size))
    }

    @Test
    fun laFirmaCmsVerificaSobreElRangoFirmadoConElCertificadoDelTitular() {
        val firmante = firmante()
        assertTrue(firmante.verify(JcaSimpleSignerInfoVerifierBuilder().build(cadena.first())))
        assertEquals("2.16.840.1.101.3.4.2.1", firmante.digestAlgOID) // SHA-256
    }

    @Test
    fun incluyeLosAtributosDePadesBaselineB() {
        val atributos = firmante().signedAttributes
        assertNotNull(atributos[CMSAttributes.contentType])
        assertNotNull(atributos[CMSAttributes.messageDigest])
        assertNull("PAdES no admite signing-time en los atributos firmados", atributos[CMSAttributes.signingTime])
        val certificadoFirmante = SigningCertificateV2.getInstance(
            atributos[PKCSObjectIdentifiers.id_aa_signingCertificateV2].attrValues.getObjectAt(0),
        )
        val hashTitular = MessageDigest.getInstance("SHA-256").digest(cadena.first().encoded)
        assertArrayEquals(hashTitular, certificadoFirmante.certs.first().certHash)
    }

    @Test
    fun incluyeLaCadenaCompletaEnElCms() {
        val incluidos = cms().certificates.getMatches(null).map { (it as X509CertificateHolder).encoded.toList() }
        cadena.forEach { assertTrue(it.encoded.toList() in incluidos) }
    }

    @Test
    fun detectaUnaAlteracionDelContenidoFirmado() {
        val bytes = firmado.readBytes()
        val rango = diccionarioFirma().byteRange
        val alterado = bytes.copyOf().also { it[rango[1] / 2] = (it[rango[1] / 2] + 1).toByte() }
        val contenido = diccionarioFirma().getContents(alterado)
        val alteradoFirmado = diccionarioFirma().getSignedContent(alterado)
        val firmanteAlterado = leerCms(alteradoFirmado, contenido).signerInfos.signers.first()
        assertFalse(runCatching { firmanteAlterado.verify(JcaSimpleSignerInfoVerifierBuilder().build(cadena.first())) }.getOrDefault(false))
    }

    private fun diccionarioFirma() = PDDocument.load(firmado).use { it.signatureDictionaries.single() }

    private fun cms(): CMSSignedData {
        val bytes = firmado.readBytes()
        val diccionario = diccionarioFirma()
        return leerCms(diccionario.getSignedContent(bytes), diccionario.getContents(bytes))
    }

    /** /Contents trae relleno de ceros tras el CMS; como los validadores, se lee solo el primer objeto ASN.1. */
    private fun leerCms(contenidoFirmado: ByteArray, contents: ByteArray): CMSSignedData =
        CMSSignedData(CMSProcessableByteArray(contenidoFirmado), ContentInfo.getInstance(ASN1InputStream(contents).readObject()))

    private fun firmante(): SignerInformation = cms().signerInfos.signers.single()
}
