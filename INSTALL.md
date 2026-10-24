# 🛠️ Manual de instalación

## 1. 📋 Requerimientos

| Nombre                   | Versión       | Descripción                                                     | Instalación                                        |
| ------------------------ | ------------- | --------------------------------------------------------------- | -------------------------------------------------- |
| `JDK`                    | ^21 (mín. 17) | Entorno Java para Gradle y Android Gradle Plugin.               | `sudo apt install openjdk-21-jdk`                  |
| `Android SDK`            | cmdline-tools | Herramientas de compilación de Android.                         | https://developer.android.com/studio#command-tools |
| `platform-tools`         | ^36           | Incluye `adb` para instalar en el teléfono.                     | `sdkmanager "platform-tools"`                      |
| `platforms;android-37.0` | 37            | API contra la que se compila (`compileSdk`).                    | `sdkmanager "platforms;android-37.0"`              |
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
# Ubicación del Android SDK (no se versiona)
echo "sdk.dir=$ANDROID_HOME" > local.properties
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

# Análisis estático
./gradlew lint
```

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
