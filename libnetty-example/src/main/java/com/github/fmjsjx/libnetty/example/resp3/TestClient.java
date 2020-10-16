package com.github.fmjsjx.libnetty.example.resp3;

import com.github.fmjsjx.libnetty.resp.DefaultArrayMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;
import com.github.fmjsjx.libnetty.resp.RespMessageEncoder;
import com.github.fmjsjx.libnetty.resp.RespMessages;
import com.github.fmjsjx.libnetty.resp.RespSimpleStringMessage;
import com.github.fmjsjx.libnetty.resp3.Resp3MessageDecoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TestClient {

    public static void main(String[] args) throws Exception {
        RespMessageEncoder respMessageEncoder = new RespMessageEncoder();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap().group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(respMessageEncoder).addLast(new Resp3MessageDecoder())
                                    .addLast(new TestClientHandler());
                        }
                    });
            ChannelFuture future = b.connect("127.0.0.1", 6379).sync();
            if (future.isSuccess()) {
                Channel channel = future.channel();
                channel.writeAndFlush(DefaultArrayMessage.bulkStringArrayAscii(channel.alloc(), "HELLO", "3"));
                channel.closeFuture().sync();
            }
        } finally {
            group.shutdownGracefully();
        }
    }

}

class TestClientHandler extends SimpleChannelInboundHandler<RespMessage> {

    private int count = 0;
    private boolean quited = false;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RespMessage msg) throws Exception {
        System.out.println("-- RESP message received --");
        System.out.println(msg);
        if (quited && msg instanceof RespSimpleStringMessage
                && RespMessages.ok().value().equals(((RespSimpleStringMessage) msg).value())) {
            System.out.println("QUIT is OK");
            ctx.close();
            return;
        }
        switch (count++) {
        case 0:
            ctx.writeAndFlush(command(ctx.alloc(), "ECHO", "Hello World!"));
            break;
        case 1:
            ctx.writeAndFlush(command(ctx.alloc(), "SMEMBERS", "any key for test"));
            break;
        case 2:
            ctx.writeAndFlush(command(ctx.alloc(), "SMEMBERS", "12"));
            break;
        case 3:
            ctx.writeAndFlush(command(ctx.alloc(), "HGETALL", "any key for test"));
            break;
        case 4:
            ctx.writeAndFlush(command(ctx.alloc(), "PING", "PING may same with ECHO"));
            break;
        case 5:
            ctx.writeAndFlush(command(ctx.alloc(), "GET", "https://www.baidu.com/"));
            break;
        case 6:
            ctx.writeAndFlush(command(ctx.alloc(), "GET", "https://www.sogou.com/"));
            break;
        default:
            quited = true;
            ctx.writeAndFlush(command(ctx.alloc(), "QUIT"));
            break;
        }
    }

    private static RespMessage command(ByteBufAllocator alloc, String... commands) {
        return DefaultArrayMessage.bulkStringArrayUtf8(alloc, commands);
    }

}