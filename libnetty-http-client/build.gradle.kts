plugins {
    id("libnetty.java-library-conventions")
    id("libnetty.publish-conventions")
}

java {
    registerFeature("nettyNativeSupport") {
        usingSourceSet(sourceSets["main"])
    }
    registerFeature("brotliSupport") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("org.slf4j:slf4j-api")

    api(project(":libnetty-handler"))
    api(project(":libnetty-http"))
    api("io.netty:netty-handler-proxy")
    api(project(":libnetty-transport"))
    api("com.github.fmjsjx:libcommon-util")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-x86_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "linux-aarch_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "linux-x86_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "osx-x86_64")
    "nettyNativeSupportImplementation"(group = "io.netty", name = "netty-tcnative-boringssl-static", classifier = "windows-x86_64")
    implementation("com.jcraft:jzlib")
    implementation("org.brotli:dec:0.1.2")
    "brotliSupportImplementation"("com.aayushatharva.brotli4j:brotli4j")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl")

}

description = "libnetty/HTTP-Client"

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
                name.set("libnetty/HTTP-Client")
                description.set("A set of some useful libraries based on netty4.1.x.")
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
