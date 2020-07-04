package com.github.fmjsjx.libnetty.example.fastcgi;

import com.github.fmjsjx.libnetty.fastcgi.FcgiMessage;
import com.github.fmjsjx.libnetty.fastcgi.FcgiMessageDecoder;
import com.github.fmjsjx.libnetty.fastcgi.FcgiMessageEncoder;
import com.github.fmjsjx.libnetty.fastcgi.FcgiRequest;
import com.github.fmjsjx.libnetty.fastcgi.FcgiVersion;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

public class TestClient {

    public static void main(String[] args) throws Exception {
        FcgiMessageEncoder encoder = new FcgiMessageEncoder();
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap b = new Bootstrap().group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<Channel>() {
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new ReadTimeoutHandler(60)).addLast(encoder)
                                    .addLast(new FcgiMessageDecoder()).addLast(new TestClientHandler());
                        }
                    });
            ChannelFuture cf = b.connect("127.0.0.1", 9000).sync();
            Channel channel = cf.channel();
            if (cf.isSuccess()) {
                request(channel, 1);
                request(channel, 2);
                request(channel, 3);
                channel.closeFuture().sync();
            }
        } finally {
            group.shutdownGracefully();
        }
    }

    static void request(Channel channel, int requestId) {
        ByteBuf content = Unpooled.copiedBuffer(
                "name=o&coung=" + requestId + "&test=haha&timestamp=" + System.currentTimeMillis(), CharsetUtil.UTF_8);
        FcgiRequest request = new FcgiRequest(FcgiVersion.VERSION_1, requestId, content);
        if (requestId != 3) {
            request.beginRequest().keepConn();
        }
        request.params().put("SCRIPT_FILENAME", "/scripts/ok.php").put("REQUEST_METHOD", "POST")
                .put("REMOTE_ADDR", "127.0.0.1").put("SERVER_ADDR", "127.0.0.1").put("SERVER_PORT", "80")
                .put("CONTENT_TYPE", "application/www-url-form-encoded").put("CONTENT_LENGTH", content.readableBytes());
        System.out.println("== request ==");
        System.out.println(request);
        channel.writeAndFlush(request);
    }

}

class TestClientHandler extends SimpleChannelInboundHandler<FcgiMessage> {

    int count = 0;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.err.println(ctx.channel() + " disconnected");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FcgiMessage msg) throws Exception {
        System.out.println(msg);
        count++;
        if (count == 3) {
            ctx.close();
        }
    }

}
