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
    api(platform("io.netty:netty-bom:4.1.84.Final"))
    // libcommon-bom
    api(platform("com.github.fmjsjx:libcommon-bom:2.7.3"))
    // jackson2-bom
    api(platform("com.fasterxml.jackson:jackson-bom:2.13.4.20221013"))
    // junit-bom
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    // mockito
    testImplementation(platform("org.mockito:mockito-bom:4.8.1"))
    // log4j2
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.19.0"))

    constraints {
        api("org.slf4j:slf4j-api:1.7.36")
        val lombokVersion = "1.18.24"
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        implementation("ch.qos.logback:logback-classic:1.2.11")
        implementation("com.jcraft:jzlib:1.1.3")
        implementation("org.brotli:dec:0.1.2")
        implementation("com.aayushatharva.brotli4j:brotli4j:1.8.0")
	}

}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(11)
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
