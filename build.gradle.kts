plugins {
    kotlin("jvm") version "2.0.0"
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}
apply(plugin = "maven-publish")
group = "aster.amo"
version = "1.0.2"

repositories {
    mavenCentral()
    maven {
        name = "MinecraftForge"
        url = uri("https://maven.minecraftforge.net/")

        content {
            includeGroup("net.minecraftforge")
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.reflections:reflections:0.10.2")
    implementation("net.minecraftforge:eventbus:6.0.5")
}

gradlePlugin {
    plugins {
        create("generateForgeEventsDSL") {
            id = "aster.amo.forge-events-dsl-plugin"
            implementationClass = "aster.amo.ktforgeeventdsl.ForgeEventsPlugin"
        }
    }
}
kotlin {
    jvmToolchain(17)
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "aster.amo"
            artifactId = "forge-events-dsl-plugin"
            version = "1.0.2"
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}