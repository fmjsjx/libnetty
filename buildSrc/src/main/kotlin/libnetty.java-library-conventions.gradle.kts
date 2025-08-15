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
    api(platform("io.netty:netty-bom:4.2.4.Final"))
    // libcommon-bom
    api(platform("com.github.fmjsjx:libcommon-bom:3.15.1"))
    // jackson2-bom
    api(platform("com.fasterxml.jackson:jackson-bom:2.19.1"))
    // junit-bom
    testImplementation(platform("org.junit:junit-bom:5.13.3"))
    // mockito
    testImplementation(platform("org.mockito:mockito-bom:5.18.0"))
    // log4j2
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.25.0"))
    // kotlin coroutines
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2"))
    // brotli4j
    implementation(platform("com.aayushatharva.brotli4j:all:1.18.0"))
    // kotlin
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.1.0"))

    constraints {
        api("org.slf4j:slf4j-api:2.0.17")
        val lombokVersion = "1.18.38"
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        implementation("ch.qos.logback:logback-classic:1.5.18")
        implementation("com.jcraft:jzlib:1.1.3")
        implementation("org.brotli:dec:0.1.2")
        val fastjson2Version = "2.0.57"
        api("com.alibaba.fastjson2:fastjson2:$fastjson2Version")
        api("com.alibaba.fastjson2:fastjson2-kotlin:$fastjson2Version")
        implementation("com.github.luben:zstd-jni:1.5.7-4")
	}

}

val javaVersion = 17

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release = javaVersion
    options.compilerArgs = options.compilerArgs + listOf("-Xlint:deprecation")
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
    options.memberLevel = JavadocMemberLevel.PUBLIC
}
