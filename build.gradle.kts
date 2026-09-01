plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("org.jetbrains.compose") version "1.8.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
}

group = "dev.mycet"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

val jfxVersion = "21.0.2"
val jfxOs = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "mac"
    else -> "linux"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.openjfx:javafx-base:$jfxVersion:$jfxOs")
    implementation("org.openjfx:javafx-graphics:$jfxVersion:$jfxOs")
    implementation("org.openjfx:javafx-controls:$jfxVersion:$jfxOs")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "dev.mycet.ydg.MainKt"
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}