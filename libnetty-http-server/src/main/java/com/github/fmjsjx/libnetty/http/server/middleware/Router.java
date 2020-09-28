package com.github.fmjsjx.libnetty.http.server.middleware;

import static io.netty.handler.codec.http.HttpMethod.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 
 * @see Middleware
 * @see MiddlewareChain
 */
public class Router implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(Router.class);

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
        List<RouteDefinition> definitions = routeDefinitions;
        logger.debug("Initial router by definitions: {}", definitions);
        PathRoute[] pathRoutes = definitions.stream()
                .collect(Collectors.groupingBy(RouteDefinition::path, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream().map(e -> {
                    String path = e.getKey();
                    PathMatcher pathMatcher = PathMatcher.fromPattern(path);
                    MethodRoute[] methodRoutes = e.getValue().stream().sorted().map(RouteDefinition::toMethodRoute)
                            .toArray(MethodRoute[]::new);
                    return new PathRoute(path, pathMatcher, methodRoutes);
                }).toArray(PathRoute[]::new);
        if (logger.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder();
            for (PathRoute pathRoute : pathRoutes) {
                builder.append("\n").append(pathRoute.toString(true));
            }
            logger.debug("Effective routes: {}{}", pathRoutes.length, builder);
        }
        this.pathRoutes = pathRoutes;
    }

    private CompletionStage<HttpResult> routing(HttpRequestContext ctx, MiddlewareChain next) {
        HttpMethod method = ctx.method();
        String path = ctx.path();
        logger.trace("Routing: {} {}", method, path);
        boolean pathMatched = false;
        for (PathRoute pathRoute : pathRoutes) {
            logger.trace("Try {}", pathRoute);
            if (pathRoute.matches(ctx)) {
                pathMatched = true;
                for (MethodRoute methodRoute : pathRoute.methodRoutes) {
                    logger.trace("Try {}", method);
                    if (methodRoute.matches(method)) {
                        logger.debug("Matched Route ({} {}): {}", method, path, methodRoute);
                        return methodRoute.service.invoke(ctx);
                    }
                }
            }
        }
        if (pathMatched) {
            // throw 405 Method Not Allowed
            return HttpServerUtil.sendMethodNotAllowed(ctx);
        }
        logger.debug("Miss match for all routes: {} {}", method, path);
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

    /**
     * Register a controller.
     * <p>
     * This method is equivalent to:
     * 
     * <pre>
     * {@code ControllerBeanUtil.register(this, controller);}
     * </pre>
     * 
     * @param controller the controller object
     * @return this {@code Router}
     * 
     * @see ControllerBeanUtil#register(Router, Object)
     */
    public Router register(Object controller) {
        ControllerBeanUtil.register(this, controller);
        return this;
    }

    /**
     * Register a controller.
     * <p>
     * This method is equivalent to:
     * 
     * <pre>
     * {@code ControllerBeanUtil.register(this, controller, clazz);}
     * </pre>
     * 
     * @param <T>        the type of the controller
     * @param controller the controller object
     * @param clazz      the class of the type
     * @return this {@code Router}
     * 
     * @see ControllerBeanUtil#register(Router, Object, Class)
     */
    public <T> Router register(T controller, Class<T> clazz) {
        ControllerBeanUtil.register(this, controller, clazz);
        return this;
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

        private MethodRoute toMethodRoute() {
            return new MethodRoute(methods, service);
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

        @Override
        public String toString() {
            return "RouteDefintion[path=" + path + ", methods=" + Arrays.toString(methods) + "]";
        }

    }

    private static final class PathRoute {

        private final String path;
        private final PathMatcher pathMatcher;
        private final MethodRoute[] methodRoutes;

        private PathRoute(String path, PathMatcher pathMatcher, MethodRoute[] methodRoutes) {
            this.path = path;
            this.pathMatcher = pathMatcher;
            this.methodRoutes = methodRoutes;
        }

        private boolean matches(HttpRequestContext ctx) {
            return pathMatcher.matches(ctx);
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean withMethodRoutes) {
            StringBuilder builder = new StringBuilder();
            builder.append("PathRoute(").append(path).append("): ").append(methodRoutes.length)
                    .append(" method routes");
            if (withMethodRoutes) {
                for (MethodRoute methodRoute : methodRoutes) {
                    builder.append("\n\t").append(methodRoute);
                }
            }
            return builder.toString();
        }

    }

    private static final class MethodRoute {

        private final String methods;
        private final MethodMatcher methodMatcher;
        private final HttpServiceInvoker service;

        private MethodRoute(HttpMethod[] methods, HttpServiceInvoker service) {
            if (methods.length == 0) {
                this.methods = "<any>";
                this.methodMatcher = MethodMatcher.any();
            } else {
                this.methods = Arrays.stream(methods).map(HttpMethod::toString).distinct()
                        .collect(Collectors.joining("|")).intern();
                this.methodMatcher = MethodMatcher.in(methods);
            }
            this.service = service;
        }

        private boolean matches(HttpMethod method) {
            return methodMatcher.matches(method);
        }

        @Override
        public String toString() {
            return "MethodRoute(" + methods + ") -> " + service;
        }
    }

}
