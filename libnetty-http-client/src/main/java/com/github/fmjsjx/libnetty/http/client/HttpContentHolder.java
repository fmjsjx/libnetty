package com.github.fmjsjx.libnetty.http.client;

import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Holds an HTTP content.
 *
 * @param <T> base type of the content
 * 
 * @author MJ Fang
 * 
 * @since 1.0
 * 
 * @see HttpContentHolders
 */
public abstract class HttpContentHolder<T> {

    private final T base;

    private ByteBuf content;

    protected HttpContentHolder(T base) {
        this.base = Objects.requireNonNull(base, "base must not be null");
    }

    protected abstract ByteBuf encode(ByteBufAllocator alloc, T base);

    ByteBuf content(ByteBufAllocator alloc) {
        if (content == null) {
            content = encode(alloc, base);
        }
        return content;
    }

    /**
     * Returns the base data.
     *
     * @return the base data
     */
    public T base() {
        return base;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(base=" + base + ")";
    }

}
