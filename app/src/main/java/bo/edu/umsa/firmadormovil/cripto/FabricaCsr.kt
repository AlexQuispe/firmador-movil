package bo.edu.umsa.firmadormovil.cripto

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.io.StringWriter
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature

/** CSR PKCS #10 firmada mediante una [Signature] ya inicializada (FM-06). */
object FabricaCsr {

    /** Sujeto de laboratorio, sin datos personales; mismo orden de RDN que la CA (C primero). */
    val SUJETO_PREDETERMINADO = X500Name("C=BO, O=Prueba de concepto firmador movil, OU=Laboratorio, CN=Titular de prueba")

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
