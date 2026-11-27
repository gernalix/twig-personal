plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
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
