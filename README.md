# LibNetty Project

A set of some useful libraries based on netty4.1.x.


## Modules

There are a number of modules in LibNetty, here is a quick overview:

### libnetty-fastcgi

The [`libnetty-fastcgi`](libnetty-fastcgi) module provides codec components for `Fast-CGI`.

### libnetty-http

The [`libnetty-http`](libnetty-http) module provides additional utility functions for `HTTP/1.x`.

### libnetty-http-client

The [`libnetty-http-client`](libnetty-http-client) module provides a simplified HTTP client, supports both synchronous and asynchronous(based on JDK8+ CompletableFuture) APIs.

### libnetty-resp

The [`libnetty-resp`](libnetty-resp) module provides codec components for `RESP (REdis Serialization Protocol)`.

### libnetty-transport

The [`libnetty-transport`](libnetty-transport) module provides additional features, such as `auto-selection of java/native transport`, for `netty-transport`.

