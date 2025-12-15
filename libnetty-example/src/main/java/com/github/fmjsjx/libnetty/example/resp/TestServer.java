package com.github.fmjsjx.libnetty.example.resp;

import static io.netty.channel.ChannelFutureListener.CLOSE;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.client.HttpClient;
import com.github.fmjsjx.libnetty.http.client.SimpleHttpClient;
import com.github.fmjsjx.libnetty.resp.CachedBulkStringMessage;
import com.github.fmjsjx.libnetty.resp.DefaultArrayMessage;
import com.github.fmjsjx.libnetty.resp.DefaultBulkStringMessage;
import com.github.fmjsjx.libnetty.resp.DefaultErrorMessage;
import com.github.fmjsjx.libnetty.resp.RedisRequest;
import com.github.fmjsjx.libnetty.resp.RedisRequestDecoder;
import com.github.fmjsjx.libnetty.resp.RespBulkStringMessage;
import com.github.fmjsjx.libnetty.resp.RespMessage;
import com.github.fmjsjx.libnetty.resp.RespMessageEncoder;
import com.github.fmjsjx.libnetty.resp.RespMessages;
import com.github.fmjsjx.libnetty.resp.util.IgnoredCaseAsciiKeyMap;

import com.github.fmjsjx.libnetty.transport.io.NioIoTransportLibrary;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

/**
 * Test server
 */
public class TestServer {

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        var respMessageEncoder = new RespMessageEncoder();
        var group = NioIoTransportLibrary.getInstance().createGroup();
        try {
            var b = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 512).childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.AUTO_READ, false).childHandler(new ChannelInitializer<>() {
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(respMessageEncoder).addLast(new RedisRequestDecoder())
                                    .addLast(new TestServerHandler());
                        }
                    });
            b.bind(6379).sync();
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

class TestServerHandler extends SimpleChannelInboundHandler<RedisRequest> {

    private static final SslContextProvider INSECURE_FOR_CLIENT = SslContextProviders.insecureForClient();

    private static final ChannelFutureListener READ_NEXT = f -> f.channel().read();

    private final IgnoredCaseAsciiKeyMap<BiConsumer<ChannelHandlerContext, RedisRequest>> commandProcedures;

    TestServerHandler() {
        commandProcedures = new IgnoredCaseAsciiKeyMap<>();
        commandProcedures.put("GET", this::get);
        commandProcedures.put("ECHO", this::echo);
        commandProcedures.put("SMEMBERS", this::smembers);
        commandProcedures.put("PING", this::ping);
        commandProcedures.put("SELECT", this::justOk);
        commandProcedures.put("QUIT", this::quit);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.err.println(ctx.channel() + " connected");
        ctx.read();
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
    protected void channelRead0(ChannelHandlerContext ctx, RedisRequest msg) {
        System.out.println("-- message received --");
        System.out.println(msg);
        BiConsumer<ChannelHandlerContext, RedisRequest> procedure = commandProcedures.get(msg.command().content());
        if (procedure == null) {
            String cmd = msg.command().toText();
            ctx.writeAndFlush(DefaultErrorMessage.createErr("unknown command `" + cmd + "`")).addListener(CLOSE);
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

    private final List<RespBulkStringMessage> smembers = Arrays.stream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 })
            .mapToObj(CachedBulkStringMessage::create).collect(Collectors.toList());

    private void smembers(ChannelHandlerContext ctx, RedisRequest msg) {
        if (msg.size() < 2) {
            ctx.writeAndFlush(RespMessages.wrongNumberOfArgumentsForCommand("smembers")).addListener(READ_NEXT);
        } else {
            try {
                int size = Math.max(0, Math.min(msg.argument(1).intValue(), 9));
                ctx.writeAndFlush(new DefaultArrayMessage<>(smembers.stream().limit(size).toArray(RespMessage[]::new)))
                        .addListener(READ_NEXT);
            } catch (Exception e) {
                ctx.writeAndFlush(RespMessages.emptyArray()).addListener(READ_NEXT);
            }
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

    private void get(ChannelHandlerContext ctx, RedisRequest msg) {
        Channel channel = ctx.channel();
        String path = msg.argument(1).textValue(CharsetUtil.UTF_8);
        try (HttpClient client = SimpleHttpClient.builder().sslContextProvider(INSECURE_FOR_CLIENT)
                .build(channel.eventLoop(), channel.getClass())) {
            client.request(URI.create(path)).get().sendAsync(ByteBuf::retainedDuplicate).thenAccept(r -> {
                if (r.statusCode() >= 400) {
                    channel.writeAndFlush(DefaultErrorMessage.createErr(r.status().toString())).addListener(READ_NEXT);
                } else {
                    channel.writeAndFlush(new DefaultBulkStringMessage(r.content())).addListener(READ_NEXT);
                }
            }).whenComplete((v, e) -> {
                if (e != null) {
                    if (e instanceof CompletionException) {
                        e = e.getCause();
                    }
                    channel.writeAndFlush(DefaultErrorMessage.createErr(e.toString())).addListener(READ_NEXT);
                }
            });
        }
    }

    private void justOk(ChannelHandlerContext ctx, RedisRequest msg) {
        ctx.writeAndFlush(RespMessages.ok()).addListener(READ_NEXT);
    }

    private void quit(ChannelHandlerContext ctx, RedisRequest msg) {
        ctx.writeAndFlush(RespMessages.ok()).addListener(CLOSE);
    }

}
