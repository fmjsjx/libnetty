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
    registerFeature("brotliSupport") {
        usingSourceSet(sourceSets["main"])
    }
    registerFeature("kotlinSupport") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {

    api("org.slf4j:slf4j-api")
    api(project(":libnetty-handler"))
    api(project(":libnetty-http"))
    api(project(":libnetty-transport"))
    implementation("com.github.fmjsjx:libcommon-util")
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
    "brotliSupportImplementation"("com.aayushatharva.brotli4j:brotli4j")
    "kotlinSupportImplementation"("com.github.fmjsjx:libcommon-kotlin")
    "kotlinSupportImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    "jsonSupportApi"("com.alibaba.fastjson2:fastjson2")
    "kotlinSupportImplementation"("com.alibaba.fastjson2:fastjson2-kotlin")
    "jsonSupportApi"("com.github.fmjsjx:libcommon-json")
    "jsonSupportApi"("com.github.fmjsjx:libcommon-json-jackson2")
    "kotlinSupportImplementation"("com.github.fmjsjx:libcommon-json-jackson2-kotlin")
    "jsonSupportApi"("com.github.fmjsjx:libcommon-json-jsoniter")
    "kotlinSupportImplementation"("com.github.fmjsjx:libcommon-json-jsoniter-kotlin")
    "jsonSupportApi"("com.github.fmjsjx:libcommon-json-fastjson2")
    "kotlinSupportImplementation"("com.github.fmjsjx:libcommon-json-fastjson2-kotlin")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    testImplementation("org.apache.logging.log4j:log4j-core")

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
