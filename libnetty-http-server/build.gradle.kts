plugins {
    id("libnetty.java-library-conventions")
    id("libnetty.publish-conventions")
}

java {
    registerFeature("nettyNativeSupport") {
        usingSourceSet(sourceSets["main"])
    }
    registerFeature("jsonSupport") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {

    api("org.slf4j:slf4j-api")
    api(project(":libnetty-handler"))
    api(project(":libnetty-http"))
    api(project(":libnetty-transport"))
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-x86_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "linux-aarch_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "linux-x86_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "osx-x86_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "windows-x86_64")
    "jsonSupportApi"("com.fasterxml.jackson.core:jackson-databind")
    "jsonSupportApi"("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    "jsonSupportApi"("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.jcraft:jzlib")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl")

}

description = "libnetty/HTTP-Server"

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
                name.set("libnetty/HTTP-Server")
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
