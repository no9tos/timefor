plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release builds get their version from the git tag (v1.2.3 -> "1.2.3", code 10203) via
// TIMEFOR_VERSION. Other builds are marked as development builds.
val releaseVersion = System.getenv("TIMEFOR_VERSION")?.takeIf { it.isNotBlank() }
val appVersionName = releaseVersion
    ?: System.getenv("GITHUB_RUN_NUMBER")?.let { "dev-$it" }
    ?: "dev"
val appVersionCode = releaseVersion
    ?.split(".")
    ?.map { it.toInt() }
    ?.let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }
    ?: 1

android {
    namespace = "com.timefor.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.timefor.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        // Release signing comes from the environment (GitHub Actions secrets); see RELEASING.md.
        // The key itself must never be committed.
        val keystore = System.getenv("TIMEFOR_KEYSTORE_FILE")
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("TIMEFOR_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("TIMEFOR_KEY_ALIAS")
                keyPassword = System.getenv("TIMEFOR_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
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
        viewBinding = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
