package com.github.fmjsjx.libnetty.http.server.middleware;

import static io.netty.handler.codec.http.HttpMethod.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServerUtil;
import com.github.fmjsjx.libnetty.http.server.HttpServiceInvoker;

import io.netty.handler.codec.http.HttpMethod;

/**
 * A {@link Middleware} routing requests.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class Router implements Middleware {

    private static final int RUNNING = 1;

    private List<RouteDefinition> routeDefinitions = new ArrayList<>();

    private volatile PathRoute[] pathRoutes;

    private volatile int state;

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        if (state != RUNNING) {
            init();
        }
        return routing(ctx, next);
    }

    /**
     * Initial this {@link Router}.
     * 
     * @return this {@code Router}
     */
    public synchronized Router init() {
        if (state != RUNNING) {
            init0();
            state = RUNNING;
        }
        return this;
    }

    private void init0() {
        this.pathRoutes = routeDefinitions.stream()
                .collect(Collectors.groupingBy(RouteDefinition::path, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream().map(e -> {
                    PathMatcher pathMatcher = PathMatcher.fromPattern(e.getKey());
                    MethodRoute[] methodRoutes = e.getValue().stream().sorted().map(RouteDefinition::toMethodRoute)
                            .toArray(MethodRoute[]::new);
                    return new PathRoute(pathMatcher, methodRoutes);
                }).toArray(PathRoute[]::new);
    }

    private CompletionStage<HttpResult> routing(HttpRequestContext ctx, MiddlewareChain next) {
        boolean pathMatched = false;
        for (PathRoute pathRoute : pathRoutes) {
            if (pathRoute.matches(ctx)) {
                pathMatched = true;
                HttpMethod method = ctx.method();
                for (MethodRoute methodRoute : pathRoute.methodRoutes) {
                    if (methodRoute.matches(method)) {
                        return methodRoute.service.invoke(ctx);
                    }
                }
            }
        }
        if (pathMatched) {
            // throw 405 Method Not Allowed
            return HttpServerUtil.sendMethodNotAllowed(ctx);
        }
        return next.doNext(ctx);
    }

    /**
     * Add a new HTTP route with given parameters.
     * 
     * @param service an HTTP service
     * @param path    the path pattern of the service
     * @param methods the array of the allowed HTTP methods
     * @return this {@code Router}
     */
    public synchronized Router add(HttpServiceInvoker service, String path, HttpMethod... methods) {
        if (state == RUNNING) {
            throw new IllegalStateException("router is already initialized");
        }
        routeDefinitions.add(new RouteDefinition(path, methods, service));
        return this;
    }

    /**
     * Add a new HTTP route with given parameters.
     * 
     * @param path    the path pattern of the service
     * @param method  the HTTP method
     * @param service an HTTP service
     * @return this {@code Router}
     */
    public Router add(String path, HttpMethod method, HttpServiceInvoker service) {
        return add(service, path, method);
    }

    /**
     * Add a new "all method allowed" route with specified path and service.
     * 
     * @param path    the path pattern of the service
     * @param service an HTTP service
     * @return this {@code Router}
     */
    public Router add(String path, HttpServiceInvoker service) {
        return add(service, path);
    }

    /**
     * Add a new HTTP GET route with specified path and service.
     * 
     * @param path    the path pattern of the service
     * @param service an HTTP service
     * @return this {@code Router}
     */
    public Router get(String path, HttpServiceInvoker service) {
        return add(path, GET, service);
    }

    /**
     * Add a new HTTP POST route with specified path and service.
     * 
     * @param path    the path pattern of the service
     * @param service an HTTP service
     * @return this {@code Router}
     */
    public Router post(String path, HttpServiceInvoker service) {
        return add(path, POST, service);
    }

    /**
     * Add a new HTTP PUT route with specified path and service.
     * 
     * @param path    the path pattern of the service
     * @param service an HTTP service
     * @return this {@code Router}
     */
    public Router put(String path, HttpServiceInvoker service) {
        return add(path, PUT, service);
    }

    /**
     * Add a new HTTP PATCH route with specified path and service.
     * 
     * @param path    the path pattern of the service
     * @param service an HTTP service
     * @return this {@code Router}
     */
    public Router patch(String path, HttpServiceInvoker service) {
        return add(path, PATCH, service);
    }

    /**
     * Add a new HTTP DELETE route with specified path and service.
     * 
     * @param path    the path pattern of the service
     * @param service an HTTP service
     * @return this {@code Router}
     */
    public Router delete(String path, HttpServiceInvoker service) {
        return add(path, DELETE, service);
    }

    private static final class RouteDefinition implements Comparable<RouteDefinition> {

        private final String path;
        private final HttpMethod[] methods;
        private final HttpServiceInvoker service;

        private RouteDefinition(String path, HttpMethod[] methods, HttpServiceInvoker service) {
            this.path = path;
            this.methods = methods;
            this.service = service;
        }

        private String path() {
            return this.path;
        }

        private MethodMatcher toMethodMatcher() {
            if (methods.length == 0) {
                return MethodMatcher.any();
            }
            return MethodMatcher.in(methods);
        }

        private MethodRoute toMethodRoute() {
            return new MethodRoute(toMethodMatcher(), service);
        }

        @Override
        public int compareTo(RouteDefinition o) {
            if (methods.length == 0 && o.methods.length > 0) {
                return 1;
            } else if (methods.length > 0 && o.methods.length == 0) {
                return -1;
            }
            return 0;
        }

    }

    private static final class PathRoute {

        private final PathMatcher pathMatcher;
        private final MethodRoute[] methodRoutes;

        private PathRoute(PathMatcher pathMatcher, MethodRoute[] methodRoutes) {
            this.pathMatcher = pathMatcher;
            this.methodRoutes = methodRoutes;
        }

        private boolean matches(HttpRequestContext ctx) {
            return pathMatcher.matches(ctx);
        }

    }

    private static final class MethodRoute {

        private final MethodMatcher methodMatcher;
        private final HttpServiceInvoker service;

        private MethodRoute(MethodMatcher methodMatcher, HttpServiceInvoker service) {
            this.methodMatcher = methodMatcher;
            this.service = service;
        }

        private boolean matches(HttpMethod method) {
            return methodMatcher.matches(method);
        }

    }

}
