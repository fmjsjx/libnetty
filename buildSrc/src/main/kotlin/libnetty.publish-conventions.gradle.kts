plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "4.0.0-RC"

publishing {
    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
