plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core-fs"))
    implementation("org.bouncycastle:bcprov-jdk18on:1.87") // scrypt (same source as sshj, deduplicate)

    // org.json: Android bundles it at runtime; the compile and unit-test classpaths provide it themselves
    compileOnly("org.json:json:20260814")
    testImplementation(project(":fs-local"))
    testImplementation("org.json:json:20260814")
    testImplementation("io.airlift:aircompressor:2.0.3") // pure-Java zstd for tests (usable on the JVM)
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
