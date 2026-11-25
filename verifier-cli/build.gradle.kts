plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

// Bytecode for Java 17, built with whatever JDK 17+ runs Gradle (no toolchain download).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

application {
    mainClass.set("io.github.wuttttta.uwb.verifier.MainKt")
    applicationName = "poe-verify"
}

dependencies {
    implementation(project(":protocol"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}
