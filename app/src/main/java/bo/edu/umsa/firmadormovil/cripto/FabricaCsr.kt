package bo.edu.umsa.firmadormovil.cripto

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.io.StringWriter
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature

/** CSR PKCS #10 firmada mediante una [Signature] ya inicializada (FM-06). */
object FabricaCsr {

    /** Componente del sujeto tal como el portal de solicitudes lo envía a Jacobitus: OID y valor. */
    data class ComponenteSujeto(val oid: String, val valor: String)

    /**
     * Sujeto de prueba con los campos y el orden del certificado de persona natural de la ECA (FM-14),
     * sin datos de una persona real.
     */
    val SUJETO_PREDETERMINADO = listOf(
        ComponenteSujeto("2.5.4.46", "CI"), // dnQualifier: tipo de documento
        ComponenteSujeto("2.5.4.3", "TITULAR DE PRUEBA"),
        ComponenteSujeto("2.5.4.6", "BO"),
        ComponenteSujeto("1.3.6.1.1.1.1.0", "0000000"), // uidNumber: número de documento ficticio
        ComponenteSujeto("2.5.4.13", "Persona Natural o Fisica Firma Simple"),
    )

    /** Un RDN por componente, en el orden recibido, como `TokenPKCS11.generarCSR` de Jacobitus (FM-14). */
    fun sujeto(componentes: List<ComponenteSujeto>): X500Name =
        X500NameBuilder().apply { componentes.forEach { addRDN(ASN1ObjectIdentifier(it.oid), it.valor) } }.build()

    /**
     * [firma] debe estar inicializada con la clave privada que corresponde a [clavePublica];
     * con una clave del Keystore, es la que devuelve BiometricPrompt tras la autenticación.
     */
    fun construir(sujeto: X500Name, clavePublica: PublicKey, firma: Signature): PKCS10CertificationRequest =
        JcaPKCS10CertificationRequestBuilder(sujeto, clavePublica).build(FirmanteDelegado(firma))

    fun aPem(csr: PKCS10CertificationRequest): String =
        StringWriter().also { salida -> JcaPEMWriter(salida).use { it.writeObject(csr) } }.toString()

    /** SHA-256 del SubjectPublicKeyInfo, para comparar la clave de la CSR y la del certificado (FM-07). */
    fun huellaClavePublica(clavePublica: PublicKey): String =
        MessageDigest.getInstance("SHA-256").digest(clavePublica.encoded).joinToString(":") { "%02X".format(it) }
}
