plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "4.3.0-alpha1"

// Prevent Gradle from generating high-order checksums (like sha256/sha512).
// This restricts the output to only standard md5 and sha1.
System.setProperty("org.gradle.internal.publish.checksums.official", "true")

// Completely disable the Gradle Module Metadata (.module) generation task.
// This instantly eliminates the .module file, its .asc signature, and all associated checksums.
tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }

publishing {
    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

// Security guard: Ensures insecure/extra checksums are intercepted during the local repository generation phase
tasks.withType<PublishToMavenRepository>().configureEach {
    System.setProperty("org.gradle.internal.publish.checksums.insecure", "true")
}

// Use standard TaskContainer.register(...) to perfectly comply with Gradle 9+ configuration avoidance API
val deleteLocalMavenMetadata = tasks.register("deleteLocalMavenMetadata") {
    group = "publishing"
    description = "Removes redundant maven-metadata.xml files from staging directory."

    val stagingDir = rootProject.layout.buildDirectory.dir("staging-deploy")

    doLast {
        val dirFile = stagingDir.get().asFile
        if (dirFile.exists()) {
            dirFile.walkTopDown().forEach { file ->
                if (file.name.startsWith("maven-metadata.xml")) {
                    val deleted = file.delete()
                    if (deleted) {
                        logger.lifecycle("Cleaned up redundant artifact: ${file.relativeTo(dirFile)}")
                    }
                }
            }
        }
    }
}

// Automatically trigger this cleanup task AFTER the publishing tasks are done
tasks.withType<PublishToMavenRepository>().configureEach {
    finalizedBy(deleteLocalMavenMetadata)
}