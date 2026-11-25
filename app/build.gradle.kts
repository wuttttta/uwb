plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.wuttttta.uwb"
    // Compose BOM 2026.11 needs android-37.1+; 37.2 is the newest stable platform.
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        // Attestation checks pin this package name: changing it invalidates old proofs.
        applicationId = "io.github.wuttttta.uwb"
        minSdk = 34 // provisioned STS needs Android 14
        targetSdk = 37
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
    }

    // A fixed key from GitHub secrets, when present, so each build installs over the
    // last one and the signing-certificate digest in attestations stays stable.
    // Without it the build uses the machine's throwaway debug key.
    val ksFile = System.getenv("UWB_KEYSTORE_FILE")
    signingConfigs {
        if (ksFile != null) {
            create("fixed") {
                storeFile = file(ksFile)
                storeType = "pkcs12"
                storePassword = System.getenv("UWB_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UWB_KEY_ALIAS")?.takeIf { it.isNotEmpty() } ?: "uwb"
                keyPassword = System.getenv("UWB_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (ksFile != null) signingConfig = signingConfigs.getByName("fixed")
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

    defaultConfig {
        // Debug-only switches from CLAUDE.md. Both stay false in every build we ship.
        buildConfigField("boolean", "INSECURE_STATIC_STS", "false")
        buildConfigField("boolean", "DEBUG_LOG_SECRETS", "false")
    }
}

dependencies {
    implementation(project(":protocol"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel.compose)

    // Radios (wired up in M1).
    implementation(libs.uwb)
    implementation(libs.play.services.nearby)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
