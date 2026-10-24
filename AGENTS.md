# AGENTS.md — firmador-movil

App Android (Kotlin) de prueba de concepto académica: firma PDF (PAdES B-B) en el teléfono con una clave RSA 2048 generada y usada en Android Keystore/StrongBox, no exportable, con certificado de una CA de laboratorio.

## Fuente de verdad

- Requisitos FM-xx, arquitectura y riesgos: `docs/ESPECIFICACIONES_TECNICAS.md`. Léela por secciones según la tarea; no la copies aquí ni la reformules en el código.
- Contexto de la monografía: `../../AGENTS.md`, `../../docs/prueba_de_concepto.md`, `../../docs/indicadores.md`. Consúltalos solo si la tarea toca evaluación o redacción.
- Contrato P2: `../firmador-servicio/docs/ESPECIFICACIONES_TECNICAS.md`, solo en I5.

## Prioridad y orden

P1 = I0–I4 (FM-01 a FM-10), en orden. P2 (I5: FM-11 a FM-13) solo con P1 cerrado. Al iniciar I3, probar primero la firma delegada PDFBox + Bouncy Castle → Keystore (riesgo R2).

## Comandos

Requieren `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (el `java` del sistema es 15). Paquete: `bo.edu.umsa.firmadormovil`.

- `./gradlew assembleDebug` · `./gradlew installDebug` (S23 por ADB) · `./gradlew testDebugUnitTest` · `./gradlew lint`
- Registros: `adb logcat --pid=$(adb shell pidof bo.edu.umsa.firmadormovil)`
- Versiones solo en `gradle/libs.versions.toml`. `compileSdk 37` lo exige AndroidX; `targetSdk 36` = Android 16 del dispositivo.

## Reglas no negociables

- Stack: Kotlin, `minSdk 31`, PDFBox-Android + `bcpkix`. Sin iText (AGPL). Dependencia nueva: justificar licencia.
- `setIsStrongBoxBacked(true)`; ante `StrongBoxUnavailableException` informar y no caer a TEE en silencio (FM-03).
- Autenticación impuesta por el Keystore en cada uso (`setUserAuthenticationParameters(0, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` + `BiometricPrompt` con `CryptoObject`), no solo por la UI.
- Ningún código lee ni exporta la clave privada. Nada de PIN, credenciales, tokens, claves ni contenido de PDF en logs.
- Firma siempre con confirmación explícita del usuario (nombre y SHA-256 del PDF). Sin firma desatendida.
- Fuera de alcance: certificados AGETIC reales, TSA/LTV, homologación, producción.

## Evidencia

Cada ejecución de prueba se registra en `../../evidencias/I<n>_<nombre>/` con los campos de la §7 de la especificación. Ejecuciones previas a la definición de indicadores son de desarrollo, no resultados. No afirmar en código, commits ni docs equivalencia con ePass2003, homologación ni compatibilidad institucional.

## Forma de trabajo

- Git: el usuario gestiona *staging* y commits. No ejecutes `git add/commit/mv/rm/stash/restore --staged`; usa `mv`/`rm`.
- Cambios mínimos y por iteración; indica qué FM-xx cubre cada cambio y cómo se verifica.
- Ante ambigüedad en un requisito, pregunta antes de implementar.
- Respuestas breves en español; código y nombres en inglés, comentarios solo donde aporten.
