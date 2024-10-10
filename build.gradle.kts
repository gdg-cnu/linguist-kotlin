plugins {
    kotlin("jvm") version "2.0.10"
    kotlin("plugin.serialization") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.1"
}

group = "io.github.shapelayer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}

tasks {
    shadowJar {
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("standalone")
        manifest {
            attributes["Main-Class"] = "io.github.shapelayer.linguist.MainKt"
        }

        mergeServiceFiles("META-INF/services/*")

        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    }

    build {
        dependsOn(shadowJar)
    }
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
