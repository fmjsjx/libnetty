package com.github.fmjsjx.libnetty.example.resp;

import com.github.fmjsjx.libnetty.resp.DefaultArrayMessage;
import com.github.fmjsjx.libnetty.resp.DefaultRespMessageDecoder;
import com.github.fmjsjx.libnetty.resp.RespArrayMessage;
import com.github.fmjsjx.libnetty.resp.RespBulkStringMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;
import com.github.fmjsjx.libnetty.resp.RespMessageEncoder;
import com.github.fmjsjx.libnetty.resp.RespMessages;
import com.github.fmjsjx.libnetty.resp.RespSimpleStringMessage;

import com.github.fmjsjx.libnetty.transport.io.NioIoTransportLibrary;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Test client.
 */
public class TestClient {

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        RespMessageEncoder respMessageEncoder = new RespMessageEncoder();
        var group = NioIoTransportLibrary.getInstance().createGroup();
        try {
            Bootstrap b = new Bootstrap().group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(respMessageEncoder).addLast(new DefaultRespMessageDecoder())
                                    .addLast(new TestClientHandler());
                        }
                    });
            ChannelFuture future = b.connect("127.0.0.1", 6379).sync();
            if (future.isSuccess()) {
                Channel channel = future.channel();
                channel.writeAndFlush(DefaultArrayMessage.bulkStringArrayAscii(channel.alloc(), "ECHO",
                        "Hello World!"));
                channel.closeFuture().sync();
            }
        } finally {
            group.shutdownGracefully();
        }
    }

    private TestClient() {
    }

}

class TestClientHandler extends SimpleChannelInboundHandler<RespMessage> {

    private int count = 0;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RespMessage msg) {
        System.out.println("-- RESP message received --");
        System.out.println(msg);
        if (msg instanceof RespBulkStringMessage || msg instanceof RespArrayMessage) {
            switch (count++) {
                case 0 -> ctx.writeAndFlush(command(ctx.alloc(), "SMEMBERS", "any key for test"));
                case 1 -> ctx.writeAndFlush(command(ctx.alloc(), "SMEMBERS", "3"));
                case 2 -> ctx.writeAndFlush(command(ctx.alloc(), "SMEMBERS", "12"));
                case 3 -> ctx.writeAndFlush(command(ctx.alloc(), "PING", "PING may same with ECHO"));
                case 4 -> ctx.writeAndFlush(command(ctx.alloc(), "GET", "https://www.baidu.com/"));
                case 5 -> ctx.writeAndFlush(command(ctx.alloc(), "GET", "https://www.sogou.com/"));
                default -> ctx.writeAndFlush(command(ctx.alloc(), "QUIT"));
            }
        } else if (msg instanceof RespSimpleStringMessage) {
            if (RespMessages.ok().value().equals(((RespSimpleStringMessage) msg).value())) {
                System.out.println("is OK");
                ctx.close();
            }
        }
    }

    private static RespMessage command(ByteBufAllocator alloc, String... commands) {
        return DefaultArrayMessage.bulkStringArrayUtf8(alloc, commands);
    }

}