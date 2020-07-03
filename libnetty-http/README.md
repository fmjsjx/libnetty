# LibNetty HTTP Project

Provides additional utility functions for `HTTP/1.x`.

## Features

### Auto-fix remote address

```java
...
protected void channelRead0(ChannelHandlerContext ctx, FcgiMessage msg) {
    String remoteAddress = HttpUtil.remoteAddress(ctx.channel(), msg.headers());
    ... 
}
...
```

### Convert Content-Type
```java
// convert content-type to "application/json;charset=UTF-8"
String contentType = HttpUtil.contentType(HttpHeaderValues.APPLICATION_JSON, CharsetUtil.UTF_8);
```