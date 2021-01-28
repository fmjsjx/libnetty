plugins {
    id("libnetty.java-library-conventions")
    id("libnetty.publish-conventions")
}

dependencies {

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("org.slf4j:slf4j-api")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl")
    implementation(project(":libnetty-fastcgi"))
    implementation(project(":libnetty-http"))
    implementation(project(":libnetty-http-client"))
    implementation(project(":libnetty-http-server"))
    implementation(project(":libnetty-resp"))
    implementation(project(":libnetty-resp3"))
    implementation(project(":libnetty-transport"))
    implementation(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-x86_64")
    implementation(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
    implementation(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "linux-aarch_64")
    implementation(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "linux-x86_64")
    implementation(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "osx-x86_64")
    implementation(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "windows-x86_64")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

}

description = "libnetty/Example"

tasks.test {
    // Use junit platform for unit tests.
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("libnetty/Example")
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
