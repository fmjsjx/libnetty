package com.github.fmjsjx.libnetty.http.server;

/**
 * The default implementation of {@link HttpServerHandlerProvider}.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class DefaultHttpServerHandlerProvider implements HttpServerHandlerProvider {

    private volatile DefaultHttpServerHandler value;

    @Override
    public DefaultHttpServerHandler get() {
        DefaultHttpServerHandler value = this.value;
        if (value == null) {
            synchronized (this) {
                if ((value = this.value) == null) {
                    this.value = value = initHandler();
                }
            }
        }
        return value;
    }

    private DefaultHttpServerHandler initHandler() {
        // TODO Auto-generated method stub
        return null;
    }

}
