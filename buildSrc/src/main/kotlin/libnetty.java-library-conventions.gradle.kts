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
    api(platform("io.netty:netty-bom:4.1.73.Final"))
    // libcommon-bom
    api(platform("com.github.fmjsjx:libcommon-bom:2.6.1"))
    // junit-bom
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    // jackson2-bom
    api(platform("com.fasterxml.jackson:jackson-bom:2.13.1"))

    constraints {
        api("org.slf4j:slf4j-api:1.7.32")
        compileOnly("org.projectlombok:lombok:1.18.22")
        annotationProcessor("org.projectlombok:lombok:1.18.22")
        implementation("ch.qos.logback:logback-classic:1.2.10")
        implementation("com.jcraft:jzlib:1.1.3")
        implementation("org.brotli:dec:0.1.2")
        implementation("com.aayushatharva.brotli4j:brotli4j:1.6.0")
        // mockito
        testImplementation("org.mockito:mockito-core:4.2.0")
        testImplementation("org.mockito:mockito-inline:4.2.0")
	}
    // log4j2
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.17.1"))

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
