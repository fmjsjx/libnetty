plugins {
    `java-library`
}

repositories {
    maven {
        url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
    jcenter()
}

dependencies {
    // netty-bom
    api(platform("io.netty:netty-bom:4.1.59.Final"))
    // libcommon-bom
    api(platform("com.github.fmjsjx:libcommon-bom:1.1.1"))
    // junit-bom
    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    // jackson2-bom
    api(platform("com.fasterxml.jackson:jackson-bom:2.12.1"))

    constraints {
        api("org.slf4j:slf4j-api:1.7.30")
        compileOnly("org.projectlombok:lombok:1.18.16")
        annotationProcessor("org.projectlombok:lombok:1.18.16")
        implementation("io.netty:netty-tcnative:2.0.36.Final")
        implementation("io.netty:netty-tcnative-boringssl-static:2.0.36.Final")
        implementation("ch.qos.logback:logback-classic:1.2.3")
        implementation("com.jcraft:jzlib:1.1.3")
        implementation("org.brotli:dec:0.1.2")
        // mockito
        testImplementation("org.mockito:mockito-core:3.7.7")
        testImplementation("org.mockito:mockito-inline:3.7.7")
	}
    // log4j2
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.14.0"))

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
