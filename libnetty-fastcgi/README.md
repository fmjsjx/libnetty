# LibNetty Fast-CGI Project

Provides codec components for `Fast-CGI`.

## Quick Start

### Client Side

```java
EventLoopGroup group = new NioEventLoopGroup();
FcgiMessageEncoder fcgiMessageEncoder = new FcgiMessageEncoder();
Bootstrap b = new Bootstrap().group(group).channel(NioSocketChannel.class)
    .option(ChannelOption.TCP_NODELAY, true)
    .handler(new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(fcgiMessageEncoder)
                .addLast(new FcgiMessageDecoder())
                .addLast(new TestClientHandler());
        }
    });
ChannelFuture future = b.connect("127.0.0.1", 9000).sync();
if (channel.isSuccess()) {
    int requestId = 1;
    ByteBuf content = Unpooled.copiedBuffer("p1=a&p2=b", CharsetUtil.UTF_8);
    FcgiRequest request = new FcgiRequest(FcgiVersion.VERSION_1, requestId, content);
    request.params().put("SCRIPT_FILENAME", "/scripts/hello.php")
                    .put("REQUEST_METHOD", "POST")
                    .put("REMOTE_ADDR", "127.0.0.1")
                    .put("SERVER_ADDR", "127.0.0.1")
                    .put("SERVER_PORT", "80")
                    .put("CONTENT_TYPE", "application/www-url-form-encoded")
                    .put("CONTENT_LENGTH", content.readableBytes());
    channel.writeAndFlush(request);
}
```

TestClientHandler.java

```java
class TestClientHandler extends SimpleChannelInboundHandler<FcgiMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FcgiMessage msg) throws Exception {
        System.out.println(msg);
        ctx.close();
    }
}
```

### Server Side

```java
FcgiMessageEncoder encoder = new FcgiMessageEncoder();
NioEventLoopGroup group = new NioEventLoopGroup();
try {
    ServerBootstrap b = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 512).childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(encoder)
                        .addLast(new FcgiMessageDecoder())
                        .addLast(new TestServerHandler());
                }
            });
    b.bind(9000).sync();
    System.out.println("TestServer started!");
    System.in.read();
} finally {
    group.shutdownGracefully();
}
```

TestServerHandler.java

```java
class TestServerHandler extends SimpleChannelInboundHandler<FcgiMessage> {
    static final String JAVA_VERSION = System.getProperty("java.version").split("_")[0];
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FcgiMessage msg) throws Exception {
        // message received
        System.out.println("-- message received --");
        System.out.println(msg);
        if (msg instanceof FcgiRequest) {
            String value = "X-Powered-By: JAVA/" + TestServer.JAVA_VERSION + "\r\n"
                    + "content-type: text/html;charset=UTF-8\r\n"
                    + "\r\n"
                    + "<!DOCTYPE html>\n"
                    + "</html>\n"
                    + "<meta charset=\"UTF-8\" />\n"
                    + "<head>\n"
                    + "<title>Test FastCGI</title>\n"
                    + "</head>\n"
                    + "<body>\n"
                    + "<h1>Test Fast-CGI</h1>\n"
                    + "</body>\n"
                    + "\n</html>";
            FcgiResponse resp = new FcgiResponse(msg.protocolVersion(), msg.requestId(), 0,
                    Unpooled.copiedBuffer(value, CharsetUtil.UTF_8));
            System.out.println("FCGI_RESPONSE ==>");
            System.out.println(resp);
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        } else if (msg instanceof FcgiGetValues) {
            FcgiGetValuesResult result = new FcgiGetValuesResult(msg.protocolVersion());
            ((FcgiGetValues) msg).names().forEach(name -> result.put(name, "0"));
            ctx.writeAndFlush(result).addListener(ChannelFutureListener.CLOSE);
        } else if (msg instanceof FcgiAbortRequest) {
            ctx.close();
        }
    }
}
```
