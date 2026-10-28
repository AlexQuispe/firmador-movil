package bo.edu.umsa.firmadormovil.ui

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bo.edu.umsa.firmadormovil.cripto.AlmacenClaves
import bo.edu.umsa.firmadormovil.cripto.ResumenCertificado
import bo.edu.umsa.firmadormovil.documento.DocumentoPorFirmar
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun PantallaInicio(
    certificado: ResumenCertificado?,
    preparando: Boolean,
    alElegirPdf: () -> Unit,
    alAbrirPruebas: () -> Unit,
    modifier: Modifier = Modifier,
    nivelSeguridad: String? = null,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Iconos.Escudo, null, Modifier.size(22.dp)) }
                }
                Text("Firmador móvil", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = alAbrirPruebas) {
                    Icon(Iconos.Matraz, "Pruebas técnicas", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Seccion("Su certificado") {
                if (certificado != null) TarjetaCertificado(certificado, nivelSeguridad) else SinCertificado(alAbrirPruebas)
            }
            if (certificado != null) Seccion("Cómo se firma") { PasosFirma() }
        }
        BotonPrincipal(
            texto = if (preparando) "Preparando documento…" else "Elegir PDF para firmar",
            icono = Iconos.DocumentoNuevo,
            alPulsar = alElegirPdf,
            habilitado = certificado != null && !preparando,
            cargando = preparando,
        )
        if (certificado == null) {
            Text(
                "Disponible cuando haya un certificado cargado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

/** FM-08: el titular ve el documento, su SHA-256 y el certificado antes de autenticarse. */
@Composable
fun PantallaConfirmacion(
    documento: DocumentoPorFirmar,
    certificado: ResumenCertificado,
    firmando: Boolean,
    alCancelar: () -> Unit,
    alFirmar: () -> Unit,
    modifier: Modifier = Modifier,
    solicitadoPor: String? = null,
    sistema: String? = null,
) {
    val desdeServicio = solicitadoPor != null
    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alCancelar, enabled = !firmando) {
                if (desdeServicio) Icon(Iconos.Cerrar, "Rechazar solicitud") else Icon(Iconos.Atras, "Volver")
            }
            Text(
                if (desdeServicio) "Solicitud de firma" else "Confirmar firma",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (solicitadoPor != null) AvisoServicio(solicitadoPor, sistema)
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(
                        bitmap = documento.vistaPrevia.asImageBitmap(),
                        contentDescription = "Vista previa de la primera página",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)),
                    )
                    Text(
                        "Página 1 de ${documento.paginas}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilaDocumento(Iconos.Documento, documento.nombre, "${paginas(documento.paginas)} · ${tamanoLegible(documento.tamano)}")
            BloqueSha256(documento.sha256, coincideConSolicitud = desdeServicio)
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = Color.Transparent,
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Etiqueta("Firmará como")
                    Text(certificado.titular, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${certificado.emisor} · hasta el ${fecha(certificado.vigenteHasta)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Iconos.Candado, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Se pedirá su huella o PIN para usar la clave.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = alCancelar,
                    enabled = !firmando,
                    colors = if (desdeServicio) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text(if (desdeServicio) "Rechazar" else "Cancelar", style = MaterialTheme.typography.titleSmall) }
                Button(onClick = alFirmar, enabled = !firmando, modifier = Modifier.weight(if (desdeServicio) 1.3f else 1f).height(56.dp)) {
                    if (firmando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    } else {
                        Icon(Iconos.Huella, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(10.dp))
                        Text(if (desdeServicio) "Firmar y enviar" else "Firmar", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

/** FM-10: el PDF firmado se guarda donde elija el titular o se comparte con otra app. */
@Composable
fun PantallaResultado(
    nombre: String,
    tamano: Long,
    alGuardar: () -> Unit,
    alCompartir: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    firmante: String? = null,
    firmadoEn: Date? = null,
) {
    PantallaDesenlace(
        icono = Iconos.Escudo,
        fondoIcono = MaterialTheme.colorScheme.secondaryContainer,
        colorIcono = MaterialTheme.colorScheme.secondary,
        titulo = "Documento firmado",
        mensaje = "La firma se generó en este teléfono, con una clave que no sale del Keystore.",
        modifier = modifier,
        contenido = {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FilaDocumento(Iconos.DocumentoFirmado, nombre, tamanoLegible(tamano))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DatoEtiquetado("Formato", "PAdES B-B")
                            DatoEtiquetado("Algoritmo", "RSA ${AlmacenClaves.TAMANO_CLAVE} · SHA-256")
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            firmante?.let { DatoEtiquetado("Firmante", it) }
                            firmadoEn?.let { DatoEtiquetado("Hora del teléfono", fechaHora(it)) }
                        }
                    }
                }
            }
            Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Iconos.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Puede verificar la firma con un validador de PDF, como Jacobitus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        acciones = {
            BotonPrincipal("Guardar en…", Iconos.Descargar, alGuardar)
            OutlinedButton(onClick = alCompartir, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Iconos.Compartir, null, Modifier.size(20.dp))
                Spacer(Modifier.size(10.dp))
                Text("Compartir", style = MaterialTheme.typography.titleSmall)
            }
            TextButton(onClick = alVolver, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Volver al inicio") }
        },
    )
}

/** La firma no terminó: el original queda igual y el titular decide si reintenta. */
@Composable
fun PantallaError(
    mensaje: String,
    detalle: String,
    nombre: String?,
    textoVolver: String,
    alReintentar: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaDesenlace(
        icono = Iconos.Alerta,
        fondoIcono = MaterialTheme.colorScheme.errorContainer,
        colorIcono = MaterialTheme.colorScheme.error,
        titulo = "No se pudo firmar",
        mensaje = mensaje,
        modifier = modifier,
        contenido = {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    nombre?.let {
                        FilaDocumento(Iconos.Documento, it, "Sin firmar")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Etiqueta("Detalle técnico")
                    Text(detalle, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        acciones = {
            BotonPrincipal("Intentar de nuevo", Iconos.Reintentar, alReintentar)
            TextButton(onClick = alVolver, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(textoVolver) }
        },
    )
}

@Composable
private fun PantallaDesenlace(
    icono: ImageVector,
    fondoIcono: Color,
    colorIcono: Color,
    titulo: String,
    mensaje: String,
    modifier: Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
    acciones: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = CircleShape, color = fondoIcono, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icono, null, Modifier.size(44.dp), tint = colorIcono) }
                }
                Text(titulo, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    mensaje,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }
            contenido()
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = acciones)
    }
}

@Composable
private fun TarjetaCertificado(certificado: ResumenCertificado, nivelSeguridad: String?) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Iconos.Persona, null, Modifier.size(22.dp)) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(certificado.titular, style = MaterialTheme.typography.titleMedium)
                    Text(certificado.emisor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Vigente hasta el ${fecha(certificado.vigenteHasta)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Solo se afirma StrongBox si KeyInfo lo informa (FM-02).
                when (nivelSeguridad) {
                    null -> Unit
                    "STRONGBOX" -> Insignia(Iconos.Candado, "Clave en StrongBox", MaterialTheme.colorScheme.secondaryContainer)
                    else -> Insignia(Iconos.Alerta, "Clave sin StrongBox", MaterialTheme.colorScheme.errorContainer)
                }
                if (certificado.emisor.contains("sin validez legal", ignoreCase = true)) {
                    Insignia(Iconos.Matraz, "Laboratorio", MaterialTheme.colorScheme.tertiaryContainer)
                }
            }
        }
    }
}

