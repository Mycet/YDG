plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.compose") version "1.8.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
}

group = "dev.maigo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3) //aparentemente necesito esto para hacer un puto combo box
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "dev.maigo.maigoloader.MainKt"
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}