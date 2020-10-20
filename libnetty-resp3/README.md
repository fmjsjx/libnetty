# LibNetty RESP3 Project

Provides codec components for [`RESP3 specification`](https://github.com/antirez/RESP3/blob/master/spec.md).

## Quick Start

### Client Side

```java
RespMessageEncoder respMessageEncoder = new RespMessageEncoder();
EventLoopGroup group = new NioEventLoopGroup();
Bootstrap b = new Bootstrap().group(group).channel(NioSocketChannel.class)
    .option(ChannelOption.TCP_NODELAY, true)
    .handler(new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(respMessageEncoder)
                .addLast(new Resp3MessageDecoder())
                .addLast(new TestClientHandler());
        }
    });
ChannelFuture future = b.connect("127.0.0.1", 6379).sync();
if (future.isSuccess()) {
    Channel channel = future.channel();
    DefaultArrayMessage cmd = DefaultArrayMessage.bulkStringArrayAscii(channel.alloc(), "HELLO", "3");
    channel.writeAndFlush(cmd);
}
```

TestClientHandler.java

```java
class TestClientHandler extends SimpleChannelInboundHandler<RespMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RespMessage msg) throws Exception {
        // RespIntegerMessage, RespErrorMessage, RespIntegerMessage, RespBulkStringMessage, RespArrayMessage
        System.out.println(msg);
        ...
    }
}
```

### Server Side

```java
RespMessageEncoder respMessageEncoder = new RespMessageEncoder();
NioEventLoopGroup group = new NioEventLoopGroup();
try {
    ServerBootstrap b = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 512).childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(respMessageEncoder)
                        .addLast(new RedisRequestDecoder())
                        .addLast(new TestServerHandler());
                }
            });
    b.bind(6379).sync();
    System.out.println("TestServer started!");
    System.in.read();
} finally {
    group.shutdownGracefully();
}
```

TestServerHandler.java

```java
class TestServerHandler extends SimpleChannelInboundHandler<RedisRequest> {

    private static final CachedBulkStringMessage SERVER_KEY = CachedBulkStringMessage.createAscii("server");
    private static final CachedBulkStringMessage SERVER_VALUE = CachedBulkStringMessage.createAscii("test");
    private static final CachedBulkStringMessage VERSION_KEY = CachedBulkStringMessage.createAscii("version");
    private static final CachedBulkStringMessage VERSION_VALUE = CachedBulkStringMessage.createAscii("1.0.0");
    private static final CachedBulkStringMessage PROTO_KEY = CachedBulkStringMessage.createAscii("proto");

    private boolean supportResp3;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RedisRequest msg) throws Exception {
        // message received
        System.out.println("-- message received --");
        System.out.println(msg);
        String commond = msg.command().toText().toUpperCase();
        switch (commond) {
        case "HELLO":
            // hello
            int version = msg.argument(1).intValue();
            switch (version) {
                case 2:
                    DefaultArrayMessage<RespMessage> array = new DefaultArrayMessage<>(SERVER_KEY, SERVER_VALUE,
                            VERSION_KEY, VERSION_VALUE, PROTO_KEY, RespMessages.integer(2));
                    ctx.writeAndFlush(array).addListener(READ_NEXT);
                    break;
                case 3:
                    supportResp3 = true;
                    DefaultMapMessage<RespBulkStringMessage, RespMessage> map = new DefaultMapMessage<>();
                    map.put(SERVER_KEY, SERVER_VALUE);
                    map.put(VERSION_KEY, VERSION_VALUE);
                    map.put(PROTO_KEY, RespMessages.integer(3));
                    ctx.writeAndFlush(map);
                    break;
                default:
                    ctx.writeAndFlush(DefaultErrorMessage.create(ctx.alloc(), "NOPROTO", "sorry this protocol version is not supported"));
                    break;
            }
            break;
        case "ECHO":
            // always returns message
            ctx.writeAndFlush((msg.argument(1).retainedDuplicate());
            break;
        case "QUIT":
            ctx.writeAndFlush(RespMessages.ok()).addListener(CLOSE);
            break;
        case "PING":
            ctx.writeAndFlush(RespMessages.pong());
            break;
        case "SELECT":
            // just returns OK
            ctx.writeAndFlush(RespMessages.ok());
            break;
        default:
            RespErrorMessage error = DefaultErrorMessage.createErrAscii(ctx.alloc(), "unknown command " + command);
            ctx.writeAndFlush(error).addListener(CLOSE);
            break;
        }
    }
}
```
