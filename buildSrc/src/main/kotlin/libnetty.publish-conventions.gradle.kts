plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "4.2.0-beta4-SNAPSHOT"

publishing {
    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
