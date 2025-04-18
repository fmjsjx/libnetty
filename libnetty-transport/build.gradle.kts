plugins {
    id("libnetty.java-library-conventions")
    id("libnetty.publish-conventions")
}

dependencies {

    implementation("org.slf4j:slf4j-api")
    api("io.netty:netty-transport")
    compileOnlyApi(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-aarch_64")
    compileOnlyApi(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-riscv64")
    compileOnlyApi(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-x86_64")
    compileOnlyApi(group = "io.netty", name = "netty-transport-native-io_uring", classifier = "linux-aarch_64")
    compileOnlyApi(group = "io.netty", name = "netty-transport-native-io_uring", classifier = "linux-riscv64")
    compileOnlyApi(group = "io.netty", name = "netty-transport-native-io_uring", classifier = "linux-x86_64")
    compileOnlyApi(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
    compileOnlyApi(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-aarch_64")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    testImplementation("org.apache.logging.log4j:log4j-core")

}

description = "libnetty/Transport"

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
                name.set("libnetty/Transport")
                description.set("A set of some useful libraries based on netty4.2.x.")
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
