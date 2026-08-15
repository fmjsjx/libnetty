plugins {
    id("libnetty.java-library-conventions")
    id("libnetty.publish-conventions")
}

dependencies {

    implementation("org.slf4j:slf4j-api")
    api("io.netty:netty-handler")
    implementation("io.netty:netty-pkitesting")
    api("io.netty:netty-codec-http")
    compileOnlyApi("io.netty:netty-codec-http2")
    api("io.netty:netty-transport")
    compileOnlyApi("io.netty:netty-transport-classes-io_uring")
    compileOnlyApi("io.netty:netty-transport-classes-epoll")
    compileOnlyApi("io.netty:netty-transport-classes-kqueue")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    testImplementation("org.apache.logging.log4j:log4j-core")
    testImplementation("org.mockito:mockito-core")

}

description = "libnetty/Core"

tasks.test {
    // Use JUnit platform for unit tests.
    useJUnitPlatform()
    // Fix for java 21
    jvmArgs = listOf(
        "-XX:+EnableDynamicAgentLoading",
        "-Xshare:off",
        "--add-exports=java.base/sun.security.x509=ALL-UNNAMED",
        "--add-exports=java.base/sun.security.pkcs=ALL-UNNAMED",
    )
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
                name.set("libnetty/Core")
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
