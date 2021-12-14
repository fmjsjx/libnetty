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
    api(platform("io.netty:netty-bom:4.1.70.Final"))
    // libcommon-bom
    api(platform("com.github.fmjsjx:libcommon-bom:2.5.3"))
    // junit-bom
    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    // jackson2-bom
    api(platform("com.fasterxml.jackson:jackson-bom:2.13.0"))

    constraints {
        api("org.slf4j:slf4j-api:1.7.32")
        compileOnly("org.projectlombok:lombok:1.18.22")
        annotationProcessor("org.projectlombok:lombok:1.18.22")
        implementation("io.netty:netty-tcnative:2.0.44.Final")
        implementation("io.netty:netty-tcnative-boringssl-static:2.0.44.Final")
        implementation("ch.qos.logback:logback-classic:1.2.6")
        implementation("com.jcraft:jzlib:1.1.3")
        implementation("org.brotli:dec:0.1.2")
        implementation("com.aayushatharva.brotli4j:brotli4j:1.6.0")
        // mockito
        testImplementation("org.mockito:mockito-core:3.12.4")
        testImplementation("org.mockito:mockito-inline:3.12.4")
	}
    // log4j2
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.15.0"))

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
