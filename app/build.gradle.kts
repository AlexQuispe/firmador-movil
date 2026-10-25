import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// FM-13: URL de firmador-servicio, solo en local.properties (fuera de git); nunca desde el enlace.
val servicioUrl = Properties()
    .apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load) }
    .getProperty("SERVICIO_URL").orEmpty()

// Con HTTP por IP se permite tráfico sin cifrar solo hacia ese host.
val redGenerada = layout.buildDirectory.dir("generated/res/red").get().asFile
File(redGenerada, "xml/network_security_config.xml").apply {
    parentFile.mkdirs()
    val host = runCatching { URI(servicioUrl) }.getOrNull()?.takeIf { it.scheme == "http" }?.host
    writeText(
        if (host == null) "<network-security-config />\n"
        else """
            <network-security-config>
                <domain-config cleartextTrafficPermitted="true">
                    <domain includeSubdomains="false">$host</domain>
                </domain-config>
            </network-security-config>
        """.trimIndent() + "\n"
    )
}

android {
    namespace = "bo.edu.umsa.firmadormovil"
    compileSdk = 37

    defaultConfig {
        applicationId = "bo.edu.umsa.firmadormovil"
        // API 31: KeyInfo.getSecurityLevel() (FM-02)
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SERVICIO_URL", "\"$servicioUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main").res.directories.add(redGenerada.path)
        // Utilidades comunes a las pruebas unitarias e instrumentadas
        getByName("test").kotlin.directories.add("src/pruebasComunes/java")
        getByName("androidTest").kotlin.directories.add("src/pruebasComunes/java")
    }
    packaging {
        resources {
            // Bouncy Castle jars ship the same license and OSGi manifest
            pickFirsts += "META-INF/LICENSE.md"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    // biometric 1.1.0 trae fragment 1.2.5; registerForActivityResult requiere >= 1.3.0
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.pdfbox.android) {
        // Trae Bouncy Castle 1.72 (jdk15to18), con las mismas clases que bcpkix-jdk18on
        exclude(group = "org.bouncycastle")
    }
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
