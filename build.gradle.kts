plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

// Configure project's dependencies
repositories {
    mavenCentral {
        url = uri("https://maven.aliyun.com/repository/central")
    }

    maven {
        url = uri("https://maven.aliyun.com/repository/jcenter")
    }

    maven {
        url = uri("https://maven.aliyun.com/repository/google")
    }

    maven {
        url = uri("https://maven.aliyun.com/repository/gradle-plugin")
    }

    // 备用仓库，以防阿里云镜像缺少某些依赖
    mavenCentral()

    google()

    gradlePluginPortal()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
        repositories {
            maven("https://maven.aliyun.com/repository/central")
            maven("https://maven.aliyun.com/repository/jcenter")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
        }
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1.4.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
    implementation("org.apache.tika:tika-core:2.9.2")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.alibaba:fastjson:1.2.83")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}