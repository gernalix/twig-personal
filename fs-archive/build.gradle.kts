plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core-fs"))
    // zip uses the JDK's built-in java.util.zip; 7z uses the pure-Java library below (no .so).
    // rar lives in :fs-archive-rar — split off purely for licensing, see the comment there.
    implementation(libs.commons.compress) // 7z
    implementation(libs.xz)               // 7z LZMA/LZMA2 decode

    testImplementation(project(":fs-local"))
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
