package com.github.fmjsjx.libnetty.resp3;

import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_LENGTH;
import static com.github.fmjsjx.libnetty.resp.RespConstants.EOL_SHORT;
import static com.github.fmjsjx.libnetty.resp.RespConstants.TYPE_LENGTH;

import com.github.fmjsjx.libnetty.resp.CachedRespMessage;
import com.github.fmjsjx.libnetty.resp.RespCodecUtil;
import com.github.fmjsjx.libnetty.resp.RespMessage;
import com.github.fmjsjx.libnetty.resp.RespMessageType;

/**
 * The cached unbound aggregate header message.
 *
 * @since 1.1
 *
 * @author MJ Fang
 */
public class CachedUnboundAggregateHeaderMessage extends CachedRespMessage implements RespMessage {

    /**
     * Singleton instance of unbound array.
     */
    public static final CachedUnboundAggregateHeaderMessage UNBOUND_ARRAY = new CachedUnboundAggregateHeaderMessage(
            RespMessageType.ARRAY);
    /**
     * Singleton instance of unbound map.
     */
    public static final CachedUnboundAggregateHeaderMessage UNBOUND_MAP = new CachedUnboundAggregateHeaderMessage(
            Resp3MessageType.MAP);
    /**
     * Singleton instance of unbound set.
     */
    public static final CachedUnboundAggregateHeaderMessage UNBOUND_SET = new CachedUnboundAggregateHeaderMessage(
            Resp3MessageType.SET);
    /**
     * Singleton instance of unbound push.
     */
    public static final CachedUnboundAggregateHeaderMessage UNBOUND_PUSH = new CachedUnboundAggregateHeaderMessage(
            Resp3MessageType.PUSH);

    private final RespMessageType type;

    private CachedUnboundAggregateHeaderMessage(RespMessageType type) {
        super(RespCodecUtil.buffer(TYPE_LENGTH + 1 + EOL_LENGTH).writeByte(type.value()).writeByte('?')
                .writeShort(EOL_SHORT));
        this.type = type;
    }

    @Override
    public RespMessageType type() {
        return type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type + "?]";
    }

}
