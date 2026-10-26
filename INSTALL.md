# 🛠️ Manual de instalación

## 1. 📋 Requerimientos

| Nombre                   | Versión       | Descripción                                                     | Instalación                                        |
| ------------------------ | ------------- | --------------------------------------------------------------- | -------------------------------------------------- |
| `JDK`                    | ^21 (mín. 17) | Entorno Java para Gradle y Android Gradle Plugin.               | `sudo apt install openjdk-21-jdk`                  |
| `Android SDK`            | cmdline-tools | Herramientas de compilación de Android.                         | https://developer.android.com/studio#command-tools |
| `platform-tools`         | ^36           | Incluye `adb` para instalar en el teléfono.                     | `sdkmanager "platform-tools"`                      |
| `platforms;android-37.0` | 37            | API contra la que se compila (`compileSdk`).                    | `sdkmanager "platforms;android-37.0"`              |
| `OpenSSL`                | ^3.0          | CA de laboratorio que emite el certificado de prueba.           | `sudo apt install openssl`                         |
| Teléfono Android         | ^12 (API 31)  | Con StrongBox y bloqueo de pantalla (PIN, patrón o contraseña). | Probado en Samsung Galaxy S23 Ultra, Android 16    |

Gradle no se instala aparte: el proyecto incluye `./gradlew` (Gradle 9.8.2).

## 2. ⚙️ Instalación

### Variables de entorno

```bash
# JDK 21 (el java por defecto del sistema puede ser anterior)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Android SDK
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Aceptar licencias del SDK
yes | sdkmanager --licenses
```

### Clonación del proyecto

```bash
# Clonación del proyecto
git clone https://gitlab.softwarelibre.gob.bo/wilmer.quispe/firmador-movil.git

# Ingresamos dentro de la carpeta del proyecto
cd firmador-movil

# Cambiamos a la rama develop
git checkout develop
```

### Archivos de configuración

```bash
# No se versiona
cp local.properties.sample local.properties
```

En `local.properties`, ajustar `sdk.dir` a la ubicación del Android SDK y `SERVICIO_URL` a la URL de `firmador-servicio`, que solo hace falta para atender solicitudes desde la web (P2). Con `http://` la app permite tráfico sin cifrar solo hacia ese host; con un dominio, usar `https://`. Tras cambiar `SERVICIO_URL`, volver a instalar la app. Sin ella, la firma local funciona igual.

### 🔏 CA de laboratorio

El certificado del titular lo emite una CA local de prueba, **sin validez legal**: los validadores reconocerán la integridad de la firma, pero no la confianza en el emisor. Todo lo que se genera en `ca-laboratorio/` queda fuera de git; solo se versiona `openssl.cnf.sample`.

```bash
cd ca-laboratorio
cp openssl.cnf.sample openssl.cnf

# Clave y certificado raíz de la CA (válido 2 años)
openssl req -x509 -new -newkey rsa:3072 -noenc -keyout ca.key -out ca.crt \
  -days 730 -sha256 -config openssl.cnf
```

Para emitir el certificado del titular, primero generar la CSR en la app (**Generar CSR**) y luego, dentro de `ca-laboratorio/`:

```bash
# Traer la CSR desde el teléfono
adb pull /sdcard/Android/data/bo.edu.umsa.firmadormovil/files/titular.csr

# Verificar la firma de la CSR
openssl req -in titular.csr -noout -verify

# Emitir el certificado X.509 v3 (válido 1 año)
openssl x509 -req -in titular.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out titular.crt -days 365 -sha256 -extfile openssl.cnf -extensions ext_titular

# Comprobar la cadena
openssl verify -CAfile ca.crt titular.crt

# Enviar la cadena (titular y CA) al teléfono
cat titular.crt ca.crt > cadena.pem
adb push cadena.pem /sdcard/Android/data/bo.edu.umsa.firmadormovil/files/
```

Luego, en la app, pulsar **Cargar certificado**: se guarda solo si su clave pública coincide con la de StrongBox.

⚠️ `ca.key` es la clave privada de la CA: no se comparte ni se sube al repositorio.

#### Jerarquía con el perfil de la ECA (I6)

`ca-laboratorio-i6/` arma una raíz y una CA emisora de laboratorio, y emite el certificado del titular con el perfil de persona natural de la ECA AGETIC (políticas, CRL, AIA, usos y SAN). Tampoco tiene validez legal.

```bash
cd ca-laboratorio-i6
./ca-i6.sh crear                  # raíz y CA emisora, una sola vez
adb pull /sdcard/Android/data/bo.edu.umsa.firmadormovil/files/titular.csr
./ca-i6.sh emitir titular.csr     # titular.pem, cadena.pem (titular, CA y raíz) y CRL
adb push cadena.pem /sdcard/Android/data/bo.edu.umsa.firmadormovil/files/
./ca-i6.sh servir                 # publica raíz, CA y CRL en http://localhost:8099 para los validadores del equipo
```

### Preparación del teléfono

1. 📱 **Ajustes → Acerca del teléfono → Información de software**: tocar 7 veces **Número de compilación**.
2. 🧑‍💻 **Opciones de desarrollador**: activar **Depuración USB**.
3. 🔌 Conectar por USB y aceptar **Permitir depuración USB**.

```bash
# Debe mostrar el dispositivo con estado "device"
adb devices

# Debe mostrar feature:android.hardware.strongbox_keystore
adb shell pm list features | grep strongbox_keystore
```

### Despliegue de la aplicación

```bash
# Compila e instala en el teléfono (modo depuración)
./gradlew installDebug

# Abre la aplicación
adb shell am start -n bo.edu.umsa.firmadormovil/.MainActivity

# Solo genera el APK en app/build/outputs/apk/debug/
./gradlew assembleDebug
```

### Ejecución de pruebas

```bash
# Pruebas unitarias
./gradlew testDebugUnitTest

# Pruebas instrumentadas en el teléfono (StrongBox, firma de PDF, pantallas)
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w bo.edu.umsa.firmadormovil.test/androidx.test.runner.AndroidJUnitRunner

# Análisis estático
./gradlew lint
```

⚠️ No usar `./gradlew connectedDebugAndroidTest`: desinstala la app al terminar y con ello elimina la clave y su certificado. Las pruebas instrumentadas usan su propio alias y no tocan la clave de trabajo.

### ✅ Verificación

En la aplicación, pulsar **Generar clave (StrongBox)** y luego **Consultar KeyInfo**. Debe mostrarse:

```
FM-02: securityLevel=STRONGBOX (CUMPLE)
```

```bash
# Registros de la aplicación
adb logcat -s FirmadorMovil
```

### Desinstalación

```bash
adb uninstall bo.edu.umsa.firmadormovil
```

⚠️ Al desinstalar se elimina la clave privada sin posibilidad de recuperarla: la clave no sale del teléfono por diseño.
