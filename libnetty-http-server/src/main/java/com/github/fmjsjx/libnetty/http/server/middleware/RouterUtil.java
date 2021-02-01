package com.github.fmjsjx.libnetty.http.server.middleware;

import static com.github.fmjsjx.libnetty.http.HttpCommonUtil.contentType;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.util.CharsetUtil.UTF_8;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libnetty.http.server.HttpMethodWrapper;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext.HttpResponseFactory;
import com.github.fmjsjx.libnetty.http.server.HttpResponder;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServiceInvoker;
import com.github.fmjsjx.libnetty.http.server.annotation.ComponentValue;
import com.github.fmjsjx.libnetty.http.server.annotation.HeaderValue;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpRoute;
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody;
import com.github.fmjsjx.libnetty.http.server.annotation.PathVar;
import com.github.fmjsjx.libnetty.http.server.annotation.PropertyValue;
import com.github.fmjsjx.libnetty.http.server.annotation.QueryVar;
import com.github.fmjsjx.libnetty.http.server.annotation.RemoteAddr;
import com.github.fmjsjx.libnetty.http.server.component.HttpServerComponent;
import com.github.fmjsjx.libnetty.http.server.component.JsonLibrary;
import com.github.fmjsjx.libnetty.http.server.component.WorkerPool;
import com.github.fmjsjx.libnetty.http.server.exception.BadRequestException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;
import io.netty.util.internal.StringUtil;

