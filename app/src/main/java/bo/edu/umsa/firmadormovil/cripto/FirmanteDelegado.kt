package bo.edu.umsa.firmadormovil.cripto

import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
import java.io.OutputStream
import java.security.Signature

/**
 * Pasa a [firma] los bytes por firmar; la clave privada no sale de su proveedor.
 * Con el Keystore, [firma] es la que autorizó BiometricPrompt.
 */
internal class FirmanteDelegado(private val firma: Signature) : ContentSigner {
    private val flujo = object : OutputStream() {
        override fun write(b: Int) = firma.update(b.toByte())
        override fun write(b: ByteArray, off: Int, len: Int) = firma.update(b, off, len)
    }

    override fun getAlgorithmIdentifier(): AlgorithmIdentifier =
        DefaultSignatureAlgorithmIdentifierFinder().find(AlmacenClaves.ALGORITMO_FIRMA)

    override fun getOutputStream(): OutputStream = flujo

    override fun getSignature(): ByteArray = firma.sign()
}
