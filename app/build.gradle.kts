plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
}
