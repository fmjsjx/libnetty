package com.github.fmjsjx.libnetty.resp;

import java.util.List;
import java.util.stream.Stream;

import io.netty.buffer.ByteBufAllocator;

public class RedisRequest implements RespArrayMessage {

    private final RespArrayMessage array;

    public RedisRequest(RespArrayMessage array) {
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
    public List<? extends RespMessage> values() {
        return array.values();
    }

    public RespBulkStringMessage command() {
        return argument(0);
    }

    public RespBulkStringMessage command(int index) {
        return argument(index);
    }

    public RespBulkStringMessage argument(int index) {
        return array.value(index);
    }

    public Stream<RespBulkStringMessage> arguments(int beginIndex) {
        return array.values().stream().skip(beginIndex).map(RespBulkStringMessage.class::cast);
    }

    public Stream<RespBulkStringMessage> arguments() {
        return arguments(1);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + array + "}";
    }

}
