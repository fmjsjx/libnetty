package com.github.fmjsjx.libnetty.http.server.component;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Component to support WebSocket feature.
 *
 * @author MJ Fang
 * @since 3.5
 */
public final class WebSocketSupport implements HttpServerComponent {

    /**
     * Returns the component key.
     *
     * @return the component key
     */
    public static Class<WebSocketSupport> componentKey() {
        return WebSocketSupport.class;
    }

    /**
     * Build a new {@link WebSocketSupport} with the specified parameters given.
     *
     * @param protocolConfig                the protocol config
     * @param webSocketFrameHandlerSupplier the web socket frame handler supplier
     * @return a new {@code WebSocketSupport} instance
     */
    public static WebSocketSupport build(WebSocketServerProtocolConfig protocolConfig, Supplier<? extends ChannelHandler> webSocketFrameHandlerSupplier) {
        return new WebSocketSupport(protocolConfig, webSocketFrameHandlerSupplier);
    }

    private final WebSocketServerProtocolConfig protocolConfig;

    private final Supplier<? extends ChannelHandler> webSocketFrameHandlerSupplier;

    private WebSocketSupport(WebSocketServerProtocolConfig protocolConfig, Supplier<? extends ChannelHandler> webSocketFrameHandlerSupplier) {
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "protocolConfig MUST not be null");
        this.webSocketFrameHandlerSupplier = Objects.requireNonNull(webSocketFrameHandlerSupplier, "webSocketFrameHandlerSupplier MUST not be null");
    }

    @Override
    public Class<WebSocketSupport> componentType() {
        return componentKey();
    }

    /**
     * Returns the protocol config.
     *
     * @return the protocol config
     */
    public WebSocketServerProtocolConfig protocolConfig() {
        return protocolConfig;
    }

    /**
     * Returns a web socket frame handler.
     *
     * @return a web socket frame handler
     */
    public ChannelHandler supplyWebSocketFrameHandler() {
        return webSocketFrameHandlerSupplier.get();
    }

}
