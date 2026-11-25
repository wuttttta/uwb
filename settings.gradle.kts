pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "uwb"

include(":protocol", ":verifier-cli")

// The Android app needs an Android SDK. On machines without one (for example the
// Raspberry Pi this is developed on: Google ships no ARM64 Linux build tools), the
// JVM modules still build and test; CI always has the SDK and builds everything.
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    file("local.properties").let { it.exists() && it.readText().contains("sdk.dir") }
if (hasAndroidSdk) include(":app") else logger.lifecycle("POE: no Android SDK found, skipping :app")
