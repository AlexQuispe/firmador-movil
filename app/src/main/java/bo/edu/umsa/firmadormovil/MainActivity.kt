package bo.edu.umsa.firmadormovil

import android.app.KeyguardManager
import android.os.Bundle
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import bo.edu.umsa.firmadormovil.cripto.ResumenCertificado
import bo.edu.umsa.firmadormovil.documento.DocumentoPorFirmar
import bo.edu.umsa.firmadormovil.ui.PantallaConfirmacion
import bo.edu.umsa.firmadormovil.ui.PantallaInicio
import bo.edu.umsa.firmadormovil.ui.PantallaResultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.core.content.FileProvider
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

/** Flujo del titular (FM-08) y pantalla de pruebas técnicas (FM-01 a FM-07, R2). */
class MainActivity : FragmentActivity() {

    private enum class Pantalla { Inicio, Confirmacion, Resultado, Pruebas }

    private val claves by lazy { AlmacenClaves() }
    private val salida = mutableStateListOf<String>()
    private var pantalla by mutableStateOf(Pantalla.Inicio)
    private var certificado by mutableStateOf<ResumenCertificado?>(null)
    private var documento by mutableStateOf<DocumentoPorFirmar?>(null)
    private var firmado by mutableStateOf<File?>(null)
    private var preparando by mutableStateOf(false)
    private var firmando by mutableStateOf(false)
    private lateinit var elegirPdf: ActivityResultLauncher<Array<String>>
    private lateinit var guardarComo: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        elegirPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { prepararDocumento(it) }
        }
        guardarComo = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            uri?.let { guardarFirmado(it) }
        }
        actualizarCertificado()
        enableEdgeToEdge()
        setContent {
            TemaFirmadorMovil {
                Scaffold(modifier = Modifier.fillMaxSize()) { margenes ->
                    val modificador = Modifier.padding(margenes)
                    BackHandler(enabled = pantalla != Pantalla.Inicio && !firmando) { volverAlInicio() }
                    when (pantalla) {
                        Pantalla.Inicio -> PantallaInicio(
                            certificado = certificado,
                            preparando = preparando,
                            alElegirPdf = { elegirPdf.launch(arrayOf("application/pdf")) },
                            alAbrirPruebas = { pantalla = Pantalla.Pruebas },
                            modifier = modificador,
                        )
                        Pantalla.Confirmacion -> PantallaConfirmacion(
                            documento = documento!!,
                            certificado = certificado!!,
                            firmando = firmando,
                            alCancelar = ::volverAlInicio,
                            alFirmar = ::firmarDocumento,
                            modifier = modificador,
                        )
                        Pantalla.Resultado -> PantallaResultado(
                            nombre = firmado!!.name,
                            tamano = firmado!!.length(),
                            alGuardar = { guardarComo.launch(firmado!!.name) },
                            alCompartir = ::compartirFirmado,
                            alVolver = ::volverAlInicio,
                            modifier = modificador,
                        )
                        Pantalla.Pruebas -> BancoPruebas(modificador)
                    }
                }
            }
        }
    }

    private fun volverAlInicio() {
        actualizarCertificado()
        documento = null
        pantalla = Pantalla.Inicio
    }

    private fun actualizarCertificado() {
        certificado = runCatching {
            claves.cadena().takeIf { it.size >= 2 }?.first()?.let(ResumenCertificado::de)
        }.onFailure { registrar("FM-07: no se pudo leer el certificado: ${it.javaClass.simpleName}: ${it.message}") }
            .getOrNull()
    }

    /** FM-08: copia, resumen y vista previa fuera del hilo principal. */
    private fun prepararDocumento(uri: Uri) {
        preparando = true
        lifecycleScope.launch {
            try {
                val preparado = withContext(Dispatchers.IO) { DocumentoPorFirmar.preparar(this@MainActivity, uri) }
                documento = preparado
                registrar("FM-08: documento preparado, ${preparado.tamano} bytes, ${preparado.paginas} página(s), SHA-256 ${preparado.sha256}")
                pantalla = Pantalla.Confirmacion
            } catch (e: Exception) {
                avisar("No se pudo abrir el PDF", e)
            } finally {
                preparando = false
            }
        }
    }

    /** FM-08: solo se firma tras la confirmación en pantalla y la autenticación (FM-04). */
    private fun firmarDocumento() {
        val porFirmar = documento ?: return
        autorizar("FM-08", "Autorizar firma", "${porFirmar.nombre} · SHA-256 ${porFirmar.sha256.take(16)}…") { firma ->
            firmando = true
            lifecycleScope.launch {
                try {
                    val nombre = porFirmar.nombre.replace('/', '_').removeSuffix(".pdf") + "_firmado.pdf"
                    val destino = File(getExternalFilesDir(CARPETA_FIRMADOS), nombre)
                    val inicio = System.nanoTime()
                    withContext(Dispatchers.IO) { FirmadorPdf.firmar(porFirmar.archivo, destino, claves.cadena(), firma) }
                    val ms = (System.nanoTime() - inicio) / 1_000_000
                    registrar("FM-08: firmado tras confirmación y autenticación, ${destino.length()} bytes, ${ms} ms; entrada SHA-256 ${porFirmar.sha256}")
                    firmado = destino
                    pantalla = Pantalla.Resultado
                } catch (e: Exception) {
                    avisar("No se pudo firmar el documento", e)
                } finally {
                    firmando = false
                }
            }
        }
    }

    /** FM-10: copia el PDF firmado al destino que eligió el titular. */
    private fun guardarFirmado(destino: Uri) {
        val origen = firmado ?: return
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(destino).use { salida ->
                        requireNotNull(salida) { "no se pudo abrir el destino" }
                        origen.inputStream().use { it.copyTo(salida) }
                    }
                }
                registrar("FM-10: PDF firmado guardado, ${origen.length()} bytes, SHA-256 ${sha256Hex(origen.readBytes())}")
                Toast.makeText(this@MainActivity, "Documento guardado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                avisar("No se pudo guardar el documento", e)
            }
        }
    }

    /** FM-10: la otra app solo recibe permiso temporal de lectura sobre este archivo. */
    private fun compartirFirmado() {
        val archivo = firmado ?: return
        val uri = FileProvider.getUriForFile(this, "$packageName.archivos", archivo)
        val envio = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(envio, "Compartir PDF firmado"))
        registrar("FM-10: hoja de compartir abierta para el PDF firmado (${archivo.length()} bytes)")
    }

    private fun avisar(mensaje: String, e: Exception) {
        registrar("$mensaje: ${e.javaClass.simpleName}: ${e.message}")
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    @Composable
    private fun BancoPruebas(modifier: Modifier) {
        Column(
            modifier = modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = ::volverAlInicio) { Text("← Volver") }
            Text("Pruebas técnicas · I1–I3", style = MaterialTheme.typography.titleLarge)
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
        const val CARPETA_FIRMADOS = "firmados"
    }
}
