plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    application
}

repositories { mavenCentral() }

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

// Only the generated model is compiled, so nothing but kotlinx.serialization is needed.
sourceSets.main {
    kotlin.srcDir("generated/src/main/kotlin/repro/models")
}

application { mainClass = "MainKt" }
