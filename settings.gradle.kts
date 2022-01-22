pluginManagement {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
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
