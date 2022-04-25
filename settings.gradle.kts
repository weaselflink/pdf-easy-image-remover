@file:Suppress("LocalVariableName")

rootProject.name = "pdf-easy-image-remover"

pluginManagement {
    val kotlin_version: String by settings
    val versions_version: String by settings
    val shadow_version: String by settings
    plugins {
        kotlin("jvm") version kotlin_version
        id("com.github.ben-manes.versions") version versions_version
        id("com.github.johnrengelman.shadow") version shadow_version
    }
}
