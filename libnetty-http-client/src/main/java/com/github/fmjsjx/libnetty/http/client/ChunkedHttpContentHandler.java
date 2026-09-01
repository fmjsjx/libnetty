package com.github.fmjsjx.libnetty.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A handler for chunked HTTP content conversions.
 *
 * @param <T> type of the content
 * @author MJ Fang
 * @since 4.3
 */
public interface ChunkedHttpContentHandler<T> extends Consumer<ByteBuf>, Supplier<T>, HttpContentHandler<T> {

    /**
     * Called when the channel is bound.
     *
     * @param channel the channel
     */
    void onBind(Channel channel);

    @Override
    T get();

    /**
     * Handle the chunk of the HTTP content.
     *
     * @param content the chunk of the HTTP content
     */
    @Override
    void accept(ByteBuf content);

    /**
     * Handle the chunk of the HTTP content and return the response body
     * object
     *
     * @return the response body object
     */
    @Override
    default T apply(ByteBuf content) {
        var t = get();
        accept(content);
        onComplete();
        return t;
    }

    /**
     * Called when the HTTP content is completed.
     */
    default void onComplete() {
    }

    /**
     * Called when the HTTP content is failed.
     *
     * @param cause the cause
     */
    default void onError(Throwable cause) {
    }

}
