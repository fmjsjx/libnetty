package com.github.fmjsjx.libnetty.example.resp;

import com.github.fmjsjx.libnetty.resp.DefaultArrayMessage;
import com.github.fmjsjx.libnetty.resp.DefaultRespMessageDecoder;
import com.github.fmjsjx.libnetty.resp.RespBulkStringMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;
import com.github.fmjsjx.libnetty.resp.RespMessageEncoder;
import com.github.fmjsjx.libnetty.resp.RespMessages;
import com.github.fmjsjx.libnetty.resp.RespSimpleStringMessage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

public class TestClient {

    public static void main(String[] args) throws Exception {
        RespMessageEncoder respMessageEncoder = new RespMessageEncoder();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap().group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(respMessageEncoder).addLast(new DefaultRespMessageDecoder())
                                    .addLast(new TestClientHandler());
                        }
                    });
            ChannelFuture future = b.connect("127.0.0.1", 6379).sync();
            if (future.isSuccess()) {
                Channel channel = future.channel();
                DefaultArrayMessage cmd = DefaultArrayMessage.bulkStringArrayAscii(channel.alloc(), "ECHO",
                        "Hello World!");
                channel.writeAndFlush(cmd);
                channel.closeFuture().sync();
            }
        } finally {
            group.shutdownGracefully();
        }
    }

}

class TestClientHandler extends SimpleChannelInboundHandler<RespMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RespMessage msg) throws Exception {
        System.out.println("-- RESP message received --");
        System.out.println(msg);
        if (msg instanceof RespBulkStringMessage) {
            System.out.println(((RespBulkStringMessage) msg).textValue(CharsetUtil.UTF_8));
        } else if (msg instanceof RespSimpleStringMessage) {
            if (RespMessages.ok().value().equals(((RespSimpleStringMessage) msg).value())) {
                System.out.println("is OK");
                ctx.close();
            }
        }
        ctx.writeAndFlush(DefaultArrayMessage.bulkStringArrayAscii(ctx.alloc(), "QUIT"));
    }

}