package com.github.fmjsjx.libnetty.http.server.middleware;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;

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

import io.netty.handler.codec.http.HttpMethod;

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
        switch (params.length) {
        case 0:
            throw new IllegalArgumentException("missing parameter as type HttpRequestContext");
        case 1:
            if (params[0].getType() != HttpRequestContext.class) {
                throw new IllegalArgumentException("missing parameter as type HttpRequestContext");
            }
            router.add(toSimpleInvoker(controller, method), path, httpMethods);
            break;
        default:
            // TODO
            break;
        }
    }

    private static final SimpleInvoker toSimpleInvoker(Object controller, Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return ctx -> method.invoke(null, ctx);
        }
        return ctx -> method.invoke(controller, ctx);
    }

    @FunctionalInterface
    private interface SimpleInvoker extends HttpServiceInvoker {

        Object invoke0(HttpRequestContext ctx) throws Exception;

        @SuppressWarnings("unchecked")
        @Override
        default CompletionStage<HttpResult> invoke(HttpRequestContext ctx) {
            try {
                return (CompletionStage<HttpResult>) invoke0(ctx);
            } catch (Exception e) {
                if (e instanceof InvocationTargetException) {
                    // ignore
                    return HttpServerUtil.sendInternalServerError(ctx,
                            ((InvocationTargetException) e).getTargetException());
                }
                // ignore
                return HttpServerUtil.sendInternalServerError(ctx, e);
            }
        }

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
