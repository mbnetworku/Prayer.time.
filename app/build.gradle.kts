plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.prayertime.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.prayertime.tv"
        minSdk = 21 // Android 5.0 - the first Android TV version
        targetSdk = 36 // Google Play requires a recent target for new apps / updates
        // CI passes the GitHub run number so every build has a higher version and installs as an update.
        val build = (project.findProperty("buildNumber") as String?)?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "1.$build"
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
        // Google Play upload key. Never stored in the repo: the release workflow decodes it from
        // GitHub secrets and passes its path / passwords as environment variables (see docs/RELEASE_SIGNING.md).
        create("release") {
            val path = System.getenv("KEYSTORE_FILE")
            if (path != null && file(path).exists()) {
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // No minify / resource shrinking: the adhan files are found by name at run time (R.raw reflection),
        // which the shrinker would remove.
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Brings java.time (dates/times) to Android 5-7.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
