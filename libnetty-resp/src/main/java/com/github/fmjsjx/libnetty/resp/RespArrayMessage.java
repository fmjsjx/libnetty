package com.github.fmjsjx.libnetty.resp;

import java.util.List;

public interface RespArrayMessage extends RespMessage {

    int size();

    List<? extends RespMessage> values();

    @SuppressWarnings("unchecked")
    default <M extends RespMessage> M value(int index) {
        return (M) values().get(index);
    }

    default RespBulkStringMessage bulkString(int index) {
        return value(index);
    }

}
