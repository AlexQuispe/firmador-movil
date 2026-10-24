package bo.edu.umsa.firmadormovil.pruebas

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** CA y certificados de software para pruebas; nunca se usan fuera de ellas. */
object CertificadosPrueba {

    const val SUJETO_CA = "C=BO, O=Pruebas, CN=CA de pruebas"
    const val SUJETO_TITULAR = "C=BO, O=Pruebas, CN=Titular de pruebas"

    private val serial = AtomicLong(1)
    private val dia = TimeUnit.DAYS.toMillis(1)

    fun parClaves(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    fun emitir(
        sujeto: String,
        clavePublica: PublicKey,
        emisor: String,
        claveEmisor: PrivateKey,
        esCa: Boolean = false,
        desde: Date = Date(System.currentTimeMillis() - dia),
        hasta: Date = Date(System.currentTimeMillis() + 365 * dia),
    ): X509Certificate {
        val uso = if (esCa) KeyUsage.keyCertSign or KeyUsage.cRLSign else KeyUsage.digitalSignature or KeyUsage.nonRepudiation
        val constructor = JcaX509v3CertificateBuilder(
            X500Name(emisor), BigInteger.valueOf(serial.getAndIncrement()), desde, hasta, X500Name(sujeto), clavePublica,
        )
            .addExtension(Extension.basicConstraints, true, BasicConstraints(esCa))
            .addExtension(Extension.keyUsage, true, KeyUsage(uso))
        val firmante = JcaContentSignerBuilder("SHA256withRSA").build(claveEmisor)
        return JcaX509CertificateConverter().getCertificate(constructor.build(firmante))
    }

    /** Cadena titular → CA para [clavePublica], emitida por una CA nueva. */
    fun cadena(clavePublica: PublicKey, ca: KeyPair = parClaves()): List<X509Certificate> {
        val certificadoCa = emitir(SUJETO_CA, ca.public, SUJETO_CA, ca.private, esCa = true)
        return listOf(emitir(SUJETO_TITULAR, clavePublica, SUJETO_CA, ca.private), certificadoCa)
    }
}
