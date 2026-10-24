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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import bo.edu.umsa.firmadormovil.cripto.AlmacenClaves
import bo.edu.umsa.firmadormovil.cripto.FabricaCsr
import bo.edu.umsa.firmadormovil.cripto.FirmadorPdf
import bo.edu.umsa.firmadormovil.ui.tema.TemaFirmadorMovil
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** Pantalla de pruebas: clave protegida (FM-01 a FM-05), CSR (FM-06) y certificado (FM-07). */
class MainActivity : FragmentActivity() {

    private val claves by lazy { AlmacenClaves() }
    private val salida = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            TemaFirmadorMovil {
                Scaffold(modifier = Modifier.fillMaxSize()) { margenes ->
                    Column(
                        modifier = Modifier
                            .padding(margenes)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Firmador móvil · I1–I2", style = MaterialTheme.typography.titleLarge)
                        BotonAccion("Generar clave (StrongBox)") { generarClave() }
                        BotonAccion("Consultar KeyInfo") { consultarKeyInfo() }
                        BotonAccion("Firmar prueba con autenticación") { firmarConAutenticacion() }
                        BotonAccion("Firmar sin autenticación (debe fallar)") { firmarSinAutenticacion() }
                        BotonAccion("Intentar exportar la clave privada") { intentarExportar() }
                        BotonAccion("Generar CSR (FM-06)") { generarCsr() }
                        BotonAccion("Cargar certificado (FM-07)") { cargarCertificado() }
                        BotonAccion("Crear PDF de prueba") { crearPdfPrueba() }
                        BotonAccion("Firmar PDF de prueba (R2)") { firmarPdfPrueba() }
                        OutlinedButton(onClick = { probarRechazoStrongBox() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Probar FM-03: RSA 4096 en StrongBox")
                        }
                        OutlinedButton(onClick = { eliminarClave() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Eliminar clave")
                        }
                        SelectionContainer {
                            Text(
                                salida.joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BotonAccion(texto: String, alPulsar: () -> Unit) {
        Button(onClick = alPulsar, modifier = Modifier.fillMaxWidth()) { Text(texto) }
    }

    /** Se muestra en pantalla y en logcat; nunca recibe secretos ni contenido de documentos. */
    private fun registrar(mensaje: String) {
        Log.i(ETIQUETA, mensaje)
        salida.add(mensaje)
    }

    private fun protegido(paso: String, bloque: () -> Unit) {
        try {
            bloque()
        } catch (e: Exception) {
            registrar("$paso: ERROR ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun generarClave() = protegido("FM-01") {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!keyguard.isDeviceSecure) {
            registrar("FM-01: sin bloqueo de pantalla seguro; configure PIN, patrón o contraseña")
            return@protegido
        }
        if (claves.existe()) {
            registrar("FM-01: ya existe la clave '${AlmacenClaves.ALIAS_PREDETERMINADO}'; elimínela para regenerar")
            return@protegido
        }
        try {
            val inicio = System.nanoTime()
            claves.generar()
            val ms = (System.nanoTime() - inicio) / 1_000_000
            registrar("FM-01: clave RSA ${AlmacenClaves.TAMANO_CLAVE} generada, alias '${AlmacenClaves.ALIAS_PREDETERMINADO}', ${ms} ms")
        } catch (e: StrongBoxUnavailableException) {
            registrar("FM-03: StrongBox no disponible; no se generó ninguna clave (${e.message})")
        }
    }

    private fun consultarKeyInfo() = protegido("FM-02") {
        val info = claves.keyInfo()
        val nivel = AlmacenClaves.nombreNivelSeguridad(info.securityLevel)
        val tiposAutenticacion = buildList {
            if (info.userAuthenticationType and KeyProperties.AUTH_BIOMETRIC_STRONG != 0) add("BIOMETRIC_STRONG")
            if (info.userAuthenticationType and KeyProperties.AUTH_DEVICE_CREDENTIAL != 0) add("DEVICE_CREDENTIAL")
        }
        registrar("FM-02: securityLevel=$nivel (${if (nivel == "STRONGBOX") "CUMPLE" else "NO CUMPLE"})")
        registrar(
            "FM-02: keySize=${info.keySize}, origin=${if (info.origin == KeyProperties.ORIGIN_GENERATED) "GENERATED" else info.origin}, " +
                "purposes=${if (info.purposes == KeyProperties.PURPOSE_SIGN) "SIGN" else info.purposes}"
        )
        registrar(
            "FM-04: userAuthenticationRequired=${info.isUserAuthenticationRequired}, " +
                "timeout=${info.userAuthenticationValidityDurationSeconds}s, types=$tiposAutenticacion, " +
                "enforcedBySecureHardware=${info.isUserAuthenticationRequirementEnforcedBySecureHardware}"
        )
        val cadena = claves.cadena()
        registrar("FM-07: cadena guardada de ${cadena.size} certificado(s), emisor del titular='${cadena.first().issuerX500Principal}'")
    }

    /** FM-04: el Keystore solo permite que [alAutorizar] firme después de que el usuario se autentica. */
    private fun autorizar(paso: String, titulo: String, subtitulo: String, alAutorizar: (Signature) -> Unit) = protegido(paso) {
        val dialogo = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = protegido(paso) {
                    alAutorizar(result.cryptoObject?.signature ?: error("sin CryptoObject"))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    registrar("$paso: autenticación cancelada o fallida ($errorCode: $errString)")
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        dialogo.authenticate(info, BiometricPrompt.CryptoObject(claves.iniciarFirma()))
    }

    private fun firmarConAutenticacion() {
        val datos = DATOS_PRUEBA.toByteArray()
        autorizar("FM-04", "Autorizar firma de prueba", "SHA-256 de los datos: ${sha256Hex(datos).take(16)}…") { firma ->
            val inicio = System.nanoTime()
            firma.update(datos)
            val valor = firma.sign()
            val ms = (System.nanoTime() - inicio) / 1_000_000
            val verificada = claves.verificar(datos, valor)
            registrar("FM-04: firma tras autenticación OK, ${valor.size} bytes, ${ms} ms, verificada con la clave pública=$verificada")
        }
    }

    private fun firmarSinAutenticacion() {
        try {
            claves.iniciarFirma().run {
                update(DATOS_PRUEBA.toByteArray())
                sign()
            }
            registrar("FM-04: NO CUMPLE, la firma se produjo sin autenticación")
        } catch (e: Exception) {
            val motivo = e.message.orEmpty().substringAfter("KeyStoreException: ").substringBefore(" (")
            registrar("FM-04: firma sin autenticación rechazada (${e.javaClass.simpleName}: $motivo) CUMPLE")
        }
    }

    private fun intentarExportar() = protegido("FM-05") {
        val codificada = claves.codificacionClavePrivada()
        registrar("FM-05: PrivateKey.getEncoded() ${if (codificada == null) "= null, no exportable CUMPLE" else "devolvió datos NO CUMPLE"}")
    }

    /** FM-06: se guarda en el almacenamiento externo propio de la app para extraerla con adb. */
    private fun generarCsr() {
        autorizar("FM-06", "Autorizar solicitud de certificado", FabricaCsr.SUJETO_PREDETERMINADO.toString()) { firma ->
            val clavePublica = claves.clavePublica()
            val csr = FabricaCsr.construir(FabricaCsr.SUJETO_PREDETERMINADO, clavePublica, firma)
            val archivo = File(getExternalFilesDir(null), ARCHIVO_CSR)
            archivo.writeText(FabricaCsr.aPem(csr))
            registrar("FM-06: CSR PKCS #10 generada, subject='${csr.subject}', ${archivo.length()} bytes")
            registrar("FM-06: archivo ${archivo.absolutePath}")
            registrar("FM-06: huella SHA-256 de la clave pública ${FabricaCsr.huellaClavePublica(clavePublica)}")
        }
    }

    /** FM-07: lee la cadena PEM (titular y CA) copiada con adb al almacenamiento externo de la app. */
    private fun cargarCertificado() = protegido("FM-07") {
        val archivo = File(getExternalFilesDir(null), ARCHIVO_CADENA)
        if (!archivo.exists()) {
            registrar("FM-07: no existe ${archivo.absolutePath}")
            return@protegido
        }
        val cadena = archivo.inputStream().use { entrada ->
            CertificateFactory.getInstance("X.509").generateCertificates(entrada).map { it as X509Certificate }
        }
        val huellaCertificado = FabricaCsr.huellaClavePublica(cadena.first().publicKey)
        val huellaKeystore = FabricaCsr.huellaClavePublica(claves.clavePublica())
        registrar("FM-07: huella del certificado ${huellaCertificado}")
        registrar("FM-07: huella del Keystore    ${huellaKeystore}")
        try {
            claves.guardarCadena(cadena)
        } catch (e: Exception) {
            registrar("FM-07: certificado rechazado (${e.javaClass.simpleName}: ${e.message})")
            return@protegido
        }
        val guardado = claves.cadena().first()
        registrar("FM-07: cadena de ${cadena.size} certificados guardada en el Keystore; claves coinciden CUMPLE")
        registrar("FM-07: titular='${guardado.subjectX500Principal}', emisor='${guardado.issuerX500Principal}'")
        registrar("FM-07: X.509 v${guardado.version}, vigente del ${guardado.notBefore} al ${guardado.notAfter}")
    }

    /** PDF de una página sin datos reales, para las pruebas de I3. */
    private fun crearPdfPrueba() = protegido("I3") {
        val archivo = File(getExternalFilesDir(null), ARCHIVO_PDF_PRUEBA)
        PDDocument().use { documento ->
            val pagina = PDPage()
            documento.addPage(pagina)
            PDPageContentStream(documento, pagina).use { contenido ->
                contenido.beginText()
                contenido.setFont(PDType1Font.HELVETICA, 14f)
                contenido.newLineAtOffset(72f, 700f)
                contenido.showText("Documento de prueba - firmador movil (sin validez legal)")
                contenido.endText()
            }
            documento.save(archivo)
        }
        registrar("I3: PDF de prueba creado, ${archivo.length()} bytes, SHA-256 ${sha256Hex(archivo.readBytes())}")
    }

    /** R2: PDFBox prepara el PDF, Bouncy Castle arma el CMS y el Keystore calcula la firma. */
    private fun firmarPdfPrueba() = protegido("R2") {
        val entrada = File(getExternalFilesDir(null), ARCHIVO_PDF_PRUEBA)
        val cadena = claves.cadena()
        if (!entrada.exists() || cadena.size < 2) {
            registrar("R2: falta el PDF de prueba o el certificado de laboratorio (FM-07)")
            return@protegido
        }
        val resumen = sha256Hex(entrada.readBytes())
        autorizar("R2", "Autorizar firma del PDF", "${entrada.name} · SHA-256 ${resumen.take(16)}…") { firma ->
            val salida = File(getExternalFilesDir(null), ARCHIVO_PDF_FIRMADO)
            val inicio = System.nanoTime()
            FirmadorPdf.firmar(entrada, salida, cadena, firma)
            val ms = (System.nanoTime() - inicio) / 1_000_000
            registrar("R2: PDF firmado con la clave del Keystore, ${salida.length()} bytes, ${ms} ms")
            registrar("R2: archivo ${salida.absolutePath}")
        }
    }

    /** StrongBox solo admite RSA 2048; la solicitud debe fallar sin crear una clave en TEE. */
    private fun probarRechazoStrongBox() {
        val sonda = AlmacenClaves(ALIAS_SONDA)
        try {
            sonda.generar(tamano = 4096)
            registrar("FM-03: RSA 4096 aceptada, nivel=${AlmacenClaves.nombreNivelSeguridad(sonda.keyInfo().securityLevel)}; prueba no concluyente")
        } catch (e: Exception) {
            registrar("FM-03: rechazo informado (${e.javaClass.simpleName}); clave creada=${sonda.existe()}")
        } finally {
            if (sonda.existe()) sonda.eliminar()
        }
    }

    private fun eliminarClave() = protegido("Eliminar") {
        claves.eliminar()
        registrar("Clave '${AlmacenClaves.ALIAS_PREDETERMINADO}' eliminada")
    }

    private fun sha256Hex(datos: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(datos).joinToString("") { "%02x".format(it) }

    private companion object {
        const val ETIQUETA = "FirmadorMovil"
        const val ALIAS_SONDA = "fm03_sonda"
        const val DATOS_PRUEBA = "Datos de prueba I1 - firmador movil"
        const val ARCHIVO_CSR = "titular.csr"
        const val ARCHIVO_CADENA = "cadena.pem"
        const val ARCHIVO_PDF_PRUEBA = "prueba.pdf"
        const val ARCHIVO_PDF_FIRMADO = "prueba_firmado.pdf"
    }
}
