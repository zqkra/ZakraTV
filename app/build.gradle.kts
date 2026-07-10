import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Load secrets from root local.properties so RD/TMDb work without runtime setup
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String): String {
    val raw = (localProps.getProperty(name)
        ?: project.findProperty(name) as String?
        ?: "").trim()
    return if (raw.isBlank() || raw.startsWith("tu_", ignoreCase = true) || raw.startsWith("TU_")) {
        ""
    } else raw.replace("\"", "\\\"")
}

android {
    namespace = "com.zakratv.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zakratv.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "1.5.0"

        buildConfigField("String", "TMDB_API_KEY", "\"${secret("TMDB_API_KEY")}\"")
        buildConfigField("String", "REAL_DEBRID_TOKEN", "\"${secret("REAL_DEBRID_TOKEN")}\"")
        // Public GitHub repo for in-app updates (no private key)
        buildConfigField("String", "UPDATE_REPO", "\"zqkra/ZakraTV\"")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("RELEASE_STORE_FILE") as String?
            val storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            val keyAliasProp = project.findProperty("RELEASE_KEY_ALIAS") as String?
            val keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAliasProp
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
                ?.takeIf { it.storeFile?.exists() == true }
                ?: signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            // Same applicationId as release so GitHub auto-update replaces the installed app
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    // Compose for TV (lean stack)
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Media3 ExoPlayer — optimized streaming
    val media3 = "1.5.0"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("androidx.media3:media3-session:$media3")

    // Network (lightweight)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Images — Coil is memory-efficient
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Leanback banner / TV launcher + AppCompat theme base
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