/**
 * Utility class for HTTP controller beans.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class RouterUtil {

    private static final Logger logger = LoggerFactory.getLogger(RouterUtil.class);

    /**
     * Register the given controller to the specified router.
     * 
     * @param router     the router
     * @param controller the controller object
     * 
     * @return the count of the services just been registered
     */
    public static final int register(Router router, Object controller) {
        return register0(router, controller, controller.getClass());
    }

    private static int register0(Router router, Object controller, Class<?> clazz) {
        String pathPrefix = getPathPrefix(clazz);
        Method[] methods = clazz.getDeclaredMethods();
        int num = 0;
        METHODS_LOOP: for (Method method : methods) {
            HttpRoute route = method.getAnnotation(HttpRoute.class);
            if (route != null) {
                String path = httpPathJoin(pathPrefix, route.value());
                HttpMethod[] httpMethods = Arrays.stream(route.method()).map(HttpMethodWrapper::wrapped)
                        .toArray(HttpMethod[]::new);
                registerMethod(router, controller, method, path, httpMethods);
                num++;
                continue METHODS_LOOP;
            }
            Annotation[] mas = method.getAnnotations();
            for (Annotation ma : mas) {
                HttpRoute methodRoute = ma.annotationType().getAnnotation(HttpRoute.class);
                if (methodRoute != null) {
                    HttpMethod[] httpMethods = Arrays.stream(methodRoute.method()).map(HttpMethodWrapper::wrapped)
                            .toArray(HttpMethod[]::new);
                    String path = httpPathJoin(pathPrefix, routeValue(ma));
                    registerMethod(router, controller, method, path, httpMethods);
                    num++;
                    continue METHODS_LOOP;
                }
            }
        }
        return num;
    }

    private static final String httpPathJoin(String pathPrefix, String[] value) {
        return (pathPrefix + "/" + String.join("/", value)).replaceAll("//+", "/");
    }

    private static final void registerMethod(Router router, Object controller, Method method, String path,
            HttpMethod[] httpMethods) {
        logger.debug("Register method: {}, {}, {}, {}, {}", router, controller, method, path, httpMethods);
        boolean blocking = !CompletionStage.class.isAssignableFrom(method.getReturnType());
        method.setAccessible(true);
        JsonBody jsonResposne = method.getAnnotation(JsonBody.class);
        if (jsonResposne != null) {
            method.setAccessible(true);
            Parameter[] params = method.getParameters();
            switch (params.length) {
            case 0:
                router.add(toJsonResponseInvoker(controller, method, blocking), path, httpMethods);
                break;
            default:
                router.add(toJsonResponseInvoker(controller, method, blocking, params), path, httpMethods);
                break;
            }
            return;
        }
        if (blocking) {
            throw new IllegalArgumentException("the return type must be a CompletionStage<HttpResult>");
        }
        checkReturnType(method);
        Parameter[] params = method.getParameters();
        requireContext(params);
        method.setAccessible(true);
        switch (params.length) {
        case 1:
            router.add(toSimpleInvoker(controller, method), path, httpMethods);
            break;
        default:
            router.add(toParamsInvoker(controller, method, params), path, httpMethods);
            break;
        }

    }

    private static final BiFunction<Object, Throwable, CompletionStage<HttpResult>> jsonResponseHandler(
            HttpRequestContext ctx) {
        return (result, cause) -> {
            if (cause != null) {
                if (cause instanceof CompletionException) {
                    return ctx.respondError(cause.getCause());
                }
                return ctx.respondError(cause);
            }
            try {
                ByteBuf content = ctx.component(JsonLibrary.class).orElseThrow(JsonConstants.MISSING_JSON_LIBRARY)
                        .write(ctx.alloc(), result);
                return ctx.simpleRespond(OK, content, JsonConstants.APPLICATION_JSON_UTF8);
            } catch (Exception e) {
                return ctx.respondError(e);
            }
        };
    }

    private static final class JsonConstants {

        private static final AsciiString APPLICATION_JSON_UTF8 = contentType(APPLICATION_JSON, UTF_8);

        private static final IllegalArgumentException MISSING_JSON_LIBRARY_EXCEPTION = new IllegalArgumentException();

        private static final Supplier<IllegalArgumentException> MISSING_JSON_LIBRARY = () -> MISSING_JSON_LIBRARY_EXCEPTION;

    }

    private static final class WorkerPoolConstants {
        private static final IllegalArgumentException MISSING_WORKER_POOL_EXCEPTION = new IllegalArgumentException();

        private static final Supplier<IllegalArgumentException> MISSING_WORKER_POOL = () -> MISSING_WORKER_POOL_EXCEPTION;
    }

    private static final Function<CompletionStage<HttpResult>, CompletionStage<HttpResult>> resultIdentity = Function
            .identity();

    private static final CompletionException fromTarget(InvocationTargetException e) {
        return valueOf(e.getTargetException());
    }

    private static final CompletionException valueOf(Throwable e) {
        if (e instanceof CompletionException) {
            return (CompletionException) e;
        } else {
            return new CompletionException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static HttpServiceInvoker toJsonResponseInvoker(Object controller, Method method, boolean blocking) {
        if (Modifier.isStatic(method.getModifiers())) {
            if (blocking) {
                return ctx -> {
                    try {
                        WorkerPool workerPool = ctx.component(WorkerPool.class)
                                .orElseThrow(WorkerPoolConstants.MISSING_WORKER_POOL);
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                return method.invoke(null);
                            } catch (InvocationTargetException e) {
                                throw fromTarget(e);
                            } catch (Exception e) {
                                throw valueOf(e);
                            }
                        }, workerPool.executor()).handle(jsonResponseHandler(ctx)).thenCompose(resultIdentity);
                    } catch (Exception e) {
                        return ctx.respondError(e);
                    }
                };
            }
            return ctx -> {
                try {
                    return ((CompletionStage<Object>) method.invoke(null)).handle(jsonResponseHandler(ctx))
                            .thenCompose(resultIdentity);
                } catch (InvocationTargetException e) {
                    return ctx.respondError(e.getTargetException());
                } catch (Exception e) {
                    return ctx.respondError(e);
                }
            };
        }
        if (blocking) {
            return ctx -> {
                try {
                    WorkerPool workerPool = ctx.component(WorkerPool.class)
                            .orElseThrow(WorkerPoolConstants.MISSING_WORKER_POOL);
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            return method.invoke(controller);
                        } catch (InvocationTargetException e) {
                            throw fromTarget(e);
                        } catch (IllegalAccessException | IllegalArgumentException e) {
                            throw valueOf(e);
                        }
                    }, workerPool.executor()).handle(jsonResponseHandler(ctx)).thenCompose(resultIdentity);
                } catch (Exception e) {
                    return ctx.respondError(e);
                }
            };
        }
        return ctx -> {
            try {
                return ((CompletionStage<Object>) method.invoke(controller)).handle(jsonResponseHandler(ctx))
                        .thenCompose(resultIdentity);
            } catch (InvocationTargetException e) {
                return ctx.respondError(e.getTargetException());
            } catch (Exception e) {
                return ctx.respondError(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static HttpServiceInvoker toJsonResponseInvoker(Object controller, Method method, boolean blocking,
            Parameter[] params) {
        Function<HttpRequestContext, Object[]> parametesMapper = toParametersMapper(params);
        if (Modifier.isStatic(method.getModifiers())) {
            if (blocking) {
                return ctx -> {
                    try {
                        WorkerPool workerPool = ctx.component(WorkerPool.class)
                                .orElseThrow(WorkerPoolConstants.MISSING_WORKER_POOL);
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                return method.invoke(null, parametesMapper.apply(ctx));
                            } catch (InvocationTargetException e) {
                                throw fromTarget(e);
                            } catch (Exception e) {
                                throw valueOf(e);
                            }
                        }, workerPool.executor()).handle(jsonResponseHandler(ctx)).thenCompose(resultIdentity);
                    } catch (Exception e) {
                        return ctx.respondError(e);
                    }
                };
            }
            return ctx -> {
                try {
                    return ((CompletionStage<Object>) method.invoke(null, parametesMapper.apply(ctx)))
                            .handle(jsonResponseHandler(ctx)).thenCompose(resultIdentity);
                } catch (InvocationTargetException e) {
                    return ctx.respondError(e.getTargetException());
                } catch (Exception e) {
                    return ctx.respondError(e);
                }
            };
        }
        if (blocking) {
            return ctx -> {
                try {
                    WorkerPool workerPool = ctx.component(WorkerPool.class)
                            .orElseThrow(WorkerPoolConstants.MISSING_WORKER_POOL);
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            return method.invoke(controller, parametesMapper.apply(ctx));
                        } catch (InvocationTargetException e) {
                            throw fromTarget(e);
                        } catch (Exception e) {
                            throw valueOf(e);
                        }
                    }, workerPool.executor()).handle(jsonResponseHandler(ctx)).thenCompose(resultIdentity);
                } catch (Exception e) {
                    return ctx.respondError(e);
                }
            };
        }
        return ctx -> {
            try {
                return ((CompletionStage<Object>) method.invoke(controller, parametesMapper.apply(ctx)))
                        .handle(jsonResponseHandler(ctx)).thenCompose(resultIdentity);
            } catch (InvocationTargetException e) {
                return ctx.respondError(e.getTargetException());
            } catch (Exception e) {
                return ctx.respondError(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static final Function<HttpRequestContext, Object[]> toParametersMapper(Parameter[] params) {
        Function<HttpRequestContext, Object>[] parameterMappers = Arrays.stream(params)
                .map(RouterUtil::toParameterMapper).toArray(Function[]::new);
        return toParametersMapper(parameterMappers);
    }

    private static final void requireContext(Parameter[] params) {
        if (Arrays.stream(params).map(Parameter::getType).noneMatch(Predicate.isEqual(HttpRequestContext.class))) {
            throw new IllegalArgumentException("missing parameter as type HttpRequestContext");
        }
    }

    @SuppressWarnings("unchecked")
    private static final HttpServiceInvoker toSimpleInvoker(Object controller, Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return ctx -> {
                try {
                    return (CompletionStage<HttpResult>) method.invoke(null, ctx);
                } catch (InvocationTargetException e) {
                    return ctx.respondError(e.getTargetException());
                } catch (Exception e) {
                    return ctx.respondError(e);
                }
            };
        }
        return ctx -> {
            try {
                return (CompletionStage<HttpResult>) method.invoke(controller, ctx);
            } catch (InvocationTargetException e) {
                return ctx.respondError(e.getTargetException());
            } catch (Exception e) {
                return ctx.respondError(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static final HttpServiceInvoker toParamsInvoker(Object controller, Method method, Parameter[] params) {
        Function<HttpRequestContext, Object[]> parametesMapper = toParametersMapper(params);
        if (Modifier.isStatic(method.getModifiers())) {
            return ctx -> {
                try {
                    return (CompletionStage<HttpResult>) method.invoke(null, parametesMapper.apply(ctx));
                } catch (InvocationTargetException e) {
                    return ctx.respondError(e.getTargetException());
                } catch (Exception e) {
                    return ctx.respondError(e);
                }
            };
        }
        return ctx -> {
            try {
                return (CompletionStage<HttpResult>) method.invoke(controller, parametesMapper.apply(ctx));
            } catch (InvocationTargetException e) {
                return ctx.respondError(e.getTargetException());
            } catch (Exception e) {
                return ctx.respondError(e);
            }
        };
    }

    private static final Function<HttpRequestContext, Object> contextMapper = ctx -> ctx;
    private static final Function<HttpRequestContext, Object> responseFactoryMapper = HttpRequestContext::responseFactory;
    private static final Function<HttpRequestContext, Object> fullRequestMapper = HttpRequestContext::request;
    private static final Function<HttpRequestContext, Object> headersMapper = HttpRequestContext::headers;
    private static final Function<HttpRequestContext, Object> queryMapper = HttpRequestContext::queryStringDecoder;
    private static final Function<HttpRequestContext, Object> eventLoopMapper = HttpRequestContext::eventLoop;
    private static final Function<HttpRequestContext, Object> remoteAddrMapper = HttpRequestContext::remoteAddress;

    private static final Function<HttpRequestContext, Object> toParameterMapper(Parameter param) {
        if (param.getType() == HttpRequestContext.class || param.getType() == HttpResponder.class) {
            return contextMapper;
        } else if (param.getType() == HttpResponseFactory.class) {
            return responseFactoryMapper;
        } else if (param.getType() == HttpRequest.class || param.getType() == FullHttpRequest.class) {
            return fullRequestMapper;
        } else if (param.getType() == HttpHeaders.class) {
            return headersMapper;
        } else if (param.getType() == QueryStringDecoder.class) {
            return queryMapper;
        } else if (param.getType().isAssignableFrom(EventLoop.class)) {
            return eventLoopMapper;
        }
        PathVar pathVar = param.getAnnotation(PathVar.class);
        if (pathVar != null) {
            return toPathVarMapper(param, pathVar);
        }
        QueryVar queryVar = param.getAnnotation(QueryVar.class);
        if (queryVar != null) {
            return toQueryVarMapper(param, queryVar);
        }
        JsonBody jsonBody = param.getAnnotation(JsonBody.class);
        if (jsonBody != null) {
            return toJsonBodyMapper(param, jsonBody);
        }
        HeaderValue headerValue = param.getAnnotation(HeaderValue.class);
        if (headerValue != null) {
            return toHeaderValueMapper(param, headerValue);
        }
        RemoteAddr remoteAddr = param.getAnnotation(RemoteAddr.class);
        if (remoteAddr != null) {
            if (param.getType() != String.class) {
                throw new IllegalArgumentException(
                        "unsupperted type " + param.getType() + " for @RemoteAddr, only support String");
            }
            return remoteAddrMapper;
        }
        ComponentValue componentValue = param.getAnnotation(ComponentValue.class);
        if (componentValue != null) {
            return toComponentValueMapper(param, componentValue);
        }
        PropertyValue propertyValue = param.getAnnotation(PropertyValue.class);
        if (propertyValue != null) {
            return toPropertyValueMapper(param, propertyValue);
        }
        return toZeroValueMapper(param);
    }

    private static Function<HttpRequestContext, Object> toPathVarMapper(Parameter param, PathVar pathVar) {
        Class<?> type = param.getType();
        String name = StringUtil.isNullOrEmpty(pathVar.value()) ? param.getName() : pathVar.value();
        Supplier<IllegalArgumentException> noSuchPathVariable = noSuchPathVariable(name);
        if (type == String.class) {
            return ctx -> ctx.pathVariables().getString(name).orElseThrow(noSuchPathVariable);
        } else if (type == Integer.class || type == int.class) {
            return ctx -> ctx.pathVariables().getString(name).map(Integer::valueOf).orElseThrow(noSuchPathVariable);
        } else if (type == Long.class || type == long.class) {
            return ctx -> ctx.pathVariables().getString(name).map(Long::valueOf).orElseThrow(noSuchPathVariable);
        } else if (type == Double.class || type == double.class) {
            return ctx -> ctx.pathVariables().getString(name).map(Double::valueOf).orElseThrow(noSuchPathVariable);
        } else if (type == Boolean.class || type == boolean.class) {
            return ctx -> ctx.pathVariables().getString(name).map(Boolean::parseBoolean)
                    .orElseThrow(noSuchPathVariable);
        } else if (type == Byte.class || type == byte.class) {
            return ctx -> ctx.pathVariables().getString(name).map(Byte::valueOf).orElseThrow(noSuchPathVariable);
        } else if (type == Short.class || type == short.class) {
            return ctx -> ctx.pathVariables().getString(name).map(Short::valueOf).orElseThrow(noSuchPathVariable);
        } else if (type == Float.class || type == float.class) {
            return ctx -> ctx.pathVariables().getString(name).map(Float::valueOf).orElseThrow(noSuchPathVariable);
        } else if (type == BigInteger.class) {
            return ctx -> ctx.pathVariables().getString(name).map(BigInteger::new).orElseThrow(noSuchPathVariable);
        } else if (type == BigDecimal.class) {
            return ctx -> ctx.pathVariables().getString(name).map(BigDecimal::new).orElseThrow(noSuchPathVariable);
        } else {
            throw new IllegalArgumentException("unsupported type " + type + " for @PathVar");
        }
    }

    private static final ConcurrentMap<String, IllegalArgumentException> illegalArgumentExceptions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Supplier<IllegalArgumentException>> illegalArguemntSuppliers = new ConcurrentHashMap<>();

    private static final Supplier<IllegalArgumentException> noSuchPathVariable(String name) {
        String message = "missing path variable " + name;
        IllegalArgumentException error = illegalArgumentExceptions.computeIfAbsent(message,
                IllegalArgumentException::new);
        return illegalArguemntSuppliers.computeIfAbsent(message, k -> () -> error);
    }

    private static final Function<HttpRequestContext, Object> toQueryVarMapper(Parameter param, QueryVar queryVar) {
        Type type = param.getParameterizedType();
        String name = StringUtil.isNullOrEmpty(queryVar.value()) ? param.getName() : queryVar.value();
        if (type instanceof Class<?>) {
            if (((Class<?>) type).isArray()) {
                return toArrayMapper(queryVar, type, name);
            } else {
                return toSimpleMapper(queryVar, type, name);
            }
        }
        if (List.class == param.getType()) {
            return toListMapper(queryVar, (ParameterizedType) type, name);
        }
        if (Set.class == param.getType()) {
            return toSetMapper(queryVar, (ParameterizedType) type, name);
        }
        if (Optional.class == param.getType()) {
            return toOptionalMapper(queryVar, (ParameterizedType) type, name);
        }
        throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
    }

    private static final Map<Class<?>, Function<List<String>, Object>> queryValueMappers;

    static {
        Map<Class<?>, Function<List<String>, Object>> map = new HashMap<>();
        // arrays
        map.put(String[].class, values -> values.stream().toArray(String[]::new));
        map.put(int[].class, values -> values.stream().mapToInt(Integer::parseInt).toArray());
        map.put(long[].class, values -> values.stream().mapToLong(Long::parseLong).toArray());
        map.put(Integer[].class, values -> values.stream().map(Integer::valueOf).toArray(Integer[]::new));
        map.put(Long[].class, values -> values.stream().map(Long::valueOf).toArray(Long[]::new));
        // simples
        map.put(String.class, values -> values.size() == 1 ? values.get(0) : String.join(",", values));
        map.put(Boolean.class, values -> Boolean.valueOf(values.get(0)));
        map.put(Byte.class, values -> Byte.valueOf(values.get(0)));
        map.put(Short.class, values -> Short.valueOf(values.get(0)));
        map.put(Integer.class, values -> Integer.valueOf(values.get(0)));
        map.put(Long.class, values -> Long.valueOf(values.get(0)));
        map.put(Float.class, values -> Float.valueOf(values.get(0)));
        map.put(Double.class, values -> Double.valueOf(values.get(0)));
        map.put(BigInteger.class, values -> new BigInteger(values.get(0)));
        map.put(BigDecimal.class, values -> new BigDecimal(values.get(0)));
        map.put(OptionalInt.class, values -> OptionalInt.of(Integer.parseInt(values.get(0))));
        map.put(OptionalLong.class, values -> OptionalLong.of(Long.parseLong(values.get(0))));
        map.put(OptionalDouble.class, values -> OptionalDouble.of(Double.parseDouble(values.get(0))));

        queryValueMappers = map;
    }

    private static final Function<HttpRequestContext, Object> toArrayMapper(QueryVar queryVar, Type type, String name) {
        Function<List<String>, Object> mapper = queryValueMappers.get(type == Object[].class ? String[].class : type);
        if (mapper == null) {
            throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
        }
        if (queryVar.required()) {
            Supplier<IllegalArgumentException> noSuchQueryVariable = noSuchQueryVariable(name);
            return ctx -> ctx.queryParameter(name).map(mapper).orElseThrow(noSuchQueryVariable);
        } else {
            return ctx -> ctx.queryParameter(name).map(mapper).orElseGet(null);
        }
    }

    private static Function<HttpRequestContext, Object> toSimpleMapper(QueryVar queryVar, Type type, String name) {
        Function<List<String>, Object> mapper;
        if (type == String.class || type == Object.class) {
            mapper = queryValueMappers.get(String.class);
        } else if (type == int.class || type == Integer.class) {
            mapper = queryValueMappers.get(Integer.class);
        } else if (type == long.class || type == Long.class) {
            mapper = queryValueMappers.get(Long.class);
        } else if (type == double.class || type == Double.class) {
            mapper = queryValueMappers.get(Double.class);
        } else if (type == boolean.class || type == Boolean.class) {
            mapper = queryValueMappers.get(Boolean.class);
        } else if (type == byte.class || type == Byte.class) {
            mapper = queryValueMappers.get(Byte.class);
        } else if (type == short.class || type == Short.class) {
            mapper = queryValueMappers.get(Short.class);
        } else if (type == float.class || type == Float.class) {
            mapper = queryValueMappers.get(Float.class);
        } else if (type == BigInteger.class) {
            mapper = queryValueMappers.get(BigInteger.class);
        } else if (type == BigDecimal.class) {
            mapper = queryValueMappers.get(BigDecimal.class);
        } else if (type == OptionalInt.class) {
            mapper = queryValueMappers.get(OptionalInt.class);
            return ctx -> ctx.queryParameter(name).map(mapper).orElse(OptionalInt.empty());
        } else if (type == OptionalLong.class) {
            mapper = queryValueMappers.get(OptionalLong.class);
            return ctx -> ctx.queryParameter(name).map(mapper).orElse(OptionalLong.empty());
        } else if (type == OptionalDouble.class) {
            mapper = queryValueMappers.get(OptionalDouble.class);
            return ctx -> ctx.queryParameter(name).map(mapper).orElse(OptionalDouble.empty());
        } else {
            throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
        }
        if (queryVar.required()) {
            Supplier<IllegalArgumentException> noSuchQueryVariable = noSuchQueryVariable(name);
            return ctx -> ctx.queryParameter(name).map(mapper).orElseThrow(noSuchQueryVariable);
        } else {
            return ctx -> ctx.queryParameter(name).map(mapper).orElse(null);
        }
    }

    private static final Supplier<IllegalArgumentException> noSuchQueryVariable(String name) {
        String message = "missing path query variable " + name;
        IllegalArgumentException error = illegalArgumentExceptions.computeIfAbsent(message,
                IllegalArgumentException::new);
        return illegalArguemntSuppliers.computeIfAbsent(message, k -> () -> error);
    }

    private static final Map<Class<?>, Function<List<String>, Object>> queryListValueMappers;

    static {
        Map<Class<?>, Function<List<String>, Object>> map = new HashMap<>();
        Collector<Object, ?, ?> toList = Collectors.toList();
        map.put(String.class, ArrayList::new);
        map.put(Byte.class, values -> values.stream().map(Byte::valueOf).collect(toList));
        map.put(Short.class, values -> values.stream().map(Short::valueOf).collect(toList));
        map.put(Integer.class, values -> values.stream().map(Integer::valueOf).collect(toList));
        map.put(Long.class, values -> values.stream().map(Long::valueOf).collect(toList));
        map.put(Float.class, values -> values.stream().map(Float::valueOf).collect(toList));
        map.put(Double.class, values -> values.stream().map(Double::valueOf).collect(toList));
        map.put(Boolean.class, values -> values.stream().map(Boolean::valueOf).collect(toList));
        map.put(BigInteger.class, values -> values.stream().map(BigInteger::new).collect(toList));
        map.put(BigDecimal.class, values -> values.stream().map(BigDecimal::new).collect(toList));
        queryListValueMappers = map;
    }

    private static final Function<HttpRequestContext, Object> toListMapper(QueryVar queryVar, ParameterizedType type,
            String name) {
        Type atype = type.getActualTypeArguments()[0];
        Function<List<String>, Object> mapper = queryListValueMappers.get(atype == Object.class ? String.class : atype);
        if (mapper == null) {
            throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
        }
        if (queryVar.required()) {
            Supplier<IllegalArgumentException> noSuchQueryVariable = noSuchQueryVariable(name);
            return ctx -> ctx.queryParameter(name).map(mapper).orElseThrow(noSuchQueryVariable);
        } else {
            return ctx -> ctx.queryParameter(name).map(mapper).orElse(null);
        }
    }

    private static final Map<Class<?>, Function<List<String>, Object>> querySetValueMappers;

    static {
        Map<Class<?>, Function<List<String>, Object>> map = new HashMap<>();
        Collector<Object, ?, ?> toSet = Collectors.toCollection(LinkedHashSet::new);
        map.put(String.class, LinkedHashSet::new);
        map.put(Byte.class, values -> values.stream().map(Byte::valueOf).collect(toSet));
        map.put(Short.class, values -> values.stream().map(Short::valueOf).collect(toSet));
        map.put(Integer.class, values -> values.stream().map(Integer::valueOf).collect(toSet));
        map.put(Long.class, values -> values.stream().map(Long::valueOf).collect(toSet));
        map.put(Float.class, values -> values.stream().map(Float::valueOf).collect(toSet));
        map.put(Double.class, values -> values.stream().map(Double::valueOf).collect(toSet));
        map.put(Boolean.class, values -> values.stream().map(Boolean::valueOf).collect(toSet));
        map.put(BigInteger.class, values -> values.stream().map(BigInteger::new).collect(toSet));
        map.put(BigDecimal.class, values -> values.stream().map(BigDecimal::new).collect(toSet));
        querySetValueMappers = map;
    }

    private static final Function<HttpRequestContext, Object> toSetMapper(QueryVar queryVar, ParameterizedType type,
            String name) {
        Type atype = type.getActualTypeArguments()[0];
        Function<List<String>, Object> mapper = querySetValueMappers.get(atype == Object.class ? String.class : atype);
        if (mapper == null) {
            throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
        }
        if (queryVar.required()) {
            Supplier<IllegalArgumentException> noSuchQueryVariable = noSuchQueryVariable(name);
            return ctx -> ctx.queryParameter(name).map(mapper).orElseThrow(noSuchQueryVariable);
        } else {
            return ctx -> ctx.queryParameter(name).map(mapper).orElse(null);
        }
    }

    private static final Function<HttpRequestContext, Object> toOptionalMapper(QueryVar queryVar,
            ParameterizedType type, String name) {
        Type atype = type.getActualTypeArguments()[0];
        Function<List<String>, Object> mapper = queryValueMappers.get(atype == Object.class ? String.class : atype);
        if (mapper == null) {
            throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
        }
        return ctx -> ctx.queryParameter(name).map(mapper);
    }

    private static final Function<HttpRequestContext, Object> contentToStringMapper = ctx -> ctx.request().content()
            .toString(UTF_8);
    private static final Function<HttpRequestContext, Object> contentToBytesMapper = ctx -> ByteBufUtil
            .getBytes(ctx.request().content());

    private static final Function<HttpRequestContext, Object> toJsonBodyMapper(Parameter param, JsonBody jsonBody) {
        Type type = param.getParameterizedType();
        if (type == String.class) {
            return contentToStringMapper;
        } else if (type == byte[].class) {
            return contentToBytesMapper;
        } else {
            return ctx -> ctx.component(JsonLibrary.class).orElseThrow(JsonConstants.MISSING_JSON_LIBRARY)
                    .read(ctx.request().content(), type);
        }
    }

    private static final Supplier<IllegalArgumentException> noSuchHeader(String name) {
        String message = "missing header " + name;
        IllegalArgumentException error = illegalArgumentExceptions.computeIfAbsent(message,
                IllegalArgumentException::new);
        return illegalArguemntSuppliers.computeIfAbsent(message, k -> () -> error);
    }

    private static final Function<HttpRequestContext, Object> toHeaderValueMapper(Parameter param,
            HeaderValue headerValue) {
        Type type = param.getParameterizedType();
        String name = headerValue.value();
        if (type instanceof Class<?>) {
            return toSimpleMapper(headerValue, type, name);
        }
        if (Optional.class == param.getType()) {
            return toOptionalMapper(headerValue, (ParameterizedType) type, name);
        }
        throw new IllegalArgumentException("unsupported type " + type + " for @HeaderValue");
    }

    @SuppressWarnings("unchecked")
    private static final Function<HttpRequestContext, Object> toComponentValueMapper(Parameter param,
            ComponentValue componentValue) {
        Type type = param.getParameterizedType();
        if (componentValue.value() == HttpServerComponent.class) {
            if (type instanceof Class<?>) {
                Class<? extends HttpServerComponent> key = (Class<? extends HttpServerComponent>) param.getType();
                if (componentValue.required()) {
                    Supplier<IllegalArgumentException> noSuchComponentValue = noSuchComponentValue(key.toString());
                    return ctx -> ctx.component(key).orElseThrow(noSuchComponentValue);
                } else {
                    return ctx -> ctx.component(key).orElse(null);
                }
            }
            if (Optional.class == param.getType()) {
                Type atype = ((ParameterizedType) type).getActualTypeArguments()[0];
                if (atype instanceof Class<?>) {
                    Class<? extends HttpServerComponent> key = (Class<? extends HttpServerComponent>) atype;
                    // skip required check
                    return ctx -> ctx.component(key);
                }
            }
        } else {
            Class<? extends HttpServerComponent> key = componentValue.value();
            if (type instanceof Class<?>) {
                if (componentValue.required()) {
                    Supplier<IllegalArgumentException> noSuchComponentValue = noSuchComponentValue(key.toString());
                    return ctx -> ctx.component(key).orElseThrow(noSuchComponentValue);
                } else {
                    return ctx -> ctx.component(key).orElse(null);
                }
            }
            if (Optional.class == param.getType()) {
                // skip required check
                return ctx -> ctx.property(key);
            }
        }
        throw new IllegalArgumentException("unsupported type " + type + " for @PropertyValue");
    }

    private static final Function<HttpRequestContext, Object> toPropertyValueMapper(Parameter param,
            PropertyValue propertyValue) {
        Type type = param.getParameterizedType();
        if (StringUtil.isNullOrEmpty(propertyValue.value())) {
            if (type instanceof Class<?>) {
                Class<?> key = param.getType();
                if (propertyValue.required()) {
                    Supplier<IllegalArgumentException> noSuchPropertyValue = noSuchPropertyValue(key.toString());
                    return ctx -> ctx.property(key).orElseThrow(noSuchPropertyValue);
                } else {
                    return ctx -> ctx.property(key).orElse(null);
                }
            }
            if (Optional.class == param.getType()) {
                Type atype = ((ParameterizedType) type).getActualTypeArguments()[0];
                if (atype instanceof Class<?>) {
                    Class<?> key = (Class<?>) atype;
                    // skip required check
                    return ctx -> ctx.property(key);
                }
            }
        } else {
            String key = propertyValue.value();
            if (type instanceof Class<?>) {
                Class<?> valueType = param.getType();
                if (propertyValue.required()) {
                    Supplier<IllegalArgumentException> noSuchPropertyValue = noSuchPropertyValue(key.toString());
                    return ctx -> ctx.property(key, valueType).orElseThrow(noSuchPropertyValue);
                } else {
                    return ctx -> ctx.property(key, valueType).orElse(null);
                }
            }
            if (Optional.class == param.getType()) {
                // skip required check
                return ctx -> ctx.property(key);
            }
        }
        throw new IllegalArgumentException("unsupported type " + type + " for @PropertyValue");
    }

    private static final Supplier<IllegalArgumentException> noSuchComponentValue(String name) {
        String message = "missing component value " + name;
        IllegalArgumentException error = illegalArgumentExceptions.computeIfAbsent(message,
                IllegalArgumentException::new);
        return illegalArguemntSuppliers.computeIfAbsent(message, k -> () -> error);
    }

    private static final Supplier<IllegalArgumentException> noSuchPropertyValue(String name) {
        String message = "missing property value " + name;
        IllegalArgumentException error = illegalArgumentExceptions.computeIfAbsent(message,
                IllegalArgumentException::new);
        return illegalArguemntSuppliers.computeIfAbsent(message, k -> () -> error);
    }

    private static Function<HttpRequestContext, Object> toSimpleMapper(HeaderValue headerValue, Type type,
            String name) {
        if (type == String.class || type == Object.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).orElseThrow(noSuchHeader);
            } else {
                return ctx -> ctx.headers().get(name);
            }
        } else if (type == int.class || type == Integer.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().getInt(name)).orElseThrow(noSuchHeader);
            } else {
                return ctx -> ctx.headers().getInt(name);
            }
        } else if (type == short.class || type == Short.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().getShort(name)).orElseThrow(noSuchHeader);
            } else {
                return ctx -> ctx.headers().getShort(name);
            }
        } else if (type == long.class || type == Long.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Long::valueOf).orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Long::valueOf).orElse(null);
            }
        } else if (type == byte.class || type == Byte.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Byte::valueOf).orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Byte::valueOf).orElse(null);
            }
        } else if (type == float.class || type == Float.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Float::valueOf)
                        .orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Float::valueOf).orElse(null);
            }
        } else if (type == double.class || type == Double.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Double::valueOf)
                        .orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Double::valueOf).orElse(null);
            }
        } else if (type == BigInteger.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(BigInteger::new)
                        .orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(BigInteger::new).orElse(null);
            }
        } else if (type == BigDecimal.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(BigDecimal::new)
                        .orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(BigDecimal::new).orElse(null);
            }
        } else if (type == Date.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Date::new)
                        .orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Date::new).orElse(null);
            }
        } else if (type == Instant.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                        .orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                        .orElse(null);
            }
        } else if (type == ZonedDateTime.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                        .map(i -> i.atZone(ZoneId.systemDefault())).orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                        .map(i -> i.atZone(ZoneId.systemDefault())).orElse(null);
            }
        } else if (type == OffsetDateTime.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                        .map(i -> i.atZone(ZoneId.systemDefault()).toOffsetDateTime()).orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                        .map(i -> i.atZone(ZoneId.systemDefault()).toOffsetDateTime()).orElse(null);
            }
        } else if (type == LocalDateTime.class) {
            if (headerValue.required()) {
                Supplier<IllegalArgumentException> noSuchHeader = noSuchHeader(name);
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                        .map(i -> LocalDateTime.ofInstant(i, ZoneId.systemDefault())).orElseThrow(noSuchHeader);
            } else {
                return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                        .map(i -> LocalDateTime.ofInstant(i, ZoneId.systemDefault())).orElse(null);
            }
        } else if (type == OptionalInt.class) {
            return ctx -> Optional.ofNullable(ctx.headers().getInt(name)).map(OptionalInt::of)
                    .orElse(OptionalInt.empty());
        } else if (type == OptionalLong.class) {
            return ctx -> {
                String v = ctx.headers().get(name);
                return v == null ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(v));
            };
        } else if (type == OptionalDouble.class) {
            return ctx -> {
                String v = ctx.headers().get(name);
                return v == null ? OptionalDouble.empty() : OptionalDouble.of(Double.parseDouble(v));
            };
        }
        throw new IllegalArgumentException("unsupported type " + type + " for @HeaderValue");
    }

    private static final Function<HttpRequestContext, Object> toOptionalMapper(HeaderValue headerValue,
            ParameterizedType type, String name) {
        Type atype = type.getActualTypeArguments()[0];
        if (atype == String.class || atype == Object.class) {
            return ctx -> Optional.ofNullable(ctx.headers().get(name));
        } else if (atype == Integer.class) {
            return ctx -> Optional.ofNullable(ctx.headers().getInt(name));
        } else if (atype == Short.class) {
            return ctx -> Optional.ofNullable(ctx.headers().getShort(name));
        } else if (atype == Long.class) {
            return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Long::valueOf);
        } else if (atype == Byte.class) {
            return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Byte::valueOf);
        } else if (atype == Double.class) {
            return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Double::valueOf);
        } else if (atype == Float.class) {
            return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(Float::valueOf);
        } else if (atype == BigDecimal.class) {
            return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(BigDecimal::new);
        } else if (atype == BigInteger.class) {
            return ctx -> Optional.ofNullable(ctx.headers().get(name)).map(BigInteger::new);
        } else if (atype == Date.class) {
            return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Date::new);
        } else if (atype == Instant.class) {
            return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli);
        } else if (atype == LocalDateTime.class) {
            return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                    .map(i -> LocalDateTime.ofInstant(i, ZoneId.systemDefault()));
        } else if (atype == ZonedDateTime.class) {
            return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                    .map(i -> i.atZone(ZoneId.systemDefault()));
        } else if (atype == OffsetDateTime.class) {
            return ctx -> Optional.ofNullable(ctx.headers().getTimeMillis(name)).map(Instant::ofEpochMilli)
                    .map(i -> i.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }
        throw new IllegalArgumentException("unsupported type " + type + " for @HeaderValue");
    }

    private static final Map<Class<?>, Function<HttpRequestContext, Object>> zeroValueMappers;
    private static final Function<HttpRequestContext, Object> nullMapper = ctx -> null;

    static {
        Map<Class<?>, Function<HttpRequestContext, Object>> map = new HashMap<>();
        Byte b0 = 0;
        Short s0 = 0;
        Integer i0 = 0;
        Long l0 = 0L;
        Float f0 = 0F;
        Double d0 = 0D;
        map.put(boolean.class, ctx -> Boolean.FALSE);
        map.put(char.class, ctx -> 0);
        map.put(byte.class, ctx -> b0);
        map.put(short.class, ctx -> s0);
        map.put(int.class, ctx -> i0);
        map.put(long.class, ctx -> l0);
        map.put(float.class, ctx -> f0);
        map.put(double.class, ctx -> d0);
        zeroValueMappers = map;
    }

    private static final Function<HttpRequestContext, Object> toZeroValueMapper(Parameter param) {
        Class<?> type = param.getType();
        if (type.isPrimitive()) {
            return zeroValueMappers.get(type);
        } else {
            return nullMapper;
        }
    }

    private static final Function<HttpRequestContext, Object[]> toParametersMapper(
            Function<HttpRequestContext, Object>[] parameterMappers) {
        return ctx -> {
            try {
                Object[] args = new Object[parameterMappers.length];
                for (int i = 0; i < parameterMappers.length; i++) {
                    args[i] = parameterMappers[i].apply(ctx);
                }
                return args;
            } catch (Exception e) {
                throw new BadRequestException(e);
            }
        };
    }

    private static final void checkReturnType(Method method) {
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
        if (!HttpResult.class.isAssignableFrom((Class<?>) returnType.getActualTypeArguments()[0])) {
            throw new IllegalArgumentException("the return type must be a CompletionStage<HttpResult>");
        }
    }

    private static final String[] routeValue(Annotation ma) {
        try {
            return (String[]) ma.annotationType().getMethod("value").invoke(ma);
        } catch (Exception e) {
            throw new IllegalArgumentException("register controller failed", e);
        }
    }

    private static final String getPathPrefix(Class<?> clazz) {
        HttpPath path = clazz.getAnnotation(HttpPath.class);
        if (path != null) {
            return "/" + String.join("/", path.value());
        }
        return "/";
    }

    /**
     * Register the given controller to the specified router.
     * 
     * @param <T>        the type of the controller
     * @param router     the router
     * @param controller the controller object
     * @param clazz      the class of the type
     * 
     * @return the count of the services just been registered
     */
    public static final <T> int register(Router router, T controller, Class<T> clazz) {
        return register0(router, controller, clazz);
    }

}
