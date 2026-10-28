package bo.edu.umsa.firmadormovil.cripto

import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.DERSet
import org.bouncycastle.asn1.cms.Attribute
import org.bouncycastle.asn1.cms.AttributeTable
import org.bouncycastle.asn1.cms.CMSAttributes
import org.bouncycastle.asn1.ess.ESSCertIDv2
import org.bouncycastle.asn1.ess.SigningCertificateV2
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.IssuerSerial
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator
import org.bouncycastle.cms.SignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Calendar

/** Firma PAdES B-B (ETSI.CAdES.detached) con firmante externo (R2, FM-09). */
object FirmadorPdf {

    private val SUB_FILTER_PADES = COSName.getPDFName("ETSI.CAdES.detached")

    /**
     * Agrega a [entrada] una firma con actualización incremental y la escribe en [salida].
     * [cadena] empieza por el certificado del titular; [firma] es la autorizada por el usuario.
     */
    fun firmar(entrada: File, salida: File, cadena: List<X509Certificate>, firma: Signature) {
        val diccionario = PDSignature().apply {
            setFilter(PDSignature.FILTER_ADOBE_PPKLITE)
            setSubFilter(SUB_FILTER_PADES)
            signDate = Calendar.getInstance()
        }
        PDDocument.load(entrada).use { documento ->
            SignatureOptions().use { opciones ->
                documento.addSignature(diccionario, opciones)
                salida.outputStream().use { flujo ->
                    val externa = documento.saveIncrementalForExternalSigning(flujo)
                    externa.setSignature(construirCms(externa.content, cadena, firma))
                }
            }
        }
    }

    /** CMS SignedData separado: firma sobre el ByteRange que entrega PDFBox. */
    private fun construirCms(contenido: InputStream, cadena: List<X509Certificate>, firma: Signature): ByteArray {
        val titular = cadena.first()
        val generadorFirmante = SignerInfoGeneratorBuilder(JcaDigestCalculatorProviderBuilder().build())
            .setSignedAttributeGenerator(AtributosPades(titular))
            .build(FirmanteDelegado(firma), JcaX509CertificateHolder(titular))
        val generador = CMSSignedDataGenerator().apply {
            addSignerInfoGenerator(generadorFirmante)
            addCertificates(JcaCertStore(cadena))
        }
        return generador.generate(CMSProcessableByteArray(contenido.readBytes()), false).getEncoded("DER")
    }

    /**
     * Atributos firmados de PAdES B-B: content-type, message-digest y signing-certificate-v2.
     * Sin signing-time: en PAdES la hora va en el diccionario de firma (/M).
     */
    private class AtributosPades(titular: X509Certificate) :
        DefaultSignedAttributeTableGenerator(AttributeTable(ASN1EncodableVector().apply { add(certificadoFirmante(titular)) })) {

        override fun getAttributes(parametros: MutableMap<Any?, Any?>?): AttributeTable =
            super.getAttributes(parametros).remove(CMSAttributes.signingTime)
    }

    private fun certificadoFirmante(titular: X509Certificate): Attribute {
        val hash = MessageDigest.getInstance("SHA-256").digest(titular.encoded)
        val emisorSerial = IssuerSerial(
            GeneralNames(GeneralName(X500Name.getInstance(titular.issuerX500Principal.encoded))),
            titular.serialNumber,
        )
        return Attribute(
            PKCSObjectIdentifiers.id_aa_signingCertificateV2,
            DERSet(SigningCertificateV2(arrayOf(ESSCertIDv2(hash, emisorSerial)))),
        )
    }
}
