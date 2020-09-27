package com.github.fmjsjx.libnetty.http.server.middleware;

import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.BiPredicate;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResponseUtil;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServerUtil;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.AsciiString;

/**
 * A {@link Middleware} allows limiting access to resources by validating the
 * user name and password using the "HTTP Basic Authentication" protocol.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 * 
 * @see Middleware
 */
public class AuthBasic implements Middleware {

    private final BiPredicate<String, String> validator;
    private final CharSequence basicRealm;

    public AuthBasic(BiPredicate<String, String> validator, String realm) {
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.basicRealm = AsciiString.cached("Basic realm=\"" + realm + "\"");
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        String authorization = ctx.headers().getAsString(HttpHeaderNames.AUTHORIZATION);
        if (authorization.startsWith("Basic ")) {
            String base64 = authorization.substring(6);
            byte[] auth = Base64.getDecoder().decode(base64);
            int index = -1;
            for (int i = 0; i < auth.length; i++) {
                if (auth[i] == ':') {
                    index = i;
                }
            }
            String name, pwd;
            if (index == -1) {
                name = new String(auth);
                pwd = null;
            } else {
                int nameLength = index;
                int pwdLength = auth.length - index - 1;
                int pwdIndex = index + 1;
                if (nameLength == 0) {
                    name = "";
                    pwd = new String(auth, pwdIndex, pwdLength);
                } else if (pwdLength == 0) {
                    name = new String(auth, 0, nameLength);
                    pwd = "";
                } else {
                    name = new String(auth, 0, nameLength);
                    pwd = new String(auth, pwdIndex, pwdLength);
                }
            }
            if (validator.test(name, pwd)) {
                return next.doNext(ctx);
            }
        }
        // validation failure
        FullHttpRequest request = ctx.request();
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response = HttpResponseUtil.create(request.protocolVersion(), HttpResponseStatus.UNAUTHORIZED,
                keepAlive);
        response.headers().set(HttpHeaderNames.WWW_AUTHENTICATE, basicRealm);
        return HttpServerUtil.sendResponse(ctx, response, 0, keepAlive);
    }

}
