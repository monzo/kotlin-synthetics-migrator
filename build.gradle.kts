import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("org.jetbrains.intellij") version "1.4.0"
}

group = "com.monzo.syntheticsmigrator"
version = "1.0.0"

val localProperties = localProperties()
val studioPath = checkNotNull(localProperties.getProperty("studio.path")) {
    "Missing required property: studio.path"
}
val studioVersion = checkNotNull(localProperties.getProperty("studio.version")) {
    "Missing required property: studio.version"
}

repositories {
    mavenCentral()
}

intellij {
    localPath.set(studioPath)
    plugins.set(listOf("android", "Kotlin", "java"))
    intellij.updateSinceUntilBuild.set(false)
}

tasks {
    instrumentCode {
        compilerVersion.set(studioVersion)
    }

    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}

fun localProperties(): Properties {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (!localPropertiesFile.exists()) {
        throw GradleException("Missing required local.properties")
    }
    return Properties().apply {
        load(localPropertiesFile.inputStream())
    }
}
