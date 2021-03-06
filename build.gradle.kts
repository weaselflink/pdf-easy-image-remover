import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun isNonStable(version: String): Boolean {
    return listOf("alpha", "beta", "dev").any { version.toLowerCase().contains(it) }
}

plugins {
    application
    kotlin("jvm")
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
}

group = "de.stefanbissell.pdf-easy-image-remover"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.itextpdf:itext7-core:7.2.3")
    implementation("org.slf4j:slf4j-nop:1.7.36")
    implementation("com.github.jai-imageio:jai-imageio-core:1.4.0")
    implementation("com.github.jai-imageio:jai-imageio-jpeg2000:1.4.0")
}

application {
    mainClass.set("de.stefanbissell.pdfeasyimageremover.MainKt")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    dependencyUpdates {
        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}
