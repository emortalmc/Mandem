import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "dev.emortal"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://nexus.velocitypowered.com/repository/maven-public/")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://jitpack.io")
}

dependencies {
    //compileOnly("com.velocitypowered:velocity-api:3.1.0")
    //kapt("com.velocitypowered:velocity-api:3.1.0")
    compileOnly(files("libs/velocity-api-3.1.2-SNAPSHOT-all.jar"))
    kapt(files("libs/velocity-api-3.1.2-SNAPSHOT-all.jar"))

    //implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.kyori:adventure-text-minimessage:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    compileOnly("org.redisson:redisson:3.17.5")
    compileOnly("mysql:mysql-connector-java:8.0.30")
    compileOnly("com.zaxxer:HikariCP:5.0.1")

    compileOnly("net.luckperms:api:5.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        relocate("com.zaxxer.hikari", "dev.emortal.datadependency.libs.hikari")
    }
}
