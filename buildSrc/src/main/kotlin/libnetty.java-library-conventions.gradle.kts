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
    api(platform("io.netty:netty-bom:4.1.87.Final"))
    // libcommon-bom
    api(platform("com.github.fmjsjx:libcommon-bom:3.0.0-RC1"))
    // jackson2-bom
    api(platform("com.fasterxml.jackson:jackson-bom:2.14.1"))
    // junit-bom
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    // mockito
    testImplementation(platform("org.mockito:mockito-bom:5.0.0"))
    // log4j2
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.19.0"))
    // kotlin coroutines
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4"))

    constraints {
        api("org.slf4j:slf4j-api:2.0.6")
        val lombokVersion = "1.18.24"
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        implementation("ch.qos.logback:logback-classic:1.4.5")
        implementation("com.jcraft:jzlib:1.1.3")
        implementation("org.brotli:dec:0.1.2")
        implementation("com.aayushatharva.brotli4j:brotli4j:1.8.0")
        val kotlinVersion = "1.7.22"
        implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
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

