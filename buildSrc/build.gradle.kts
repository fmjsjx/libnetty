plugins {
    `kotlin-dsl`
}

buildscript {
    repositories {
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

repositories {
    maven {
        url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
    gradlePluginPortal()
}
