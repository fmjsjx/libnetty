package com.github.fmjsjx.libnetty.resp;

import java.util.List;
import java.util.stream.Stream;

import io.netty.buffer.ByteBufAllocator;

/**
 * A REDIS request.
 * 
 * <p>
 * According to the official Redis documentation <a href=
 * "https://redis.io/topics/protocol#sending-commands-to-a-redis-server">Redis
 * Protocol specification</a>:
 * <ul>
 * <li><i> A client sends the Redis server a RESP Array consisting of just Bulk
 * Strings.</i></li>
 * </ul>
 * 
 * @since 1.0
 *
 * @author MJ Fang
 * 
 * @see RespArrayMessage
 * @see DefaultArrayMessage
 */
public class RedisRequest implements RespArrayMessage<RespBulkStringMessage> {

    private final RespArrayMessage<RespBulkStringMessage> array;

    /**
     * Constructs a new {@link RedisRequest} with the specified RESP array.
     * 
     * @param array a {@code RespArrayMessage}
     */
    public RedisRequest(RespArrayMessage<RespBulkStringMessage> array) {
        this.array = array;
    }

    @Override
    public RespMessageType type() {
        return array.type();
    }

    @Override
    public void encode(ByteBufAllocator alloc, List<Object> out) throws Exception {
        array.encode(alloc, out);
    }

    @Override
    public int refCnt() {
        return array.refCnt();
    }

    @Override
    public RedisRequest retain() {
        array.retain();
        return this;
    }

    @Override
    public RedisRequest retain(int increment) {
        array.retain(increment);
        return this;
    }

    @Override
    public RedisRequest touch() {
        array.touch();
        return this;
    }

    @Override
    public RedisRequest touch(Object hint) {
        array.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return array.release();
    }

    @Override
    public boolean release(int decrement) {
        return array.release(decrement);
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public List<RespBulkStringMessage> values() {
        return array.values();
    }

    /**
     * Returns the command (first bulk string) of this request.
     * 
     * <p>
     * This method is equivalent to:
     * 
     * <pre>
     * {@code
     *     return argument(0);
     * }
     * </pre>
     * 
     * @return a {@code RespBulkStringMessage}
     */
    public RespBulkStringMessage command() {
        return argument(0);
    }

    /**
     * Returns the bulk string at the specified position in this request.
     * 
     * <p>
     * This method is equivalent to:
     * 
     * <pre>
     * {@code
     *     return argument(index);
     * }
     * </pre>
     * 
     * @param index index of the value to return
     * @return a {@code RespBulkStringMessage}
     */
    public RespBulkStringMessage command(int index) {
        return argument(index);
    }

    /**
     * Returns the bulk string at the specified position in this request.
     * 
     * <p>
     * This method is equivalent to:
     * 
     * @param index index of the value to return
     * @return a {@code RespBulkStringMessage}
     */
    public RespBulkStringMessage argument(int index) {
        return array.value(index);
    }

    /**
     * Returns a stream consisting of the bulk strings of this request after
     * discarding the first {@code n} bulk strings of this request.
     * 
     * @param beginIndex index of the first returned value
     * @return a {@code Stream<RespBulkStringMessage>}
     */
    public Stream<RespBulkStringMessage> arguments(int beginIndex) {
        return array.values().stream().skip(beginIndex).map(RespBulkStringMessage.class::cast);
    }

    /**
     * Returns a stream consisting of the remaining bulk strings of this request
     * after discarding the first {@code 1} bulk string of this request.
     * 
     * <p>
     * This method is equivalent to:
     * 
     * <pre>
     * {@code
     *     return arguments(1);
     * }
     * </pre>
     * 
     * @return a {@code Stream<RespBulkStringMessage>}
     */
    public Stream<RespBulkStringMessage> arguments() {
        return arguments(1);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + array + "}";
    }

}
