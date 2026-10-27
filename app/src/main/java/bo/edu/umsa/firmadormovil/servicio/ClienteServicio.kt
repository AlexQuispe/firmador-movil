package bo.edu.umsa.firmadormovil.servicio

import android.content.Intent
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Llamadas del móvil al servicio que emitió la solicitud (FM-13). El enlace trae el ID y la URL
 * base del servicio; la app no está atada a un único backend. Hacen E/S: llamar fuera del hilo principal.
 */
object ClienteServicio {

    /**
     * Solicitud abierta desde un enlace: su ID, la URL base del servicio que la emitió y, si viene,
     * el nombre del sistema. El nombre lo declara el solicitante y no se verifica; el host sí es el contactado.
     */
    class Enlace(val id: String, val servicio: String, val sistema: String? = null) {
        /** Host (y puerto) del servicio, para que el titular vea quién pide la firma. */
        val origen: String get() = URI(servicio).authority
    }

    class Solicitud(val nombre: String, val sha256: String)

    private val ID_SOLICITUD = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")

    /** `firmadormovil://firmar/{id}?servicio={url}[&sistema={nombre}]`, o null si no es un enlace de firma válido. */
    fun enlace(intent: Intent): Enlace? {
        val datos = intent.data?.takeIf { it.scheme == "firmadormovil" && it.host == "firmar" } ?: return null
        val id = datos.lastPathSegment?.takeIf(ID_SOLICITUD::matches) ?: return null
        val servicio = datos.getQueryParameter("servicio")?.let(::servicioPermitido) ?: return null
        return Enlace(id, servicio, datos.getQueryParameter("sistema")?.let(::nombreSistema))
    }

    /**
     * Texto plano de una línea y hasta 60 caracteres; null si queda vacío. Quita los caracteres de formato
     * invisibles (p. ej. U+202E, que invierte el texto) y convierte saltos de línea y controles en espacios.
     */
    fun nombreSistema(texto: String): String? =
        texto.replace(FORMATO, "").replace(ESPACIOS, " ").trim().take(60).trim().ifEmpty { null }

    private val FORMATO = Regex("\\p{Cf}")
    private val ESPACIOS = Regex("[\\s\\x00-\\x1F\\x7F-\\x9F]+")

    /**
     * URL base normalizada (`esquema://host[:puerto]`) si se admite: HTTPS hacia cualquier host, o HTTP
     * solo hacia una IPv4 privada (red local del laboratorio). Sin usuario, ruta, consulta ni fragmento.
     */
    fun servicioPermitido(texto: String): String? {
        val uri = runCatching { URI(texto) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
        if (!uri.rawPath.isNullOrEmpty() && uri.rawPath != "/") return null
        val admitido = when (uri.scheme) {
            "https" -> true
            "http" -> esIpv4Privada(host)
            else -> false
        }
        if (!admitido) return null
        return "${uri.scheme}://$host" + if (uri.port != -1) ":${uri.port}" else ""
    }

    private fun esIpv4Privada(host: String): Boolean {
        val partes = host.split(".")
        if (partes.size != 4 || partes.any { !it.matches(Regex("0|[1-9][0-9]{0,2}")) }) return false
        val octetos = partes.map(String::toInt)
        if (octetos.any { it > 255 }) return false
        val (a, b) = octetos
        return a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168)
    }

    /** PENDIENTE → EN_PROCESO; devuelve el nombre y el SHA-256 del PDF a firmar. */
    fun iniciar(enlace: Enlace): Solicitud = llamar(enlace, "iniciar", "POST") { entrada ->
        val json = JSONObject(entrada.bufferedReader().readText())
        Solicitud(json.getString("nombre"), json.getString("sha256"))
    }

    fun descargar(enlace: Enlace, destino: File) = llamar(enlace, "documento", "GET") { entrada ->
        destino.outputStream().use { entrada.copyTo(it) }
    }

    fun entregar(enlace: Enlace, firmado: File) = llamar(enlace, "resultado", "POST", firmado) {}

    fun rechazar(enlace: Enlace) = llamar(enlace, "rechazo", "POST") {}

    private fun <T> llamar(enlace: Enlace, accion: String, metodo: String, cuerpo: File? = null, leer: (InputStream) -> T): T {
        val conexion = URL("${enlace.servicio}/api/solicitudes/${enlace.id}/$accion").openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 30_000
            // Una redirección podría llevar el PDF firmado a otro host distinto del mostrado al titular.
            conexion.instanceFollowRedirects = false
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/pdf")
                conexion.setFixedLengthStreamingMode(cuerpo.length())
                conexion.outputStream.use { salida -> cuerpo.inputStream().use { it.copyTo(salida) } }
            }
            check(conexion.responseCode in 200..299) { "el servicio respondió ${conexion.responseCode}" }
            return conexion.inputStream.use(leer)
        } finally {
            conexion.disconnect()
        }
    }
}
