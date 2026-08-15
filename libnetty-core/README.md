# LibNetty Core Project

Provides additional features for Netty4.

## Features

- Provides a SslContextProvider.
- Provides additional features for `netty-transport`.
- Provides additional utility functions for `HTTP/1.x`.

## Auto-selection of java/native transport examples

```java
TransportLibrary transportLibrary = TransportLibrary.getDefault();
// EpollEventLoopGroup | KQueueEventLoopGroup | NioEventLoopGroup
EventLoopGroup group = transportLibrary.createGroup();
// EpollSocketChannel.class | KQueueSocketChannel.class | NioSocketChannel.class
Class<? extends Channel> channelClass = transportLibrary.channelClass();
// EpollServerSocketChannel.class | KQueueServerSocketChannel.class | NioServerSocketChannel.class
Class<? extends ServerChannel> serverChannelClass = transportLibrary.serverChannelClass();
```

## Auto-fix remote address examples

```java
class MyHandler extends SimpleChannelInboundHandler<FcgiMessage> { 

//...

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FcgiMessage msg) {
        String remoteAddress = HttpUtil.remoteAddress(ctx.channel(), msg.headers());
        //... 
    }

//...

}
```

### Convert Content-Type
```java
// convert content-type to "application/json;charset=UTF-8"
String contentType = HttpUtil.contentType(HttpHeaderValues.APPLICATION_JSON, CharsetUtil.UTF_8);
```


