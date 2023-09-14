package com.github.fmjsjx.libnetty.http.server.middleware;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.fmjsjx.libcommon.util.StringUtil;
import com.github.fmjsjx.libnetty.http.server.PathPatternUtil;
import io.netty.util.collection.IntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServiceInvoker;

import io.netty.handler.codec.http.HttpMethod;

/**
 * A {@link Middleware} routing requests.
 *
 * @author MJ Fang
 * @see Middleware
 * @see MiddlewareChain
 * @since 1.1
 */
public class Router implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private static final int RUNNING = 1;

    private final List<RouteDefinition> routeDefinitions = new ArrayList<>();

    private final List<RouteDefinition> placeholderRouteDefinitions = new ArrayList<>();

    private volatile int state;

    private volatile RoutingPolicy routingPolicy = RoutingPolicy.SIMPLE;

    private volatile ServiceRouter serviceRouter;

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
        if (routingPolicy == null) {
            if (routeDefinitions.size() + placeholderRouteDefinitions.size() <= 8) {
                routingPolicy = RoutingPolicy.SIMPLE;
            } else {
                routingPolicy = RoutingPolicy.TREE_MAP;
            }
        }
        serviceRouter = switch (routingPolicy) {
            case SIMPLE -> new SimpleServiceRouter(routeDefinitions, placeholderRouteDefinitions);
            case TREE_MAP -> new TreeMapServiceRouter(routeDefinitions, placeholderRouteDefinitions);
        };
    }

    private CompletionStage<HttpResult> routing(HttpRequestContext ctx, MiddlewareChain next) {
        return serviceRouter.routing(ctx, next);
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
        var routeDefinition = new RouteDefinition(path, methods, service);
        if (path.contains("{")) {
            placeholderRouteDefinitions.add(routeDefinition);
        } else {
            routeDefinitions.add(routeDefinition);
        }
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
     * @see RouterUtil#register(Router, Object)
     */
    public Router register(Object controller) {
        RouterUtil.register(this, controller);
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
     * @see RouterUtil#register(Router, Object, Class)
     */
    public <T> Router register(T controller, Class<T> clazz) {
        RouterUtil.register(this, controller, clazz);
        return this;
    }

    /**
     * Returns the {@link RoutingPolicy}.
     *
     * @return the {@code RoutingPolicy}
     * @since 3.3
     */
    public RoutingPolicy routingPolicy() {
        return routingPolicy;
    }

    /**
     * Sets the {@link RoutingPolicy}.
     *
     * @param routingPolicy the {@link RoutingPolicy}
     * @return this {@code Router}
     * @since 3.3
     */
    public synchronized Router routingPolicy(RoutingPolicy routingPolicy) {
        if (state == RUNNING) {
            throw new IllegalStateException("router is already initialized");
        }
        this.routingPolicy = routingPolicy;
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
            return new MethodRoute(path, methods, service);
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
        private final MatchedRoute matchedRoute;

        private MethodRoute(String path, HttpMethod[] methods, HttpServiceInvoker service) {
            if (methods.length == 0) {
                this.methods = "<any>";
                this.methodMatcher = MethodMatcher.any();
            } else {
                this.methods = Arrays.stream(methods).map(HttpMethod::toString).distinct()
                        .collect(Collectors.joining("|")).intern();
                this.methodMatcher = MethodMatcher.in(methods);
            }
            this.service = service;
            this.matchedRoute = new MatchedRouteImpl(this.methods, path);
        }

        private boolean matches(HttpMethod method) {
            return methodMatcher.matches(method);
        }

        @Override
        public String toString() {
            return "MethodRoute(" + methods + ") -> " + service;
        }

    }

    /**
     * The route matched in {@link Router}.
     *
     * @author MJ Fang
     * @since 2.6
     */
    public interface MatchedRoute extends HttpRequestContext.PropertyKeyProvider {

        /**
         * The property key constants.
         */
        static final Class<MatchedRoute> KEY = MatchedRoute.class;

        @Override
        default Class<MatchedRoute> key() {
            return KEY;
        }

        /**
         * Returns the target methods.
         * <p>e.g. {@code "<any>", "put", "get|post"}</p>
         *
         * @return the target methods
         */
        String methods();

        /**
         * Returns the path pattern of the matched route.
         *
         * @return the path pattern of the matched route
         */
        String path();

    }

    private static final class MatchedRouteImpl implements MatchedRoute {
        private final String methods;
        private final String path;

        private MatchedRouteImpl(String methods, String path) {
            this.methods = methods;
            this.path = path;
        }

        @Override
        public String methods() {
            return methods;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String toString() {
            return "MatchedRouteImpl(methods=" + methods + ", path=" + path + ")";
        }

        @Override
        public int hashCode() {
            return 31 * methods.hashCode() + path.hashCode();
        }

    }

    /**
     * Enumeration of routing policy.
     *
     * @author MJ Fang
     * @since 3.3
     */
    public enum RoutingPolicy {
        /**
         * Simple
         */
        SIMPLE,
        /**
         * Tree Map
         */
        TREE_MAP,
    }

    private interface ServiceRouter {

        default Stream<PathRoute> toPathRouteStream(List<RouteDefinition> routeDefinitions) {
            return routeDefinitions.stream()
                    .collect(Collectors.groupingBy(RouteDefinition::path, LinkedHashMap::new, Collectors.toList()))
                    .entrySet().stream().map(e -> {
                        String path = e.getKey();
                        PathMatcher pathMatcher = PathMatcher.fromPattern(path);
                        MethodRoute[] methodRoutes = e.getValue().stream().sorted().map(RouteDefinition::toMethodRoute)
                                .toArray(MethodRoute[]::new);
                        return new PathRoute(path, pathMatcher, methodRoutes);
                    });
        }

        CompletionStage<HttpResult> routing(HttpRequestContext ctx, MiddlewareChain next);

    }

    private static final class SimpleServiceRouter implements ServiceRouter {

        private final PathRoute[] pathRoutes;

        private SimpleServiceRouter(List<RouteDefinition> routeDefinitions, List<RouteDefinition> placeholderRouteDefinitions) {
            var definitions = new ArrayList<>(routeDefinitions);
            definitions.addAll(placeholderRouteDefinitions);
            logger.debug("Initial router by definitions: {}", definitions);
            PathRoute[] pathRoutes = toPathRouteStream(definitions).toArray(PathRoute[]::new);
            if (logger.isDebugEnabled()) {
                StringBuilder builder = new StringBuilder();
                for (PathRoute pathRoute : pathRoutes) {
                    builder.append("\n").append(pathRoute.toString(true));
                }
                logger.debug("Effective routes: {}{}", pathRoutes.length, builder);
            }
            this.pathRoutes = pathRoutes;
        }

        @Override
        public CompletionStage<HttpResult> routing(HttpRequestContext ctx, MiddlewareChain next) {
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
                            ctx.putProperty(methodRoute.matchedRoute);
                            return methodRoute.service.invoke(ctx);
                        }
                    }
                }
            }
            if (pathMatched) {
                // throw 405 Method Not Allowed
                return ctx.simpleRespond(METHOD_NOT_ALLOWED);
            }
            logger.debug("Miss match for all routes: {} {}", method, path);
            return next.doNext(ctx);
        }

    }

    private static final class TreeMapServiceRouter implements ServiceRouter {

        private final List<PathRoute> rootRoutes;
        private final Node[] nodes;

        private TreeMapServiceRouter(List<RouteDefinition> routeDefinitions, List<RouteDefinition> placeholderRouteDefinitions) {
            var rootNodes = new IntObjectHashMap<Node>();
            var rootRoutes = new ArrayList<PathRoute>();
            var maxSize = 0;
            for (var route : toPathRouteStream(routeDefinitions).toList()) {
                var paths = Arrays.stream(route.path.split("/+")).filter(StringUtil::isNotEmpty).toList();
                var size = paths.size();
                maxSize = Math.max(maxSize, size);
                if (size == 0) {
                    rootRoutes.add(route);
                } else {
                    var node = rootNodes.get(size);
                    if (node == null) {
                        rootNodes.put(size, node = new Node(0));
                    }
                    node.addFixedRoute(paths, route);
                }
            }
            for (var route : toPathRouteStream(placeholderRouteDefinitions).toList()) {
                var paths = Arrays.stream(route.path.split("/+")).filter(StringUtil::isNotEmpty).toList();
                var size = paths.size();
                maxSize = Math.max(maxSize, size);
                if (size == 0) {
                    rootRoutes.add(route);
                } else {
                    var node = rootNodes.get(size);
                    if (node == null) {
                        rootNodes.put(size, node = new Node(0));
                    }
                    node.addPatternRoute(paths, route);
                }
            }
            if (rootRoutes.isEmpty()) {
                this.rootRoutes = null;
            } else {
                this.rootRoutes = rootRoutes;
            }
            var nodes = new Node[maxSize + 1];
            for (var i = 1; i < nodes.length; i++) {
                nodes[i] = rootNodes.get(i);
            }
            this.nodes = nodes;
        }

        @Override
        public CompletionStage<HttpResult> routing(HttpRequestContext ctx, MiddlewareChain next) {
            String path = ctx.path();
            logger.trace("Routing: {} {}", ctx.method(), path);
            var paths = Arrays.asList(Arrays.stream(path.split("/+")).filter(StringUtil::isNotEmpty).toArray(String[]::new));
            var size = paths.size();
            if (size == 0) {
                return routeRoot(ctx, next);
            }
            var nodes = this.nodes;
            if (size >= nodes.length) {
                logger.debug("Miss match for all routes: {} {}", ctx.method(), path);
                return next.doNext(ctx);
            }
            var node = nodes[size];
            if (node == null) {
                logger.debug("Miss match for all routes: {} {}", ctx.method(), path);
                return next.doNext(ctx);
            }
            var result = node.routing(ctx, paths);
            if (result.hit().isPresent()) {
                var methodRoute = result.hit().get();
                logger.debug("Matched Route ({} {}): {}", ctx.method(), path, methodRoute);
                ctx.putProperty(methodRoute.matchedRoute);
                return methodRoute.service.invoke(ctx);
            }
            if (result.pathMatchedCount() > 0) {
                // throw 405 Method Not Allowed
                return ctx.simpleRespond(METHOD_NOT_ALLOWED);
            }
            logger.debug("Miss match for all routes: {} {}", ctx.method(), path);
            return next.doNext(ctx);
        }

        private CompletionStage<HttpResult> routeRoot(HttpRequestContext ctx, MiddlewareChain next) {
            var rootRoutes = this.rootRoutes;
            var method = ctx.method();
            if (rootRoutes != null) {
                for (var pathRoute : rootRoutes) {
                    logger.trace("Try {}", pathRoute);
                    for (MethodRoute methodRoute : pathRoute.methodRoutes) {
                        logger.trace("Try {}", method);
                        if (methodRoute.matches(method)) {
                            logger.debug("Matched Route ({} {}): {}", method, ctx.path(), methodRoute);
                            ctx.putProperty(methodRoute.matchedRoute);
                            return methodRoute.service.invoke(ctx);
                        }
                    }
                }
                // throw 405 Method Not Allowed
                return ctx.simpleRespond(METHOD_NOT_ALLOWED);
            }
            logger.debug("Miss match for all routes: {} {}", method, ctx.path());
            return next.doNext(ctx);
        }

        private static final record RoutingResult(int pathMatchedCount, Optional<MethodRoute> hit) {
        }

        private static final class Node {

            private final int depthLevel;
            private Map<String, Node> children;

            private Map<String, PathRoute> routeEnds;

            private List<PathRoute> patternRouteEnds;

            private Node(int depthLevel) {
                this.depthLevel = depthLevel;
            }

            private void addFixedRoute(List<String> paths, PathRoute route) {
                var depthLevel = this.depthLevel;
                var pathKey = paths.get(depthLevel);
                if (paths.size() - 1 == depthLevel) {
                    // reach end route
                    var routeEnds = this.routeEnds;
                    if (routeEnds == null) {
                        this.routeEnds = routeEnds = new LinkedHashMap<>();
                    }
                    routeEnds.put(pathKey, route);
                } else {
                    // add route to child node
                    var children = this.children;
                    if (children == null) {
                        this.children = children = new LinkedHashMap<>();
                    }
                    var child = children.get(pathKey);
                    if (child == null) {
                        children.put(pathKey, child = new Node(depthLevel + 1));
                    }
                    child.addFixedRoute(paths, route);
                }
            }

            private void addPatternRoute(List<String> paths, PathRoute route) {
                var depthLevel = this.depthLevel;
                var pathKey = paths.get(depthLevel);
                if (PathPatternUtil.anyPathVariablePattern().matcher(pathKey).find()) {
                    var patternRouteEnds = this.patternRouteEnds;
                    if (patternRouteEnds == null) {
                        this.patternRouteEnds = patternRouteEnds = new ArrayList<>();
                    }
                    patternRouteEnds.add(route);
                } else {
                    if (paths.size() - 1 == depthLevel) {
                        // reach end route
                        var routeEnds = this.routeEnds;
                        if (routeEnds == null) {
                            this.routeEnds = routeEnds = new LinkedHashMap<>();
                        }
                        routeEnds.put(pathKey, route);
                    } else {
                        // add route to child node
                        var children = this.children;
                        if (children == null) {
                            this.children = children = new LinkedHashMap<>();
                        }
                        var child = children.get(pathKey);
                        if (child == null) {
                            children.put(pathKey, child = new Node(depthLevel + 1));
                        }
                        child.addPatternRoute(paths, route);
                    }
                }
            }

            private RoutingResult routing(HttpRequestContext ctx, List<String> paths) {
                var depthLevel = this.depthLevel;
                var pathKey = paths.get(depthLevel);
                var pathMatchedCount = 0;
                if (paths.size() - 1 == depthLevel) {
                    // reach end route
                    var routeEnds = this.routeEnds;
                    if (routeEnds != null) {
                        var pathRoute = routeEnds.get(pathKey);
                        if (pathRoute != null) {
                            pathMatchedCount = 1;
                            logger.trace("Try {}", pathRoute);
                            var method = ctx.method();
                            for (MethodRoute methodRoute : pathRoute.methodRoutes) {
                                logger.trace("Try {}", method);
                                if (methodRoute.matches(method)) {
                                    return new RoutingResult(pathMatchedCount, Optional.of(methodRoute));
                                }
                            }
                        }
                    }
                } else {
                    var children = this.children;
                    if (children != null) {
                        var child = children.get(pathKey);
                        if (child != null) {
                            var childResult = child.routing(ctx, paths);
                            if (childResult.hit().isPresent()) {
                                return childResult;
                            }
                            pathMatchedCount += childResult.pathMatchedCount;
                        }
                    }
                }
                var method = ctx.method();
                var patternRouteEnds = this.patternRouteEnds;
                if (patternRouteEnds != null) {
                    for (PathRoute pathRoute : patternRouteEnds) {
                        logger.trace("Try {}", pathRoute);
                        if (pathRoute.matches(ctx)) {
                            pathMatchedCount++;
                            for (MethodRoute methodRoute : pathRoute.methodRoutes) {
                                logger.trace("Try {}", method);
                                if (methodRoute.matches(method)) {
                                    return new RoutingResult(pathMatchedCount, Optional.of(methodRoute));
                                }
                            }
                        }
                    }
                }
                return new RoutingResult(pathMatchedCount, Optional.empty());
            }

        }

    }

}
