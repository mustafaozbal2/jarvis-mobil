plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val calistirma = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.mustafa.jarvis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mustafa.jarvis"
        minSdk = 26
        targetSdk = 35
        versionCode = calistirma
        versionName = "14.0.$calistirma"
    }

    // Her derlemede AYNI anahtarla imzalanır: telefona yeni sürüm kurulunca eskisinin üzerine yazılır (kaldırmak gerekmez).
    // Depo herkese açık: anahtar dosyası güçlü şifreyle kilitli, şifre YALNIZCA GitHub gizli değerinde
    // (Settings → Secrets and variables → Actions → JARVIS_KEYSTORE_SIFRE). Şifresiz dosya işe yaramaz.
    val ksSifre = System.getenv("JARVIS_KEYSTORE_SIFRE").orEmpty()
    signingConfigs {
        create("jarvis") {
            storeFile = file("jarvis.keystore")
            storePassword = ksSifre
            keyAlias = "jarvis"
            keyPassword = ksSifre
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("jarvis")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("jarvis")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
