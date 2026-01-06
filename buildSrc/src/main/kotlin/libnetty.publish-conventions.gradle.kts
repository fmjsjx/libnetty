plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "4.1.0-RC-SNAPSHOT"

publishing {
    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
