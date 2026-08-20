plugins {
    `kotlin-dsl`
}

buildscript {
    repositories {
        exclusiveContent {
            forRepositories(
                maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") },
                gradlePluginPortal(),
                mavenCentral(),
            )
            filter { includeGroupByRegex(".*") }
        }
    }
}

repositories {
    exclusiveContent {
        forRepositories(
            maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") },
            gradlePluginPortal(),
            mavenCentral(),
        )
        filter { includeGroupByRegex(".*") }
    }
}
