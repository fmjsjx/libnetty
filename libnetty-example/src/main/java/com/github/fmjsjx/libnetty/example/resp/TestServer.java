package com.github.fmjsjx.libnetty.example.resp;

import static io.netty.channel.ChannelFutureListener.*;

import java.util.function.BiConsumer;

import com.github.fmjsjx.libnetty.resp.DefaultErrorMessage;
import com.github.fmjsjx.libnetty.resp.RedisRequest;
import com.github.fmjsjx.libnetty.resp.RedisRequestDecoder;
import com.github.fmjsjx.libnetty.resp.RespBulkStringMessage;
import com.github.fmjsjx.libnetty.resp.RespMessageEncoder;
import com.github.fmjsjx.libnetty.resp.RespMessages;
import com.github.fmjsjx.libnetty.resp.util.IgnoredCaseAsciiKeyMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class TestServer {

    public static void main(String[] args) throws Exception {
        RespMessageEncoder respMessageEncoder = new RespMessageEncoder();
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 512).childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.AUTO_READ, false).childHandler(new ChannelInitializer<Channel>() {
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new ReadTimeoutHandler(900)).addLast(respMessageEncoder)
                                    .addLast(new RedisRequestDecoder()).addLast(new TestServerHandler());
                        }
                    });
            b.bind(6369).sync();
            System.out.println("TestServer started!");
            System.in.read();

        } finally {
            group.shutdownGracefully();
        }
    }

}

class TestServerHandler extends SimpleChannelInboundHandler<RedisRequest> {

    private static final ChannelFutureListener READ_NEXT = f -> f.channel().read();

    private final IgnoredCaseAsciiKeyMap<BiConsumer<ChannelHandlerContext, RedisRequest>> commandProcedures;

    TestServerHandler() {
        commandProcedures = new IgnoredCaseAsciiKeyMap<>();
        commandProcedures.put("ECHO", this::echo);
        commandProcedures.put("PING", this::ping);
        commandProcedures.put("SELECT", this::justOk);
        commandProcedures.put("QUIT", this::quit);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.err.println(ctx.channel() + " connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.err.println(ctx.channel() + " disconnected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RedisRequest msg) throws Exception {
        System.out.println("-- message received --");
        System.out.println(msg);
        BiConsumer<ChannelHandlerContext, RedisRequest> procedure = commandProcedures.get(msg.command().content());
        if (procedure == null) {
            String cmd = msg.command().toText();
            ctx.writeAndFlush(DefaultErrorMessage.createErrAscii(ctx.alloc(), "unknown command `" + cmd + "`"))
                    .addListener(CLOSE);
        } else {
            procedure.accept(ctx, msg);
        }
    }

    private void echo(ChannelHandlerContext ctx, RedisRequest msg) {
        // always returns message
        if (msg.size() != 2) {
            ctx.writeAndFlush(RespMessages.wrongNumberOfArgumentsForCommand("echo")).addListener(READ_NEXT);
        } else {
            RespBulkStringMessage message = msg.argument(1);
            ctx.writeAndFlush(message.retainedDuplicate()).addListener(READ_NEXT);
        }
    }

    private void ping(ChannelHandlerContext ctx, RedisRequest msg) {
        int size = msg.size();
        if (msg.size() == 1) {
            ctx.writeAndFlush(RespMessages.pong()).addListener(READ_NEXT);
        } else if (size == 2) {
            ctx.writeAndFlush(msg.argument(1).retainedDuplicate()).addListener(READ_NEXT);
        } else {
            ctx.writeAndFlush(RespMessages.wrongNumberOfArgumentsForCommand("ping")).addListener(READ_NEXT);
        }
    }

    private void justOk(ChannelHandlerContext ctx, RedisRequest msg) {
        ctx.writeAndFlush(RespMessages.ok()).addListener(READ_NEXT);
    }

    private void quit(ChannelHandlerContext ctx, RedisRequest msg) {
        ctx.writeAndFlush(RespMessages.ok()).addListener(CLOSE);
    }

}
