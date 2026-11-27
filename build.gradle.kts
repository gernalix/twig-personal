// Keep R8 explicit for reproducible builds. AGP 8.13.2 bundles this same version.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools:r8:8.13.19")
    }
}

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
}

val dependencyMinimums = providers.fileContents(
    layout.projectDirectory.file("gradle/dependency-constraints.txt"),
).asText.map { text ->
    text.lineSequence()
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
        .map { it.split(' ', limit = 2) }
        .map { fields ->
            require(fields.size == 2 && fields[0] in setOf("implementation", "testImplementation", "androidTestImplementation")) {
                "Invalid dependency constraint"
            }
            require(Regex("[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[A-Za-z0-9_.+-]+").matches(fields[1])) {
                "Invalid dependency constraint coordinate"
            }
            fields[0] to fields[1]
        }
        .toList()
}

subprojects {
    listOf("com.android.application", "com.android.library", "org.jetbrains.kotlin.jvm").forEach { plugin ->
        pluginManager.withPlugin(plugin) {
            dependencyMinimums.get().forEach { (configuration, dependency) ->
                if (configurations.findByName(configuration) != null) {
                    dependencies.constraints.add(configuration, dependency)
                }
            }
        }
    }
}
