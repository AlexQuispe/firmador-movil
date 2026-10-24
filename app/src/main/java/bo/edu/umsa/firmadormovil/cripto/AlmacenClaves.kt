package bo.edu.umsa.firmadormovil.cripto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

/** Clave de firma en Android Keystore, solo con respaldo StrongBox (FM-01 a FM-05). */
class AlmacenClaves(private val alias: String = ALIAS_PREDETERMINADO) {

    private val keyStore: KeyStore = KeyStore.getInstance(PROVEEDOR).apply { load(null) }

    fun existe(): Boolean = keyStore.containsAlias(alias)

    /**
     * FM-01, FM-04. Nunca recurre al TEE: una [StrongBoxUnavailableException]
     * llega a quien llama (FM-03).
     */
    fun generar(tamano: Int = TAMANO_CLAVE) {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setKeySize(tamano)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setIsStrongBoxBacked(true)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PROVEEDOR).run {
            initialize(spec)
            generateKeyPair()
        }
    }

    fun eliminar() = keyStore.deleteEntry(alias)

    fun clavePublica(): PublicKey = keyStore.getCertificate(alias).publicKey

    private fun clavePrivada(): PrivateKey = keyStore.getKey(alias, null) as PrivateKey

    /** FM-02. */
    fun keyInfo(): KeyInfo {
        val clave = clavePrivada()
        return KeyFactory.getInstance(clave.algorithm, PROVEEDOR).getKeySpec(clave, KeyInfo::class.java)
    }

    /** FM-05: una clave privada de Android Keystore no expone su material codificado. */
    fun codificacionClavePrivada(): ByteArray? = clavePrivada().encoded

    /** Firma ligada a la clave; solo se puede usar después de que BiometricPrompt la autoriza (FM-04). */
    fun iniciarFirma(): Signature =
        Signature.getInstance(ALGORITMO_FIRMA).apply { initSign(clavePrivada()) }

    fun verificar(datos: ByteArray, firma: ByteArray): Boolean =
        Signature.getInstance(ALGORITMO_FIRMA).run {
            initVerify(clavePublica())
            update(datos)
            verify(firma)
        }

    companion object {
        const val ALIAS_PREDETERMINADO = "firma_poc"
        const val TAMANO_CLAVE = 2048
        const val ALGORITMO_FIRMA = "SHA256withRSA"
        private const val PROVEEDOR = "AndroidKeyStore"

        fun nombreNivelSeguridad(nivel: Int): String = when (nivel) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> "STRONGBOX"
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TRUSTED_ENVIRONMENT"
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> "SOFTWARE"
            KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> "UNKNOWN_SECURE"
            else -> "UNKNOWN($nivel)"
        }
    }
}
