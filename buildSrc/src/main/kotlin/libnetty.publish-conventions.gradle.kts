plugins {
    `maven-publish`
    signing
}

group = "com.github.fmjsjx"
version = "2.7.0-RC1"

publishing {
    repositories {
        maven {
            name = "sonatypeOss"
            credentials(PasswordCredentials::class)
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}
