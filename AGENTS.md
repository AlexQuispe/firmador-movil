# AGENTS.md — firmador-movil

App Android (Kotlin) de prueba de concepto académica: firma PDF (PAdES B-B) en el teléfono con una clave RSA 2048 generada y usada en Android Keystore/StrongBox, no exportable, con certificado de una CA de laboratorio.

## Comandos

Requieren `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (el `java` del sistema es 15). Paquete: `bo.edu.umsa.firmadormovil`.

- `./gradlew assembleDebug` · `./gradlew installDebug` (S23 por ADB) · `./gradlew testDebugUnitTest` · `./gradlew lint`
- Instrumentadas: `./gradlew installDebug installDebugAndroidTest` y `adb shell am instrument -w bo.edu.umsa.firmadormovil.test/androidx.test.runner.AndroidJUnitRunner`. **Nunca** `connectedDebugAndroidTest` ni `adb uninstall`: desinstalan la app y destruyen la clave `firma_poc` y su certificado. Las pruebas usan su propio alias.
- Registros: `adb logcat --pid=$(adb shell pidof bo.edu.umsa.firmadormovil)`
- Versiones solo en `gradle/libs.versions.toml`. `compileSdk 37` lo exige AndroidX; `targetSdk 36` = Android 16 del dispositivo.

## Reglas no negociables

- Stack: Kotlin, `minSdk 31`, PDFBox-Android + `bcpkix`. Sin iText (AGPL). Dependencia nueva: justificar licencia.
- `setIsStrongBoxBacked(true)`; ante `StrongBoxUnavailableException` informar y no caer a TEE en silencio (FM-03).
- Autenticación impuesta por el Keystore en cada uso (`setUserAuthenticationParameters(0, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` + `BiometricPrompt` con `CryptoObject`), no solo por la UI.
- Ningún código lee ni exporta la clave privada. Nada de PIN, credenciales, tokens, claves ni contenido de PDF en logs.
- Firma siempre con confirmación explícita del usuario (nombre y SHA-256 del PDF). Sin firma desatendida.
- Fuera de alcance: certificados AGETIC reales, TSA/LTV, homologación, producción.

## Forma de trabajo

- Git: el usuario gestiona *staging* y commits. No ejecutes `git add/commit/mv/rm/stash/restore --staged`; usa `mv`/`rm`.
- Cambios mínimos y por iteración; indica qué FM-xx cubre cada cambio y cómo se verifica.
- Ante ambigüedad en un requisito, pregunta antes de implementar. No pidas confirmación para lo ya autorizado.
- Español en la comunicación y, cuando sea natural, en nombres de archivos, clases, funciones y variables; en inglés los términos de estándares o APIs (CSR, KeyInfo, PAdES, Signer). Comentarios solo donde aporten.
- Sigue los patrones del proyecto antes de añadir dependencias o soluciones paralelas.
- README e INSTALL solo cambian si quedan incorrectos o hace falta para usar o mantener una funcionalidad; breves, fieles al código y legibles de arriba abajo.

## Pruebas y cierre

- Sin pruebas nuevas mientras una funcionalidad está en desarrollo; al aprobarla el usuario, recuérdale crearlas (o créalas si lo pide o lo autoriza antes).
- Ejecuta las comprobaciones pertinentes (`testDebugUnitTest`, `lint`, prueba en el teléfono) e informa qué pasó y qué quedó sin verificar.
- Al cerrar: qué cambió y dónde, cómo se validó y limitaciones. No afirmes prueba visual, revisión experta ni ejecución en el teléfono que no ocurrieron.

## UX/UI

- La pantalla actual es un banco de pruebas; la revisión UX aplica al flujo del titular (confirmación FM-08, firma, resultado).
- Antes de cerrar un cambio visual, pide revisión al revisor UX/UI de la sesión si existe; si no, dilo y deja la limitación. Nunca inventes su aprobación.
- Cambios visuales importantes: propuesta concreta (mockup si ayuda o se pide) antes de implementar; luego verifica en el teléfono los estados relevantes (captura con `adb exec-out screencap -p`).
- Mantén Material 3 y el tema existente; estilos limitados al componente; sin imágenes ni adornos que no mejoren la experiencia.
