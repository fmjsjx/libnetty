plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "4.1.6"

publishing {
    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
