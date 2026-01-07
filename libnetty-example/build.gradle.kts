plugins {
    id("libnetty.java-library-conventions")
    id("libnetty.publish-conventions")
    kotlin("jvm") version embeddedKotlinVersion
}

dependencies {

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("org.slf4j:slf4j-api")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    runtimeOnly("org.apache.logging.log4j:log4j-core")
    implementation(project(":libnetty-fastcgi"))
    implementation(project(":libnetty-http"))
    implementation(project(":libnetty-http-client"))
    implementation(project(":libnetty-http-server"))
    implementation(project(":libnetty-resp"))
    implementation(project(":libnetty-resp3"))
    implementation(project(":libnetty-transport"))
    implementation("io.netty:netty-transport-native-io_uring::linux-x86_64")
    implementation("io.netty:netty-transport-native-epoll::linux-x86_64")
    implementation("io.netty:netty-transport-native-kqueue::osx-x86_64")
    implementation("io.netty:netty-tcnative-boringssl-static::linux-x86_64")
    implementation("io.netty:netty-tcnative-boringssl-static::osx-x86_64")
    implementation("io.netty:netty-tcnative-boringssl-static::windows-x86_64")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.github.fmjsjx:libcommon-kotlin")
    implementation("com.github.fmjsjx:libcommon-json-fastjson2")
    implementation("com.github.fmjsjx:libcommon-json-fastjson2-kotlin")
    implementation("com.github.fmjsjx:libcommon-json-jackson2")
    implementation("com.github.fmjsjx:libcommon-json-jackson2-kotlin")
    implementation("tools.jackson.core:jackson-databind")
    implementation("com.github.fmjsjx:libcommon-json-jackson3")
    implementation("com.github.fmjsjx:libcommon-json-jackson3-kotlin")
    implementation("com.github.fmjsjx:libcommon-json-jsoniter")
    implementation("com.github.fmjsjx:libcommon-json-jsoniter-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.bouncycastle:bcpkix-lts8on:2.73.7")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

}

description = "libnetty/Example"

tasks.test {
    // Use junit platform for unit tests.
    useJUnitPlatform()
}
