plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "3.9.0-RC1"

publishing {
    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
