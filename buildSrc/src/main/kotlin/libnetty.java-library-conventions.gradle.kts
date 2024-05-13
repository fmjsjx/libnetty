plugins {
    `java-library`
}

repositories {
    maven {
        url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
    mavenCentral()
}

dependencies {
    // netty-bom
    api(platform("io.netty:netty-bom:4.1.109.Final"))
    // libcommon-bom
    api(platform("com.github.fmjsjx:libcommon-bom:3.8.0"))
    // jackson2-bom
    api(platform("com.fasterxml.jackson:jackson-bom:2.17.1"))
    // junit-bom
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    // mockito
    testImplementation(platform("org.mockito:mockito-bom:5.11.0"))
    // log4j2
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.23.1"))
    // kotlin coroutines
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.0"))

    constraints {
        api("org.slf4j:slf4j-api:2.0.13")
        val lombokVersion = "1.18.32"
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        implementation("ch.qos.logback:logback-classic:1.5.6")
        implementation("com.jcraft:jzlib:1.1.3")
        implementation("org.brotli:dec:0.1.2")
        implementation("com.aayushatharva.brotli4j:brotli4j:1.16.0")
        val kotlinVersion = "1.9.0"
        implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
        implementation("org.bouncycastle:bcpkix-jdk15to18:1.76")
        val fastjson2Version = "2.0.49"
        api("com.alibaba.fastjson2:fastjson2:$fastjson2Version")
        api("com.alibaba.fastjson2:fastjson2-kotlin:$fastjson2Version")
	}

}

val javaVersion = 17

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
    options.memberLevel = JavadocMemberLevel.PUBLIC
}
