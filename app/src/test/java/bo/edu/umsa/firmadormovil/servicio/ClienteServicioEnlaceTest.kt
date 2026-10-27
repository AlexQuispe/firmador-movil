package bo.edu.umsa.firmadormovil.servicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** FM-13: qué URL de servicio y qué nombre de sistema se aceptan desde el enlace. */
class ClienteServicioEnlaceTest {

    @Test
    fun aceptaHttpsHaciaCualquierHostYNormalizaLaUrl() {
        assertEquals("https://firma.ejemplo.bo", ClienteServicio.servicioPermitido("https://firma.ejemplo.bo"))
        assertEquals("https://firma.ejemplo.bo", ClienteServicio.servicioPermitido("https://firma.ejemplo.bo/"))
        assertEquals("https://8.8.8.8:8443", ClienteServicio.servicioPermitido("https://8.8.8.8:8443"))
    }

    @Test
    fun aceptaHttpSoloHaciaIpv4Privadas() {
        listOf("http://10.0.0.5:3000", "http://172.16.0.1", "http://172.31.255.254:3000", "http://192.168.0.29:3000")
            .forEach { assertEquals(it, it, ClienteServicio.servicioPermitido(it)) }
    }

    @Test
    fun rechazaHttpHaciaHostsPublicosODominios() {
        listOf(
            "http://8.8.8.8:3000",
            "http://172.32.0.1",
            "http://172.15.0.1",
            "http://192.169.0.1",
            "http://firma.ejemplo.bo",
            "http://localhost:3000",
            "http://192.168.0.300",
            "http://192.168.01.1",
            "http://192.168.0",
        ).forEach { assertNull(it, ClienteServicio.servicioPermitido(it)) }
    }

    @Test
    fun rechazaUsuarioRutaConsultaOFragmento() {
        listOf(
            "https://usuario@firma.ejemplo.bo",
            "https://firma.ejemplo.bo/api",
            "https://firma.ejemplo.bo?x=1",
            "https://firma.ejemplo.bo#x",
        ).forEach { assertNull(it, ClienteServicio.servicioPermitido(it)) }
    }

    @Test
    fun rechazaOtrosEsquemasYTextoQueNoEsUrl() {
        listOf("ftp://192.168.0.29", "file:///sdcard/x.pdf", "javascript:alert(1)", "firma.ejemplo.bo", "no es url", "")
            .forEach { assertNull(it, ClienteServicio.servicioPermitido(it)) }
    }

    @Test
    fun conservaElNombreDelSistemaConTildes() {
        assertEquals("Gestión documental de ejemplo", ClienteServicio.nombreSistema("  Gestión documental de ejemplo "))
    }

    @Test
    fun dejaElNombreEnUnaSolaLinea() {
        assertEquals("Gestión documental", ClienteServicio.nombreSistema("Gestión\ndocumental"))
        assertEquals("Gestión documental", ClienteServicio.nombreSistema("Gestión\t\r\n  documental"))
    }

    @Test
    fun quitaLosCaracteresInvisiblesDeFormato() {
        assertEquals("Sistemafdp.exe", ClienteServicio.nombreSistema("Sistema‮fdp.exe"))
        assertEquals("AGETIC", ClienteServicio.nombreSistema("AGE​TIC"))
    }

    @Test
    fun limitaElNombreA60Caracteres() {
        assertEquals("a".repeat(60), ClienteServicio.nombreSistema("a".repeat(100)))
    }

    @Test
    fun unNombreVacioNoSeMuestra() {
        listOf("", "   ", "\n\t", "‮").forEach { assertNull(ClienteServicio.nombreSistema(it)) }
    }
}
