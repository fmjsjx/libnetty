plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "3.8.2"

publishing {
    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
