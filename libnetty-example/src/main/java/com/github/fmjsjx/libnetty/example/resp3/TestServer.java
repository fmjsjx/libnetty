package com.github.fmjsjx.libnetty.example.resp3;

import static io.netty.channel.ChannelFutureListener.CLOSE;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.github.fmjsjx.libnetty.handler.ssl.SslContextProvider;
import com.github.fmjsjx.libnetty.handler.ssl.SslContextProviders;
import com.github.fmjsjx.libnetty.http.client.SimpleHttpClient;
import com.github.fmjsjx.libnetty.resp.CachedBulkStringMessage;
import com.github.fmjsjx.libnetty.resp.CachedErrorMessage;
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
import com.github.fmjsjx.libnetty.resp3.CachedBooleanMessage;
import com.github.fmjsjx.libnetty.resp3.DefaultDoubleMessage;
import com.github.fmjsjx.libnetty.resp3.DefaultMapMessage;
import com.github.fmjsjx.libnetty.resp3.DefaultSetMessage;
import com.github.fmjsjx.libnetty.resp3.FieldValuePair;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

/**
 * Test server.
 */
public class TestServer {

    /**
     * Main method.
     *
     * @param args main arguments
     * @throws Exception any error occurs
     */
    public static void main(String[] args) throws Exception {
        RespMessageEncoder respMessageEncoder = new RespMessageEncoder();
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 512).childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.AUTO_READ, false).childHandler(new ChannelInitializer<>() {
                        protected void initChannel(Channel ch) throws Exception {
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

}

class TestServerHandler extends SimpleChannelInboundHandler<RedisRequest> {

    private static final SslContextProvider INSECURE_FOR_CLIENT = SslContextProviders.insecureForClient();

    private static final ChannelFutureListener READ_NEXT = f -> f.channel().read();

    private static final CachedErrorMessage PROTO_ERROR = CachedErrorMessage
            .createAscii("NOPROTO sorry this protocol version is not supported");

    private static final CachedBulkStringMessage SERVER_KEY = CachedBulkStringMessage.createAscii("server");
    private static final CachedBulkStringMessage SERVER_VALUE = CachedBulkStringMessage.createAscii("test");
    private static final CachedBulkStringMessage VERSION_KEY = CachedBulkStringMessage.createAscii("version");
    private static final CachedBulkStringMessage VERSION_VALUE = CachedBulkStringMessage.createAscii("1.0.0");
    private static final CachedBulkStringMessage PROTO_KEY = CachedBulkStringMessage.createAscii("proto");

    private final IgnoredCaseAsciiKeyMap<BiConsumer<ChannelHandlerContext, RedisRequest>> commandProcedures;

    private boolean supportResp3;

    TestServerHandler() {
        commandProcedures = new IgnoredCaseAsciiKeyMap<>();
        commandProcedures.put("HELLO", this::hello);
        commandProcedures.put("GET", this::get);
        commandProcedures.put("HGETALL", this::hgetall);
        commandProcedures.put("ECHO", this::echo);
        commandProcedures.put("SMEMBERS", this::smembers);
        commandProcedures.put("PING", this::ping);
        commandProcedures.put("SELECT", this::justOk);
        commandProcedures.put("QUIT", this::quit);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.err.println(ctx.channel() + " connected");
        ctx.read();
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
            ctx.writeAndFlush(DefaultErrorMessage.createErr("unknown command `" + cmd + "`")).addListener(CLOSE);
        } else {
            procedure.accept(ctx, msg);
        }
    }

    private void hello(ChannelHandlerContext ctx, RedisRequest msg) {
        if (msg.size() < 2) {
            ctx.writeAndFlush(RespMessages.wrongNumberOfArgumentsForCommand("echo")).addListener(READ_NEXT);
        } else {
            try {
                int version = msg.argument(1).intValue();
                switch (version) {
                    case 2 -> {
                        DefaultArrayMessage<RespMessage> array = new DefaultArrayMessage<>(SERVER_KEY, SERVER_VALUE,
                                VERSION_KEY, VERSION_VALUE, PROTO_KEY, RespMessages.integer(2));
                        ctx.writeAndFlush(array).addListener(READ_NEXT);
                    }
                    case 3 -> {
                        supportResp3 = true;
                        DefaultMapMessage<RespBulkStringMessage, RespMessage> map = new DefaultMapMessage<>();
                        map.put(SERVER_KEY, SERVER_VALUE);
                        map.put(VERSION_KEY, VERSION_VALUE);
                        map.put(PROTO_KEY, RespMessages.integer(3));
                        map.put(DefaultBulkStringMessage.createAscii(ctx.alloc(), "testMode"), CachedBooleanMessage.TRUE);
                        ZonedDateTime now = ZonedDateTime.now();
                        String unixTime = now.toEpochSecond() + "." + now.getNano() / 1_000_000;
                        map.put(DefaultBulkStringMessage.createAscii(ctx.alloc(), "unixTime"), new DefaultDoubleMessage(unixTime));
                        ctx.writeAndFlush(map).addListener(READ_NEXT);
                    }
                    default -> throw new Exception(); // protocol version error
                }
            } catch (Exception e) {
                ctx.writeAndFlush(PROTO_ERROR).addListener(READ_NEXT);
            }
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
        if (msg.size() != 2) {
            ctx.writeAndFlush(RespMessages.wrongNumberOfArgumentsForCommand("smembers")).addListener(READ_NEXT);
        } else {
            try {
                int size = Math.max(0, Math.min(msg.argument(1).intValue(), 9));
                if (supportResp3) {
                    ctx.writeAndFlush(new DefaultSetMessage<>(smembers.subList(0, size))).addListener(READ_NEXT);
                } else {
                    ctx.writeAndFlush(new DefaultArrayMessage<>(smembers.subList(0, size))).addListener(READ_NEXT);
                }
            } catch (Exception e) {
                ctx.writeAndFlush(RespMessages.emptyArray()).addListener(READ_NEXT);
            }
        }
    }

    private final List<FieldValuePair<RespBulkStringMessage, RespBulkStringMessage>> hgetall = Arrays.asList(
            new FieldValuePair<>(CachedBulkStringMessage.createAscii("name"),
                    CachedBulkStringMessage.createAscii("test")),
            new FieldValuePair<>(CachedBulkStringMessage.createAscii("value"),
                    CachedBulkStringMessage.createAscii("Hello World!")));

    private void hgetall(ChannelHandlerContext ctx, RedisRequest msg) {
        if (msg.size() != 2) {
            ctx.writeAndFlush(RespMessages.wrongNumberOfArgumentsForCommand("smembers")).addListener(READ_NEXT);
        } else {
            if (supportResp3) {
                ctx.writeAndFlush(new DefaultMapMessage<>(hgetall)).addListener(READ_NEXT);
            } else {
                List<RespBulkStringMessage> array = new ArrayList<>();
                for (FieldValuePair<RespBulkStringMessage, RespBulkStringMessage> pair : hgetall) {
                    array.add(pair.field());
                    array.add(pair.value());
                }
                ctx.writeAndFlush(new DefaultArrayMessage<>(array)).addListener(READ_NEXT);
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
        var channel = ctx.channel();
        var path = msg.argument(1).textValue(CharsetUtil.UTF_8);
        try (var client = SimpleHttpClient.builder().sslContextProvider(INSECURE_FOR_CLIENT)
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
