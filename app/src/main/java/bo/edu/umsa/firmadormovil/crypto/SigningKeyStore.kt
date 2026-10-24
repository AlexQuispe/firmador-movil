package bo.edu.umsa.firmadormovil.crypto

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

/** Signing key kept in Android Keystore, StrongBox-backed only (FM-01 to FM-05). */
class SigningKeyStore(private val alias: String = DEFAULT_ALIAS) {

    private val keyStore: KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    fun exists(): Boolean = keyStore.containsAlias(alias)

    /**
     * FM-01, FM-04. Never falls back to TEE: a [StrongBoxUnavailableException]
     * reaches the caller (FM-03).
     */
    fun generate(keySize: Int = KEY_SIZE) {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setKeySize(keySize)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setIsStrongBoxBacked(true)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PROVIDER).run {
            initialize(spec)
            generateKeyPair()
        }
    }

    fun delete() = keyStore.deleteEntry(alias)

    fun publicKey(): PublicKey = keyStore.getCertificate(alias).publicKey

    private fun privateKey(): PrivateKey = keyStore.getKey(alias, null) as PrivateKey

    /** FM-02. */
    fun keyInfo(): KeyInfo {
        val key = privateKey()
        return KeyFactory.getInstance(key.algorithm, PROVIDER).getKeySpec(key, KeyInfo::class.java)
    }

    /** FM-05: an Android Keystore private key exposes no encoded material. */
    fun privateKeyEncoded(): ByteArray? = privateKey().encoded

    /** Signature bound to the key; usable only after BiometricPrompt authenticates it (FM-04). */
    fun initSignature(): Signature =
        Signature.getInstance(SIGNATURE_ALGORITHM).apply { initSign(privateKey()) }

    fun verify(data: ByteArray, signature: ByteArray): Boolean =
        Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(publicKey())
            update(data)
            verify(signature)
        }

    companion object {
        const val DEFAULT_ALIAS = "firma_poc"
        const val KEY_SIZE = 2048
        const val SIGNATURE_ALGORITHM = "SHA256withRSA"
        private const val PROVIDER = "AndroidKeyStore"

        fun securityLevelName(level: Int): String = when (level) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> "STRONGBOX"
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TRUSTED_ENVIRONMENT"
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> "SOFTWARE"
            KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> "UNKNOWN_SECURE"
            else -> "UNKNOWN($level)"
        }
    }
}
