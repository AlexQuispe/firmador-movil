package bo.edu.umsa.firmadormovil.servicio

import android.content.Intent
import bo.edu.umsa.firmadormovil.BuildConfig
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Llamadas del móvil a firmador-servicio (FM-13). La solicitud se identifica solo por su ID;
 * la URL del servicio es la configurada en la app. Hacen E/S: llamar fuera del hilo principal.
 */
object ClienteServicio {

    class Solicitud(val nombre: String, val sha256: String)

    private val ID_SOLICITUD = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")

    /** ID de `firmadormovil://firmar/{id}`, o null si el intent no es un enlace de firma. */
    fun idSolicitud(intent: Intent): String? =
        intent.data?.takeIf { it.scheme == "firmadormovil" && it.host == "firmar" }
            ?.lastPathSegment?.takeIf(ID_SOLICITUD::matches)

    /** PENDIENTE → EN_PROCESO; devuelve el nombre y el SHA-256 del PDF a firmar. */
    fun iniciar(id: String): Solicitud = llamar(id, "iniciar", "POST") { entrada ->
        val json = JSONObject(entrada.bufferedReader().readText())
        Solicitud(json.getString("nombre"), json.getString("sha256"))
    }

    fun descargar(id: String, destino: File) = llamar(id, "documento", "GET") { entrada ->
        destino.outputStream().use { entrada.copyTo(it) }
    }

    fun entregar(id: String, firmado: File) = llamar(id, "resultado", "POST", firmado) {}

    fun rechazar(id: String) = llamar(id, "rechazo", "POST") {}

    private fun <T> llamar(id: String, accion: String, metodo: String, cuerpo: File? = null, leer: (InputStream) -> T): T {
        val conexion = URL("${BuildConfig.SERVICIO_URL}/api/solicitudes/$id/$accion").openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 30_000
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/pdf")
                conexion.setFixedLengthStreamingMode(cuerpo.length())
                conexion.outputStream.use { salida -> cuerpo.inputStream().use { it.copyTo(salida) } }
            }
            check(conexion.responseCode in 200..299) { "firmador-servicio respondió ${conexion.responseCode}" }
            return conexion.inputStream.use(leer)
        } finally {
            conexion.disconnect()
        }
    }
}
