plugins {
    id("libnetty.java-library-conventions")
    id("libnetty.publish-conventions")
}

dependencies {

    api("org.slf4j:slf4j-api")
    api(project(":libnetty-handler"))
    api(project(":libnetty-http"))
    api(project(":libnetty-transport"))
    api("io.netty:netty-codec-http2")
    implementation("com.github.fmjsjx:libcommon-util")
    compileOnly("io.netty:netty-tcnative-boringssl-static::linux-aarch_64")
    compileOnly("io.netty:netty-tcnative-boringssl-static::linux-x86_64")
    compileOnly("io.netty:netty-tcnative-boringssl-static::osx-x86_64")
    compileOnly("io.netty:netty-tcnative-boringssl-static::windows-x86_64")
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.jcraft:jzlib")
    compileOnly("com.aayushatharva.brotli4j:brotli4j")
    compileOnly("com.github.fmjsjx:libcommon-kotlin")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    compileOnly("com.alibaba.fastjson2:fastjson2")
    compileOnly("com.alibaba.fastjson2:fastjson2-kotlin")
    compileOnly("com.github.fmjsjx:libcommon-json")
    compileOnly("com.github.fmjsjx:libcommon-json-jackson2")
    compileOnly("com.github.fmjsjx:libcommon-json-jackson2-kotlin")
    compileOnly("com.github.fmjsjx:libcommon-json-jsoniter")
    compileOnly("com.github.fmjsjx:libcommon-json-jsoniter-kotlin")
    compileOnly("com.github.fmjsjx:libcommon-json-fastjson2")
    compileOnly("com.github.fmjsjx:libcommon-json-fastjson2-kotlin")
    compileOnly("tools.jackson.core:jackson-databind")
    compileOnly("com.github.fmjsjx:libcommon-json-jackson3")
    compileOnly("com.github.fmjsjx:libcommon-json-jackson3-kotlin")

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
    jvmArgs = listOf(
        "-XX:+EnableDynamicAgentLoading",
        "-Xshare:off",
        classpath.find { "mockito-core" in it.name }?.let { "-javaagent:${it.absolutePath}" } ?: "",
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
                name.set("libnetty/HTTP-Server")
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
