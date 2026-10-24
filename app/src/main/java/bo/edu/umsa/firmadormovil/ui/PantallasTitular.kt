package bo.edu.umsa.firmadormovil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import bo.edu.umsa.firmadormovil.cripto.ResumenCertificado
import bo.edu.umsa.firmadormovil.documento.DocumentoPorFirmar
import java.text.DateFormat

@Composable
fun PantallaInicio(
    certificado: ResumenCertificado?,
    preparando: Boolean,
    alElegirPdf: () -> Unit,
    alAbrirPruebas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Firmador móvil", style = MaterialTheme.typography.headlineSmall)
        if (certificado != null) {
            DatoEtiquetado("Certificado", "${certificado.titular}\n${certificado.emisor} · hasta ${fecha(certificado)}")
        } else {
            Text(
                "No hay un certificado cargado. Cárguelo desde Pruebas técnicas (FM-07).",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = alElegirPdf,
            enabled = certificado != null && !preparando,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (preparando) "Preparando documento…" else "Elegir PDF para firmar") }
        TextButton(onClick = alAbrirPruebas) { Text("Pruebas técnicas") }
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
) {
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Confirmar firma", style = MaterialTheme.typography.headlineSmall)
            Image(
                bitmap = documento.vistaPrevia.asImageBitmap(),
                contentDescription = "Vista previa de la primera página",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
            )
            Text(
                "Página 1 de ${documento.paginas}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            DatoEtiquetado("Documento", "${documento.nombre} · ${tamanoLegible(documento.tamano)}")
            DatoEtiquetado("SHA-256", documento.sha256.chunked(4).joinToString(" "), monoespaciado = true)
            DatoEtiquetado("Firmará como", "${certificado.titular}\n${certificado.emisor} · hasta ${fecha(certificado)}")
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = alCancelar, enabled = !firmando, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = alFirmar, enabled = !firmando, modifier = Modifier.weight(1f)) {
                if (firmando) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Firmar")
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
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Documento firmado", style = MaterialTheme.typography.headlineSmall)
        DatoEtiquetado("Archivo", "$nombre · ${tamanoLegible(tamano)}")
        Spacer(Modifier.height(8.dp))
        Button(onClick = alGuardar, modifier = Modifier.fillMaxWidth()) { Text("Guardar en…") }
        OutlinedButton(onClick = alCompartir, modifier = Modifier.fillMaxWidth()) { Text("Compartir") }
        TextButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al inicio") }
    }
}

@Composable
private fun DatoEtiquetado(etiqueta: String, valor: String, monoespaciado: Boolean = false) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontFamily = if (monoespaciado) FontFamily.Monospace else null)
    }
}

private fun fecha(certificado: ResumenCertificado): String =
    DateFormat.getDateInstance(DateFormat.SHORT).format(certificado.vigenteHasta)

private fun tamanoLegible(bytes: Long): String =
    if (bytes < 1024) "$bytes B" else "${(bytes + 512) / 1024} KB"
