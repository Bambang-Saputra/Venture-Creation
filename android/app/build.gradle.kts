import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.spotless)
}

// local.properties tidak masuk repo. CI menulisnya dari secrets (android-ci.yml).
val lokal = Properties().apply {
    val berkas = rootProject.file("local.properties")
    if (berkas.exists()) berkas.inputStream().use { load(it) }
}

// Bawaan untuk emulator: 10.0.2.2 adalah 127.0.0.1 milik laptop (php artisan serve).
// Nilai kosong (secret CI belum diisi) juga jatuh ke bawaan ini.
val apiBaseUrl = lokal.getProperty("API_BASE_URL").orEmpty().ifBlank { "http://10.0.2.2:8000/api/" }
    .let { if (it.endsWith("/")) it else "$it/" }
val mapsApiKey = lokal.getProperty("MAPS_API_KEY").orEmpty()
// Client ID OAuth tipe Web (sama dengan GOOGLE_CLIENT_ID di api/.env). Kosong = tombol Google disembunyikan.
val googleWebClientId = lokal.getProperty("GOOGLE_WEB_CLIENT_ID").orEmpty()

android {
    namespace = "id.lifeoffoods"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "id.lifeoffoods"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        // ADR-0005: peta Google Maps hanya menyala kalau ada kunci. Tanpa kunci, K08 memakai daftar + intent geo:.
        buildConfigField("boolean", "MAPS_ENABLED", mapsApiKey.isNotBlank().toString())
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric butuh resource dan manifest aplikasi untuk menggambar layar.
            isIncludeAndroidResources = true
            all {
                // Tangkapan layar (docs/panduan/pola-layar-java.md) hanya dibuat dengan
                // -Ptangkapan, supaya android-ci tetap ringan. Hasilnya di app/build/outputs/roborazzi/.
                val tangkapan = project.hasProperty("tangkapan")
                it.systemProperty("lof.tangkapan", tangkapan.toString())
                it.systemProperty("roborazzi.test.record", tangkapan.toString())
                // Nama berkas di tes relatif ke build/outputs/roborazzi, bukan ke folder kerja.
                it.systemProperty("roborazzi.record.filePathStrategy", "relativePathFromRoborazziContextOutputDirectory")
                it.maxHeapSize = "1g"
                // Robolectric native graphics membaca FileDescriptor lewat internal JDK 17+.
                it.jvmArgs(
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
            }
        }
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(libs.versions.googleJavaFormat.get()).aosp()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)
    // Gambar jualan dari /storage API (ADR-0002).
    implementation(libs.glide)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.okhttp.mockwebserver)
}
