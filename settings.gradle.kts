pluginManagement {
    repositories {
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "libnetty"
include(":libnetty-bom")
include(":libnetty-core")
include(":libnetty-example")
include(":libnetty-fastcgi")
include(":libnetty-http-client")
include(":libnetty-http-server")
include(":libnetty-resp")
