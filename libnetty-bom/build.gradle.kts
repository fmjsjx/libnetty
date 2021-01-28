plugins {
    `java-platform`
    id("libnetty.publish-conventions")
}

description = "libnetty/BOM"

dependencies {
    constraints {
        api(project(":libnetty-example"))
        api(project(":libnetty-fastcgi"))
        api(project(":libnetty-handler"))
        api(project(":libnetty-http"))
        api(project(":libnetty-http-client"))
        api(project(":libnetty-http-server"))
        api(project(":libnetty-resp"))
        api(project(":libnetty-resp3"))
        api(project(":libnetty-transport"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["javaPlatform"])
            pom {
                name.set("libnetty/BOM")
                description.set("A set of some common useful libraries.")
                url.set("https://github.com/fmjsjx/libnetty")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("fmjsjx")
                        name.set("MJ Fang")
                        email.set("fmjsjx@163.com")
                        url.set("https://github.com/fmjsjx")
                        organization.set("fmjsjx")
                        organizationUrl.set("https://github.com/fmjsjx")
                    }
                }
                scm {
                    url.set("https://github.com/fmjsjx/libnetty")
                    connection.set("scm:git:https://github.com/fmjsjx/libnetty.git")
                    developerConnection.set("scm:git:https://github.com/fmjsjx/libnetty.git")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
