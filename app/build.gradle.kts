plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.prayertime.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.prayertime.tv"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Fixed key so every build has the same signature (updates install over older builds).
    // This is a public, non-secret key for sideloading only - not for Google Play.
    signingConfigs {
        getByName("debug") {
            storeFile = file("prayer-debug.jks")
            storePassword = "prayertime"
            keyAlias = "prayertime"
            keyPassword = "prayertime"
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
