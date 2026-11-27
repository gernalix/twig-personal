import groovy.json.JsonSlurper
import org.gradle.api.artifacts.Configuration

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
        maven("https://jitpack.io") // termux terminal-view/emulator
    }
}

rootProject.name = "Twig"
include(":app")
include(":core-fs")
include(":fs-local")
include(":fs-archive")
include(":fs-archive-rar")
include(":fs-network")
include(":fs-smb")
include(":fs-restic")
include(":fs-zstd")
include(":git-lite")

include(":fs-nfs")

// The build plugins and test tools resolve some dependencies outside the app's
// dependency declarations. Keep tested minimums scoped to those classpaths.
val toolchainPlan = layout.settingsDirectory.file("gradle/toolchain-constraints.json")
val toolchainGroups = JsonSlurper().parseText(providers.fileContents(toolchainPlan).asText.get()) as Map<*, *>
val toolchainRules = mutableMapOf<String, Map<String, String>>()
val supportedVersion = Regex("""([0-9]+(?:\.[0-9]+)*)(?:-(android|jre))?""")
for (entry in toolchainGroups["groups"] as List<*>) {
    val group = entry as Map<*, *>
    val versions = (group["versions"] as Map<*, *>).entries.associate { (coordinate, version) ->
        val minimum = version as String
        require(supportedVersion.matches(minimum)) { "Invalid Gradle classpath minimum: $minimum" }
        (coordinate as String) to minimum
    }
    for (context in group["contexts"] as List<*>) {
        require(toolchainRules.put(context as String, versions) == null) { "Duplicate Gradle classpath: $context" }
    }
}

fun isBelowMinimum(requested: String?, minimum: String): Boolean {
    if (requested == null) return true
    val current = supportedVersion.matchEntire(requested) ?: return false
    val candidate = supportedVersion.matchEntire(minimum) ?: return false
    if (current.groupValues[2] != candidate.groupValues[2]) return false
    val a = current.groupValues[1].split('.').map(String::toInt)
    val b = candidate.groupValues[1].split('.').map(String::toInt)
    for (index in 0 until maxOf(a.size, b.size)) {
        val before = a.getOrElse(index) { 0 }
        val after = b.getOrElse(index) { 0 }
        if (before != after) return before < after
    }
    return false
}

gradle.beforeProject {
    val owner = path
    fun configureMinimum(configuration: Configuration, buildscript: Boolean) {
        val versions = toolchainRules["$owner|${configuration.name}|$buildscript"] ?: return
        configuration.resolutionStrategy.eachDependency {
            val minimum = versions["${requested.group}:${requested.name}"] ?: return@eachDependency
            if (isBelowMinimum(requested.version, minimum)) {
                useVersion(minimum)
                because("Twig's verified Gradle classpath minimum")
            }
        }
    }
    buildscript.configurations.configureEach { configureMinimum(this, true) }
    configurations.configureEach { configureMinimum(this, false) }
}
