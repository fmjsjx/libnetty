# LibNetty Project

A set of some useful libraries based on Netty4.

## JDK version compatibility list
| version | JDK version |
|---------|-------------|
| 4.x     | JDK 21+     |
| 3.x     | JDK 17+     |
| 2.x     | JDK 11+     |
| older   | JDK 8+      |

## Add Dependencies

### Add Maven Dependencies
`pom.xml`
```xml
<pom>
  <dependencyManagement>
    <dependencies>
      <!-- BOM -->
      <dependency>
        <groupId>com.github.fmjsjx</groupId>
        <artifactId>libnetty-bom</artifactId>
        <version>4.1.0-RC2</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <!-- HTTP server -->
    <dependency>
      <groupId>com.github.fmjsjx</groupId>
      <artifactId>libnetty-http-server</artifactId>
    </dependency>
  </dependencies>
</pom>
```

### Add Gradle Dependencies

#### Groovy DSL
```groovy
repositories {
    mavenCentral
}

dependencies {
    // BOM
    implementation platform('com.github.fmjsjx:libnetty-bom:4.1.0-RC2')
    // HTTP server
    implementation 'com.github.fmjsjx:libnetty-http-server'
}
```

#### Kotlin DSL
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    // BOM
    implementation(platform("com.github.fmjsjx:libnetty-bom:4.1.0-RC2"))
    // HTTP server
    implementation("com.github.fmjsjx:libnetty-http-server")
}
```

## Modules

There are a number of modules in LibNetty, here is a quick overview:

### libnetty-fastcgi

The [`libnetty-fastcgi`](libnetty-fastcgi) module provides codec components for [`Fast-CGI`](https://fastcgi-archives.github.io/FastCGI_Specification.html).

### libnetty-handler

The [`libnetty-handler`](libnetty-handler) module provides additional features for `netty-handler`.

### libnetty-http

The [`libnetty-http`](libnetty-http) module provides additional utility functions for `HTTP/1.x`.

### libnetty-http-client

The [`libnetty-http-client`](libnetty-http-client) module provides a simplified HTTP client, supports both synchronous and asynchronous(based on JDK8+ CompletableFuture) APIs.

### libnetty-http-server

The [`libnetty-http-server`](libnetty-http-server) module provides a simplified HTTP server framework.

### libnetty-resp

The [`libnetty-resp`](libnetty-resp) module provides codec components for [`RESP(REdis Serialization Protocol)`](https://redis.io/topics/protocol).

### libnetty-resp3

The [`libnetty-resp3`](libnetty-resp3) module provides codec components for [`RESP3 specification`](https://github.com/antirez/RESP3/blob/master/spec.md).

### libnetty-transport

The [`libnetty-transport`](libnetty-transport) module provides additional features, such as `auto-selection of java/native transport`, for `netty-transport`.

