import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Release signing. Secrets live in keystore.properties at the repo root (gitignored), so the
// keystore password/alias never enter version control. When the file is absent (CI without
// secrets, fresh clone) the release build simply falls back to unsigned — debug is unaffected.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

android {
    namespace = "net.packsi.tunnels"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.packsi.tunnels"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }

        // Backend API base URL. Emulator reaches the host machine via 10.0.2.2
        // (not "localhost"). Override per build type / on a real device as needed.
        buildConfigField("String", "BASE_URL", "\"https://core.packsi.net\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.play.services.ads)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // AmneziaWG tunnel is VENDORED, not pulled from a repo: the lib is published nowhere
    // (not Maven Central; JitPack can't build its Go native). So its Java sources live under
    // app/src/main/java/org/amnezia/awg and the prebuilt native libs (libwg-go/libwg/libwg-quick,
    // extracted from the official amneziawg APK) live under app/src/main/jniLibs. These are the
    // tunnel module's only external compile deps. androidx.annotation + androidx.collection are
    // already on the app classpath transitively (compose/core), so only jsr305 (compile-time
    // nullness meta-annotations in NonNullForAll) must be added; it is runtime-irrelevant.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    debugImplementation(libs.androidx.ui.tooling)
}
