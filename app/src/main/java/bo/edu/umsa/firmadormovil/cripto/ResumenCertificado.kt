package bo.edu.umsa.firmadormovil.cripto

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import java.security.cert.X509Certificate
import java.util.Date

/** Datos del certificado que el titular ve antes de firmar. */
data class ResumenCertificado(val titular: String, val emisor: String, val vigenteHasta: Date) {
    companion object {
        fun de(certificado: X509Certificate) = ResumenCertificado(
            titular = nombreComun(certificado.subjectX500Principal.encoded),
            emisor = nombreComun(certificado.issuerX500Principal.encoded),
            vigenteHasta = certificado.notAfter,
        )

        private fun nombreComun(nombre: ByteArray): String =
            X500Name.getInstance(nombre).getRDNs(BCStyle.CN).firstOrNull()
                ?.let { IETFUtils.valueToString(it.first.value) } ?: "(sin CN)"
    }
}
