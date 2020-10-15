package com.github.fmjsjx.libnetty.resp3;

import io.netty.util.ReferenceCounted;

/**
 * An interface defines a RESP3 Map message. Combines the {@link Resp3Message}
 * and the {@link ReferenceCounted}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public interface Resp3MapMessage extends Resp3Message, ReferenceCounted {

    
    
}
