package bo.edu.umsa.firmadormovil

import android.app.KeyguardManager
import android.os.Bundle
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import bo.edu.umsa.firmadormovil.crypto.SigningKeyStore
import bo.edu.umsa.firmadormovil.ui.theme.FirmadorMovilTheme
import java.security.MessageDigest

/** I1 screen: protected key checks (FM-01 to FM-05). */
class MainActivity : FragmentActivity() {

    private val keys by lazy { SigningKeyStore() }
    private val output = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirmadorMovilTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Firmador móvil · I1", style = MaterialTheme.typography.titleLarge)
                        ActionButton("Generar clave (StrongBox)") { generateKey() }
                        ActionButton("Consultar KeyInfo") { reportKeyInfo() }
                        ActionButton("Firmar prueba con autenticación") { signWithAuthentication() }
                        ActionButton("Firmar sin autenticación (debe fallar)") { signWithoutAuthentication() }
                        ActionButton("Intentar exportar la clave privada") { tryExport() }
                        OutlinedButton(onClick = { probeStrongBoxRejection() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Probar FM-03: RSA 4096 en StrongBox")
                        }
                        OutlinedButton(onClick = { deleteKey() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Eliminar clave")
                        }
                        SelectionContainer {
                            Text(
                                output.joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ActionButton(label: String, onClick: () -> Unit) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
    }

    /** Shown on screen and in logcat; never receives secrets or document content. */
    private fun report(message: String) {
        Log.i(TAG, message)
        output.add(message)
    }

    private fun guarded(step: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            report("$step: ERROR ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun generateKey() = guarded("FM-01") {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!keyguard.isDeviceSecure) {
            report("FM-01: sin bloqueo de pantalla seguro; configure PIN, patrón o contraseña")
            return@guarded
        }
        if (keys.exists()) {
            report("FM-01: ya existe la clave '${SigningKeyStore.DEFAULT_ALIAS}'; elimínela para regenerar")
            return@guarded
        }
        try {
            val started = System.nanoTime()
            keys.generate()
            val ms = (System.nanoTime() - started) / 1_000_000
            report("FM-01: clave RSA ${SigningKeyStore.KEY_SIZE} generada, alias '${SigningKeyStore.DEFAULT_ALIAS}', ${ms} ms")
        } catch (e: StrongBoxUnavailableException) {
            report("FM-03: StrongBox no disponible; no se generó ninguna clave (${e.message})")
        }
    }

    private fun reportKeyInfo() = guarded("FM-02") {
        val info = keys.keyInfo()
        val level = SigningKeyStore.securityLevelName(info.securityLevel)
        val authTypes = buildList {
            if (info.userAuthenticationType and KeyProperties.AUTH_BIOMETRIC_STRONG != 0) add("BIOMETRIC_STRONG")
            if (info.userAuthenticationType and KeyProperties.AUTH_DEVICE_CREDENTIAL != 0) add("DEVICE_CREDENTIAL")
        }
        report("FM-02: securityLevel=$level (${if (level == "STRONGBOX") "CUMPLE" else "NO CUMPLE"})")
        report(
            "FM-02: keySize=${info.keySize}, origin=${if (info.origin == KeyProperties.ORIGIN_GENERATED) "GENERATED" else info.origin}, " +
                "purposes=${if (info.purposes == KeyProperties.PURPOSE_SIGN) "SIGN" else info.purposes}"
        )
        report(
            "FM-04: userAuthenticationRequired=${info.isUserAuthenticationRequired}, " +
                "timeout=${info.userAuthenticationValidityDurationSeconds}s, types=$authTypes, " +
                "enforcedBySecureHardware=${info.isUserAuthenticationRequirementEnforcedBySecureHardware}"
        )
    }

    private fun signWithAuthentication() = guarded("FM-04") {
        val signature = keys.initSignature()
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = guarded("FM-04") {
                    val authorized = result.cryptoObject?.signature ?: error("sin CryptoObject")
                    val data = TEST_DATA.toByteArray()
                    val started = System.nanoTime()
                    authorized.update(data)
                    val value = authorized.sign()
                    val ms = (System.nanoTime() - started) / 1_000_000
                    val verified = keys.verify(data, value)
                    report("FM-04: firma tras autenticación OK, ${value.size} bytes, ${ms} ms, verificada con la clave pública=$verified")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    report("FM-04: autenticación cancelada o fallida ($errorCode: $errString)")
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autorizar firma de prueba")
            .setSubtitle("SHA-256 de los datos: ${sha256Hex(TEST_DATA.toByteArray()).take(16)}…")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(signature))
    }

    private fun signWithoutAuthentication() {
        try {
            keys.initSignature().run {
                update(TEST_DATA.toByteArray())
                sign()
            }
            report("FM-04: NO CUMPLE, la firma se produjo sin autenticación")
        } catch (e: Exception) {
            val reason = e.message.orEmpty().substringAfter("KeyStoreException: ").substringBefore(" (")
            report("FM-04: firma sin autenticación rechazada (${e.javaClass.simpleName}: $reason) CUMPLE")
        }
    }

    private fun tryExport() = guarded("FM-05") {
        val encoded = keys.privateKeyEncoded()
        report("FM-05: PrivateKey.getEncoded() ${if (encoded == null) "= null, no exportable CUMPLE" else "devolvió datos NO CUMPLE"}")
    }

    /** StrongBox only supports RSA 2048; the request must fail without a TEE key being created. */
    private fun probeStrongBoxRejection() {
        val probe = SigningKeyStore(PROBE_ALIAS)
        try {
            probe.generate(keySize = 4096)
            report("FM-03: RSA 4096 aceptada, nivel=${SigningKeyStore.securityLevelName(probe.keyInfo().securityLevel)}; prueba no concluyente")
        } catch (e: Exception) {
            report("FM-03: rechazo informado (${e.javaClass.simpleName}); clave creada=${probe.exists()}")
        } finally {
            if (probe.exists()) probe.delete()
        }
    }

    private fun deleteKey() = guarded("Eliminar") {
        keys.delete()
        report("Clave '${SigningKeyStore.DEFAULT_ALIAS}' eliminada")
    }

    private fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "FirmadorMovil"
        const val PROBE_ALIAS = "fm03_sonda"
        const val TEST_DATA = "Datos de prueba I1 - firmador movil"
    }
}
