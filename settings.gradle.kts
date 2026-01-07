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
include(":libnetty-example")
include(":libnetty-fastcgi")
include(":libnetty-handler")
include(":libnetty-http")
include(":libnetty-http-client")
include(":libnetty-http-server")
include(":libnetty-resp")
include(":libnetty-resp3")
include(":libnetty-transport")
include(":libnetty-http2-server")
