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
    api(platform("io.netty:netty-bom:4.2.0.Beta1"))
    // libcommon-bom
    api(platform("com.github.fmjsjx:libcommon-bom:3.9.1"))
    // jackson2-bom
    api(platform("com.fasterxml.jackson:jackson-bom:2.18.1"))
    // junit-bom
    testImplementation(platform("org.junit:junit-bom:5.11.2"))
    // mockito
    testImplementation(platform("org.mockito:mockito-bom:5.14.2"))
    // log4j2
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.24.1"))
    // kotlin coroutines
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.9.0"))
    // brotli4j
    implementation(platform("com.aayushatharva.brotli4j:all:1.17.0"))
    // kotlin
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.0"))

    constraints {
        api("org.slf4j:slf4j-api:2.0.16")
        val lombokVersion = "1.18.34"
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        implementation("ch.qos.logback:logback-classic:1.5.9")
        implementation("com.jcraft:jzlib:1.1.3")
        implementation("org.brotli:dec:0.1.2")
        implementation("com.aayushatharva.brotli4j:brotli4j")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        val fastjson2Version = "2.0.53"
        api("com.alibaba.fastjson2:fastjson2:$fastjson2Version")
        api("com.alibaba.fastjson2:fastjson2-kotlin:$fastjson2Version")
        implementation("com.github.luben:zstd-jni:1.5.6-7")
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
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
    options.memberLevel = JavadocMemberLevel.PUBLIC
}
