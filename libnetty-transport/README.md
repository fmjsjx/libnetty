# LibNetty Transport Project

Provides additional features for `netty-transport`.

## Features

- Auto-selection of java/native transport


## Quick Start

```java
TransportLibrary transportLibrary = TransportLibrary.getDefault();
// EpollEventLoopGroup | KQueueEventLoopGroup | NioEventLoopGroup
EventLoopGroup group = transportLibrary.createGroup();
// EpollSocketChannel.class | KQueueSocketChannel.class | NioSocketChannel.class
Class<? extends Channel> channelClass = transportLibrary.channelClass();
// EpollServerSocketChannel.class | KQueueServerSocketChannel.class | NioServerSocketChannel.class
Class<? extends ServerChannel> serverChannelClass = transportLibrary.serverChannelClass();
```