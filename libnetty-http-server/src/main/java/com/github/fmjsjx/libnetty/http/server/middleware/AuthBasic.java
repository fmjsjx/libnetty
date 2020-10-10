package com.github.fmjsjx.libnetty.http.server.middleware;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaderNames.WWW_AUTHENTICATE;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.BiPredicate;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServer.AbstractUser;
import com.github.fmjsjx.libnetty.http.server.HttpServer.User;

import io.netty.handler.codec.http.FullHttpResponse;
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

    /**
     * A {@link User} holding the username parsed from the "HTTP Basic
     * Authentication" protocol.
     * 
     * @since 1.1
     *
     * @author MJ Fang
     */
    public static final class BasicUser extends AbstractUser {

        /**
         * Constructs a new {@link BasicUser} instance with the specified
         * {@code username}.
         * 
         * @param username the username parsed from the "HTTP Basic Authentication"
         *                 protocol
         */
        public BasicUser(String username) {
            super(username);
        }

    }

    private final BiPredicate<String, String> validator;
    private final CharSequence basicRealm;

    /**
     * Constructs a new {@link AuthBasic} with the specified users and realm.
     * 
     * @param users a map contains the users' name and their password
     * @param realm the realm attribute
     */
    public AuthBasic(Map<String, String> users, String realm) {
        this((n, p) -> {
            String pwd = users.get(n);
            if (pwd != null) {
                return pwd.equals(p);
            }
            return false;
        }, realm);
    }

    /**
     * Constructs a new {@link AuthBasic} with the specified validator and realm.
     * 
     * @param validator function to validating the user name and password
     * @param realm     the realm attribute
     */
    public AuthBasic(BiPredicate<String, String> validator, String realm) {
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        Objects.requireNonNull(realm, "realm must not be null");
        this.basicRealm = AsciiString.cached("Basic realm=\"" + realm + "\"");
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        String authorization = ctx.headers().getAsString(AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Basic ")) {
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
                ctx.property(User.KEY, new BasicUser(name));
                return next.doNext(ctx);
            }
        }
        // validation failure
        FullHttpResponse response = ctx.responseFactory().createFull(UNAUTHORIZED);
        response.headers().set(WWW_AUTHENTICATE, basicRealm);
        return ctx.sendResponse(response, 0);
    }

    @Override
    public String toString() {
        return "AuthBasic(" + basicRealm + ")";
    }

}