@Composable
private fun SinCertificado(alAbrirPruebas: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Iconos.Llave, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.tertiary) }
        }
        Text("Aún no hay un certificado", style = MaterialTheme.typography.titleMedium)
        Text(
            "Genere la clave en StrongBox y cargue el certificado de laboratorio desde Pruebas técnicas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = alAbrirPruebas, modifier = Modifier.height(48.dp)) {
            Text("Ir a Pruebas técnicas")
            Spacer(Modifier.size(8.dp))
            Icon(Iconos.Siguiente, null, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PasosFirma() {
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Paso(1, "Elija un PDF", "Desde el teléfono, Drive o una descarga.")
        Paso(2, "Revise el documento", "Vista previa, SHA-256 y certificado.")
        Paso(3, "Autorice con huella o PIN", "La clave firma sin salir del teléfono.")
    }
}

@Composable
private fun Paso(numero: Int, titulo: String, detalle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(28.dp).border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("$numero", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Column {
            Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AvisoServicio(origen: String, sistema: String?) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        val secundario = LocalContentColor.current.copy(alpha = 0.75f)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Iconos.Enlace, null, Modifier.size(20.dp)) }
                }
                // El nombre lo declara el solicitante; el host es el servicio al que se conecta la app.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "SOLICITADA POR",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.7.sp),
                        color = secundario,
                    )
                    if (sistema != null) {
                        Text(sistema, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(origen, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = secundario)
                    } else {
                        Text(origen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.12f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Iconos.Info, null, Modifier.size(16.dp), tint = secundario)
                Text(
                    "Al firmar, el documento se envía a este sistema y usted vuelve al navegador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = secundario,
                )
            }
        }
    }
}

@Composable
private fun BloqueSha256(sha256: String, coincideConSolicitud: Boolean) {
    val portapapeles = LocalClipboard.current
    val alcance = rememberCoroutineScope()
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etiqueta("SHA-256 del documento", Modifier.weight(1f))
                IconButton(onClick = {
                    alcance.launch { portapapeles.setClipEntry(ClipEntry(ClipData.newPlainText("SHA-256", sha256))) }
                }) { Icon(Iconos.Copiar, "Copiar SHA-256", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(
                sha256.chunked(4).joinToString(" "),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 12.dp),
            )
            // El SHA-256 ya se comparó con el de la solicitud antes de mostrar esta pantalla (FM-13).
            if (coincideConSolicitud) {
                Insignia(Iconos.Hecho, "Coincide con la solicitud", MaterialTheme.colorScheme.secondaryContainer, Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun BotonPrincipal(
    texto: String,
    icono: ImageVector,
    alPulsar: () -> Unit,
    habilitado: Boolean = true,
    cargando: Boolean = false,
) {
    Button(onClick = alPulsar, enabled = habilitado, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Icon(icono, null, Modifier.size(20.dp))
        }
        Spacer(Modifier.size(10.dp))
        Text(texto, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Insignia(icono: ImageVector, texto: String, fondo: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = fondo, modifier = modifier) {
        Row(
            Modifier.height(32.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icono, null, Modifier.size(16.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FilaDocumento(icono: ImageVector, nombre: String, detalle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icono, null, Modifier.size(20.dp)) }
        }
        Column {
            Text(nombre, style = MaterialTheme.typography.titleMedium)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Etiqueta(titulo)
        contenido()
    }
}

@Composable
private fun Etiqueta(texto: String, modifier: Modifier = Modifier) {
    Text(texto.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.7.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
private fun DatoEtiquetado(etiqueta: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Etiqueta(etiqueta)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun paginas(n: Int) = if (n == 1) "1 página" else "$n páginas"

private fun fecha(dia: Date): String = DateFormat.getDateInstance(DateFormat.SHORT).format(dia)

private fun fechaHora(momento: Date): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(momento)

private fun tamanoLegible(bytes: Long): String =
    if (bytes < 1024) "$bytes B" else "${(bytes + 512) / 1024} KB"
