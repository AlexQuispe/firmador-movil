package bo.edu.umsa.firmadormovil.pruebas

import bo.edu.umsa.firmadormovil.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Rutas de la web de firmador-servicio para preparar solicitudes reales en las pruebas de FM-13.
 * Usa el servicio de SERVICIO_URL: cada prueba deja un documento de prueba en él.
 */
object ServicioPrueba {

    private const val USUARIO = "prueba-instrumentada"

    fun disponible(): Boolean = runCatching { llamar("GET", "/documentos") }.isSuccess

    /** Sube [pdf] como lo haría la web y crea una solicitud sobre él; devuelve el ID de la solicitud. */
    fun crearSolicitud(pdf: File): String {
        val documento = JSONObject(llamar("POST", "/documentos", pdf))
        return JSONObject(llamar("POST", "/documentos/${documento.getString("id")}/solicitudes")).getString("id")
    }

    fun estado(solicitud: String): String = JSONObject(llamar("GET", "/solicitudes/$solicitud")).getString("estado")

    fun documento(solicitud: String): JSONObject {
        val documentoId = JSONObject(llamar("GET", "/solicitudes/$solicitud")).getString("documentoId")
        return JSONObject(llamar("GET", "/documentos/$documentoId"))
    }

    private fun llamar(metodo: String, ruta: String, pdf: File? = null): String {
        val conexion = URL("${BuildConfig.SERVICIO_URL}/api$ruta").openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = 5_000
            conexion.setRequestProperty("X-Usuario", USUARIO)
            if (pdf != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/pdf")
                conexion.setRequestProperty("X-Nombre-Archivo", pdf.name)
                conexion.outputStream.use { salida -> pdf.inputStream().use { it.copyTo(salida) } }
            }
            check(conexion.responseCode in 200..299) { "firmador-servicio respondió ${conexion.responseCode} a $metodo $ruta" }
            return conexion.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }
}
