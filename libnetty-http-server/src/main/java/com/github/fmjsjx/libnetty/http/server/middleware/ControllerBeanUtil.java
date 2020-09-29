package com.github.fmjsjx.libnetty.http.server.middleware;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libnetty.http.server.HttpMethodWrapper;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServerUtil;
import com.github.fmjsjx.libnetty.http.server.HttpServiceInvoker;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpPath;
import com.github.fmjsjx.libnetty.http.server.annotation.HttpRoute;
import com.github.fmjsjx.libnetty.http.server.annotation.JsonBody;
import com.github.fmjsjx.libnetty.http.server.annotation.PathVar;
import com.github.fmjsjx.libnetty.http.server.annotation.QueryVar;
import com.github.fmjsjx.libnetty.http.server.exception.BadRequestException;
import com.github.fmjsjx.libnetty.http.server.middleware.SupportJson.JsonLibrary;

import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

/**
 * Utility class for HTTP controller beans.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class ControllerBeanUtil {

    private static final Logger logger = LoggerFactory.getLogger(ControllerBeanUtil.class);

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
        Method[] methods = clazz.getMethods();
        int num = 0;
        METHODS_LOOP: for (Method method : methods) {
            HttpRoute route = method.getAnnotation(HttpRoute.class);
            if (route != null) {
                String path = (pathPrefix + "/" + String.join("/", route.value())).replaceAll("//+", "/");
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
                    String path = (pathPrefix + "/" + String.join("/", routeValue(ma))).replaceAll("//+", "/");
                    registerMethod(router, controller, method, path, httpMethods);
                    num++;
                    continue METHODS_LOOP;
                }
            }
        }
        return num;
    }

    private static final void registerMethod(Router router, Object controller, Method method, String path,
            HttpMethod[] httpMethods) {
        logger.debug("Register method: {}, {}, {}, {}, {}", router, controller, method, path, httpMethods);
        if (!CompletionStage.class.isAssignableFrom(method.getReturnType())) {
            throw new IllegalArgumentException("the return type must be a CompletionStage");
        }
        JsonBody jsonResposne = method.getAnnotation(JsonBody.class);
        if (jsonResposne != null) {
            // TODO
            return;
        }
        checkReturnType(method);
        Parameter[] params = method.getParameters();
        requireContext(params);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        switch (params.length) {
        case 1:
            router.add(toSimpleInvoker(controller, method), path, httpMethods);
            break;
        default:
            @SuppressWarnings("unchecked")
            Function<HttpRequestContext, Object>[] parameterMappers = Arrays.stream(params)
                    .map(ControllerBeanUtil::toParameterMapper).toArray(Function[]::new);
            Function<HttpRequestContext, Object[]> parametesMapper = toParametersMapper(parameterMappers);
            router.add(toParamsInvoker(controller, method, parametesMapper), path, httpMethods);
            break;
        }
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
                    return HttpServerUtil.sendInternalServerError(ctx, e.getTargetException());
                } catch (Exception e) {
                    return HttpServerUtil.sendInternalServerError(ctx, e);
                }
            };
        }
        return ctx -> {
            try {
                return (CompletionStage<HttpResult>) method.invoke(controller, ctx);
            } catch (InvocationTargetException e) {
                return HttpServerUtil.sendInternalServerError(ctx, e.getTargetException());
            } catch (Exception e) {
                return HttpServerUtil.sendInternalServerError(ctx, e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static final HttpServiceInvoker toParamsInvoker(Object controller, Method method,
            Function<HttpRequestContext, Object[]> parametesMapper) {
        if (Modifier.isStatic(method.getModifiers())) {
            return ctx -> {
                try {
                    return (CompletionStage<HttpResult>) method.invoke(null, parametesMapper.apply(ctx));
                } catch (BadRequestException e) {
                    logger.error("Bad request on {} {}", controller, method, e);
                    return HttpServerUtil.sendBadReqest(ctx);
                } catch (InvocationTargetException e) {
                    return HttpServerUtil.sendInternalServerError(ctx, e.getTargetException());
                } catch (Exception e) {
                    return HttpServerUtil.sendInternalServerError(ctx, e);
                }
            };
        }
        return ctx -> {
            try {
                return (CompletionStage<HttpResult>) method.invoke(controller, parametesMapper.apply(ctx));
            } catch (BadRequestException e) {
                logger.error("Bad request on {} {}", controller, method, e);
                return HttpServerUtil.sendBadReqest(ctx);
            } catch (InvocationTargetException e) {
                return HttpServerUtil.sendInternalServerError(ctx, e.getTargetException());
            } catch (Exception e) {
                return HttpServerUtil.sendInternalServerError(ctx, e);
            }
        };
    }

    private static final Function<HttpRequestContext, Object> toParameterMapper(Parameter param) {

        if (param.getType() == HttpRequestContext.class) {
            return ctx -> ctx;
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
        return null;
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
                return arrayMapper(queryVar, type, name);
            } else {
                return singleMapper(queryVar, type, name);
            }
        }
        if (List.class.isAssignableFrom(param.getType())) {
            return listMapper(queryVar, type, name);
        }
        if (Set.class.isAssignableFrom(param.getType())) {
            // TODO
        }
        if (Optional.class == param.getType()) {
            // TODO
        }
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
    }

    private static final Function<HttpRequestContext, Object> arrayMapper(QueryVar queryVar, Type type, String name) {
        Supplier<IllegalArgumentException> noSuchQueryVariable = noSuchQueryVariable(name);
        if (type == String[].class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).orElseThrow(noSuchQueryVariable).stream().toArray(String[]::new);
            } else {
                return ctx -> ctx.queryParameter(name).map(values -> values.stream().toArray(String[]::new))
                        .orElseGet(null);
            }
        } else if (type == int[].class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).orElseThrow(noSuchQueryVariable).stream()
                        .mapToInt(Integer::parseInt).toArray();
            } else {
                return ctx -> ctx.queryParameter(name)
                        .map(values -> values.stream().mapToInt(Integer::parseInt).toArray()).orElse(null);
            }
        } else if (type == long[].class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).orElseThrow(noSuchQueryVariable).stream()
                        .mapToLong(Long::parseLong).toArray();
            } else {
                return ctx -> ctx.queryParameter(name)
                        .map(values -> values.stream().mapToLong(Long::parseLong).toArray()).orElse(null);
            }
        } else if (type == Integer[].class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).orElseThrow(noSuchQueryVariable).stream().map(Integer::valueOf)
                        .toArray(Integer[]::new);
            } else {
                return ctx -> ctx.queryParameter(name)
                        .map(values -> values.stream().map(Integer::valueOf).toArray(Integer[]::new)).orElseGet(null);
            }
        } else if (type == Long[].class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).orElseThrow(noSuchQueryVariable).stream().map(Long::valueOf)
                        .toArray(Long[]::new);
            } else {
                return ctx -> ctx.queryParameter(name)
                        .map(values -> values.stream().map(Long::valueOf).toArray(Long[]::new)).orElseGet(null);
            }
        } else {
            throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
        }
    }

    private static Function<HttpRequestContext, Object> singleMapper(QueryVar queryVar, Type type, String name) {
        Supplier<IllegalArgumentException> noSuchQueryVariable = noSuchQueryVariable(name);
        if (type == String.class) {
            if (queryVar.required()) {
                return ctx -> joinStrings(ctx.queryParameter(name).orElseThrow(noSuchQueryVariable));

            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::joinStrings).orElse(null);
            }
        } else if (type == int.class || type == Integer.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Integer::valueOf)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Integer::valueOf)
                        .orElse(null);
            }
        } else if (type == long.class || type == Long.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Long::valueOf)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Long::valueOf).orElse(null);
            }
        } else if (type == double.class || type == Double.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Double::valueOf)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Double::valueOf).orElse(null);
            }
        } else if (type == boolean.class || type == Boolean.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Boolean::valueOf)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Boolean::valueOf)
                        .orElse(null);
            }
        } else if (type == byte.class || type == Byte.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Byte::valueOf)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Byte::valueOf).orElse(null);
            }
        } else if (type == short.class || type == Short.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Short::valueOf)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Short::valueOf).orElse(null);
            }
        } else if (type == float.class || type == Float.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Float::valueOf)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Float::valueOf).orElse(null);
            }
        } else if (type == BigInteger.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(BigInteger::new)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(BigInteger::new).orElse(null);
            }
        } else if (type == BigDecimal.class) {
            if (queryVar.required()) {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(BigDecimal::new)
                        .orElseThrow(noSuchQueryVariable);
            } else {
                return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(BigDecimal::new).orElse(null);
            }
        } else if (type == OptionalInt.class) {
            return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Integer::valueOf)
                    .map(OptionalInt::of).orElse(OptionalInt.empty());
        } else if (type == OptionalLong.class) {
            return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Long::valueOf)
                    .map(OptionalLong::of).orElse(OptionalLong.empty());
        } else if (type == OptionalDouble.class) {
            return ctx -> ctx.queryParameter(name).map(ControllerBeanUtil::first).map(Double::valueOf)
                    .map(OptionalDouble::of).orElse(OptionalDouble.empty());
        } else {
            throw new IllegalArgumentException("unsupported type " + type + " for @QueryVar");
        }
    }

    private static final String joinStrings(List<String> values) {
        if (values.size() == 1) {
            return first(values);
        } else {
            return String.join(",", values);
        }
    }

    private static final String first(List<String> values) {
        return values.get(0);
    }

    private static final Supplier<IllegalArgumentException> noSuchQueryVariable(String name) {
        String message = "missing path query variable " + name;
        IllegalArgumentException error = illegalArgumentExceptions.computeIfAbsent(message,
                IllegalArgumentException::new);
        return illegalArguemntSuppliers.computeIfAbsent(message, k -> () -> error);
    }

    private static final Function<HttpRequestContext, Object> listMapper(QueryVar queryVar, Type type, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    private static final Function<HttpRequestContext, Object> toJsonBodyMapper(Parameter param, JsonBody jsonBody) {
        Type type = param.getParameterizedType();
        if (type == String.class) {
            return ctx -> ctx.request().content().toString(CharsetUtil.UTF_8);
        } else if (type == byte[].class) {
            return ctx -> ByteBufUtil.getBytes(ctx.request().content());
        } else {
            return ctx -> ctx.property(JsonLibrary.KEY).orElseThrow(MISSING_JSON_LIBRARY).read(ctx.request().content(),
                    type);
        }
    }

    private static final IllegalArgumentException MISSING_JSON_LIBRARY_EXCEPTION = new IllegalArgumentException();

    private static final Supplier<IllegalArgumentException> MISSING_JSON_LIBRARY = () -> MISSING_JSON_LIBRARY_EXCEPTION;

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
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
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
