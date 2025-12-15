package com.github.fmjsjx.libnetty.example.fastcgi;

import com.github.fmjsjx.libnetty.fastcgi.FcgiAbortRequest;
import com.github.fmjsjx.libnetty.fastcgi.FcgiGetValues;
import com.github.fmjsjx.libnetty.fastcgi.FcgiGetValuesResult;
import com.github.fmjsjx.libnetty.fastcgi.FcgiMessage;
import com.github.fmjsjx.libnetty.fastcgi.FcgiMessageDecoder;
import com.github.fmjsjx.libnetty.fastcgi.FcgiMessageEncoder;
import com.github.fmjsjx.libnetty.fastcgi.FcgiRequest;
import com.github.fmjsjx.libnetty.fastcgi.FcgiResponse;

import com.github.fmjsjx.libnetty.transport.io.NioIoTransportLibrary;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

/**
 * Test server.
 */
public class TestServer {

    static final String JAVA_VERSION = System.getProperty("java.version").split("_")[0];

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        FcgiMessageEncoder encoder = new FcgiMessageEncoder();
        var group = NioIoTransportLibrary.getInstance().createGroup();
        try {

            ServerBootstrap b = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 512).childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<>() {
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new ReadTimeoutHandler(60)).addLast(encoder)
                                    .addLast(new FcgiMessageDecoder()).addLast(new TestServerHandler());
                        }
                    });
            b.bind(9000).sync();
            System.out.println("TestServer started!");
            //noinspection ResultOfMethodCallIgnored
            System.in.read();

        } finally {
            group.shutdownGracefully();
        }
    }

    private TestServer() {
    }

}

class TestServerHandler extends SimpleChannelInboundHandler<FcgiMessage> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.err.println(ctx.channel() + " connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.err.println(ctx.channel() + " disconnected");
    }

    @SuppressWarnings("CallToPrintStackTrace")
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FcgiMessage msg) {
        System.out.println("-- message received --");
        System.out.println(msg);
        if (msg instanceof FcgiRequest req) {
            String value = "X-Powered-By: JAVA/" + TestServer.JAVA_VERSION + "\r\n" // CRLF
                    + "content-type: text/html;charset=UTF-8\r\n" // CRLF
                    + "\r\n" // CRLF
                    + "<!DOCTYPE html>\n" // LF
                    + "</html>\n" // LF
                    + "<meta charset=\"UTF-8\" />\n" // LF
                    + "<head>\n" // LF
                    + "<title>Test FastCGI</title>\n" // LF
                    + "</head>\n" // LF
                    + "<body>\n" // LF
                    + "<h1>Test Fast-CGI</h1>\n" // LF
                    + "</body>\n" // LV
                    + "\n</html>";
            FcgiResponse resp = new FcgiResponse(req.protocolVersion(), req.requestId(), 0,
                    Unpooled.copiedBuffer(value, CharsetUtil.UTF_8));
            System.out.println("FCGI_RESPONSE ==>");
            System.out.println(resp);
            if (req.beginRequest().isKeepConn()) {
                ctx.writeAndFlush(resp);
            } else {
                ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
            }
        } else if (msg instanceof FcgiGetValues) {
            FcgiGetValuesResult result = new FcgiGetValuesResult(msg.protocolVersion());
            ((FcgiGetValues) msg).names().forEach(name -> result.put(name, "0"));
            ctx.writeAndFlush(result);
        } else if (msg instanceof FcgiAbortRequest) {
            ctx.close();
        }
    }

}
