plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "3.10.0-SNAPSHOT"

publishing {
    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
