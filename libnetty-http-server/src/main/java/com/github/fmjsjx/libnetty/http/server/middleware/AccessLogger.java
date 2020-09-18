package com.github.fmjsjx.libnetty.http.server.middleware;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.CharsetUtil;

/**
 * A {@link Middleware} logging HTTP access logs.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 */
public class AccessLogger implements Middleware {

    private static final DateTimeFormatter DEFAULT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final BigDecimal T6 = BigDecimal.valueOf(1_000_000L);

    @FunctionalInterface
    public interface LoggerWrapper {

        void log(String content);

        default boolean isEnabled() {
            return true;
        }

    }

    public static final class StdoutLoggerWrapper extends FunctionalLoggerWrapper {

        private static final StdoutLoggerWrapper INSTANCE = new StdoutLoggerWrapper();

        public static final StdoutLoggerWrapper getInstance() {
            return INSTANCE;
        }

        public StdoutLoggerWrapper() {
            super(System.out::println);
        }

    }

    public static final class StderrLoggerWrapper extends FunctionalLoggerWrapper {

        private static final StderrLoggerWrapper INSTANCE = new StderrLoggerWrapper();

        public static final StderrLoggerWrapper getInstance() {
            return INSTANCE;
        }

        public StderrLoggerWrapper() {
            super(System.err::println);
        }

    }

    public static class FunctionalLoggerWrapper implements LoggerWrapper {

        private final Consumer<String> logAction;
        private final BooleanSupplier enabledChecker;

        public FunctionalLoggerWrapper(Consumer<String> logAction, BooleanSupplier enabledChecker) {
            this.logAction = Objects.requireNonNull(logAction, "logAction must not be null");
            this.enabledChecker = Objects.requireNonNull(enabledChecker, "enabledChecker must not be null");
        }

        public FunctionalLoggerWrapper(Consumer<String> logAction) {
            this(logAction, Boolean.TRUE::booleanValue);
        }

        @Override
        public void log(String content) {
            logAction.accept(content);
        }

        @Override
        public boolean isEnabled() {
            return enabledChecker.getAsBoolean();
        }

    }

    public static final class Slf4jLoggerWrapper extends FunctionalLoggerWrapper {

        private static final Consumer<String> logAction(Logger logger, Level level) {
            Objects.requireNonNull(logger, "logger must not be null");
            Objects.requireNonNull(level, "level must not be null");
            switch (level) {
            case DEBUG:
                return logger::debug;
            case ERROR:
                return logger::error;
            case INFO:
                return logger::info;
            case TRACE:
                return logger::trace;
            case WARN:
                return logger::warn;
            default:
                // can't reach this line
                return logger::info;
            }
        }

        private static final BooleanSupplier enabledChecker(Logger logger, Level level) {
            Objects.requireNonNull(logger, "logger must not be null");
            Objects.requireNonNull(level, "level must not be null");
            switch (level) {
            case DEBUG:
                return logger::isDebugEnabled;
            case ERROR:
                return logger::isErrorEnabled;
            case INFO:
                return logger::isInfoEnabled;
            case TRACE:
                return logger::isTraceEnabled;
            case WARN:
                return logger::isWarnEnabled;
            default:
                // can't reach this line
                return logger::isInfoEnabled;
            }
        }

        public Slf4jLoggerWrapper(Logger logger, Level level) {
            super(logAction(logger, level), enabledChecker(logger, level));
        }

        public Slf4jLoggerWrapper(Logger logger) {
            this(logger, Level.INFO);
        }

        public Slf4jLoggerWrapper(String name, Level level) {
            this(LoggerFactory.getLogger(name), level);
        }

        public Slf4jLoggerWrapper(String name) {
            this(name, Level.INFO);
        }

    }

    /**
     * Some pre-defined log formats.
     * 
     * @since 1.1
     *
     * @author MJ Fang
     */
    public enum LogFormat {

        /**
         * The minimal output.
         * 
         * <pre>
         * :method :path :status :result-length - :response-time ms
         * </pre>
         */
        TINY(":method :path :status :result-length - :response-time ms"),

        /**
         * Shorter than default, also including response time.
         * 
         * <pre>
         * :remote-addr :remote-user :method :path :http-version :status :result-length - :response-time ms
         * </pre>
         */
        SHORT(":remote-addr :remote-user :method :path :http-version :status :result-length - :response-time ms"),

        /**
         * Concise output colored by response status for development use. The :status
         * token will be colored green for success codes, red for server error codes,
         * yellow for client error codes, cyan for redirection codes, and uncolored for
         * information codes.
         * 
         * <pre>
         * :method :path :status :response-time ms - :result-length
         * </pre>
         */
        DEV(":method :path :status :response-time ms - :result-length"),

        /**
         * Standard Apache common log output.
         * 
         * <pre>
         * :remote-addr - :remote-user [:datetime] \":method :path :http-version\" :status :result-length
         * </pre>
         */
        COMMON(":remote-addr - :remote-user [:datetime] \":method :path :http-version\" :status :result-length"),

        /**
         * Standard Apache combined log output.
         * 
         * <pre>
         * :remote-addr - :remote-user [:datetime] ":method :path :http-version" :status :result-length ":referrer" ":user-agent"
         * </pre>
         */
        COMBINED(
                ":remote-addr - :remote-user [:datetime] \":method :path :http-version\" :status :result-length \":referrer\" \":user-agent\""),

        /**
         * Basic log output.
         * 
         * <pre>
         * :datetime :method :path :http-version :remote-addr - :status :response-time ms :result-length
         * </pre>
         */
        BASIC(":datetime :method :path :http-version :remote-addr - :status :response-time ms :result-length"),
        
        /**
         * Another basic log output, make result length human readable.
         * 
         * <pre>
         * :datetime :method :path :http-version :remote-addr - :status :response-time ms :result-length-humanreadable
         * </pre>
         */
        BASIC2(":datetime :method :path :http-version :remote-addr - :status :response-time ms :result-length-humanreadable");

        private final String pattern;

        private LogFormat(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return name().toLowerCase() + "(" + pattern + ")";
        }

    }

    private static final Pattern SYMBOL_PATTERN = Pattern.compile(":[0-9a-z\\-]+");

    private static final Function<HttpResult, String> generateMapperFromPattern(String pattern) {
        Matcher m = SYMBOL_PATTERN.matcher(pattern);
        List<String> txts = new ArrayList<String>();
        List<String> symbols = new ArrayList<String>();
        int start = 0;
        for (; m.find(start); start = m.end()) {
            symbols.add(m.group());
            if (m.start() > start) {
                txts.add(pattern.substring(start, m.start()));
            } else {
                txts.add("");
            }
        }
        if (symbols.isEmpty()) {
            return r -> pattern;
        }
        String[] atxt = txts.toArray(new String[txts.size()]);
        @SuppressWarnings("unchecked")
        Function<HttpResult, Object>[] symbolMappers = symbols.stream().map(AccessLogger::symbolMapper)
                .toArray(Function[]::new);
        if (pattern.length() > start) {
            String lastTxt = pattern.substring(start);
            return r -> {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < symbolMappers.length; i++) {
                    builder.append(atxt[i]).append(symbolMappers[i].apply(r));
                }
                return builder.append(lastTxt).toString();
            };
        } else {
            return r -> {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < symbolMappers.length; i++) {
                    builder.append(atxt[i]).append(symbolMappers[i].apply(r));
                }
                return builder.toString();
            };
        }
    }

    private static final Function<HttpResult, Object> symbolMapper(String symbol) {
        switch (symbol) {
        case ":version":
        case ":http-version":
            return result -> result.requestContext().version();
        case ":method":
        case ":http-method":
            return result -> result.requestContext().method();
        case ":url":
        case ":path":
            return result -> result.requestContext().path();
        case ":raw-path":
            return result -> result.requestContext().rawPath();
        case ":query":
            return result -> result.requestContext().rawQuery();
        case ":host":
            return result -> result.requestContext().headers().get(HttpHeaderNames.HOST, "-");
        case ":content-length":
            return result -> result.requestContext().contentLength();
        case ":content":
            return result -> {
                try {
                    return result.requestContext().body().toString(CharsetUtil.UTF_8);
                } catch (Exception e) {
                    // ByteBuf may be released before
                    return "-";
                }
            };
        case ":content-type":
            return result -> result.requestContext().contentType().orElse("-");
        case ":remote-addr":
        case ":remote-address":
            return result -> result.requestContext().remoteAddress();
        case ":remote-user":
            return result -> {
                String auth = result.requestContext().headers().get(HttpHeaderNames.AUTHORIZATION);
                if (auth == null || !auth.startsWith("Basic ")) {
                    return "-";
                }
                String base64 = auth.substring(6);
                String basic = new String(Base64.getDecoder().decode(base64.getBytes(CharsetUtil.UTF_8)),
                        CharsetUtil.UTF_8);
                return basic.split(":")[0];
            };
        case ":user-agent":
            return result -> result.requestContext().headers().get(HttpHeaderNames.USER_AGENT, "-");
        case ":referrer":
            return result -> result.requestContext().headers().get(HttpHeaderNames.REFERER, "-");
        case ":accept":
            return result -> result.requestContext().headers().get(HttpHeaderNames.ACCEPT, "-");
        case ":status":
            return result -> result.responseStatus();
        case ":status-code":
            return result -> result.responseStatus().codeAsText();
        case ":status-reason":
            return result -> result.responseStatus().reasonPhrase();
        case ":result-length":
            return HttpResult::resultLength;
        case ":result-length-humanreadable":
            return result -> toHumanReadableSize(result.resultLength());
        case ":iso-local-datetime":
            return result -> result.respondedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        case ":datetime":
            return result -> result.respondedTime().format(DEFAULT_DATE_TIME);
        case ":iso-local-date":
            return result -> result.respondedTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        case ":basic-iso-date":
            return result -> result.respondedTime().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        case ":iso-local-time":
            return result -> result.respondedTime().toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
        case ":response-time":
            return result -> BigDecimal.valueOf(result.nanoUsed()).divide(T6, 3, RoundingMode.HALF_EVEN);
        default:
            return result -> symbol;
        }
    }

    private static final String toHumanReadableSize(long length) {
        if (length < 1024) {
            return Long.toString(length);
        } else if (length < 1024 * 10) {
            return String.format("%.2fK", length / 1024.0);
        } else if (length < 1024 * 100) {
            return String.format("%.fK", length / 1024.0);
        } else if (length < 1024 * 1024) {
            return (length / 1024) + "K";
        } else if (length < 1024 * 1024 * 10) {
            return String.format("%.2fM", length / (1024 * 1024.0));
        } else if (length < 1024 * 1024 * 100) {
            return String.format("%.1fM", length / (1024 * 1024.0));
        } else if (length < 1024 * 1024 * 1024) {
            return (length / (1024 * 1024)) + "M";
        } else if (length < 1024 * 1024 * 1024 * 10) {
            return String.format("%.2fG", length / (1024 * 1024 * 1024.0));
        } else if (length < 1024 * 1024 * 1024 * 100) {
            return String.format("%.1fG", length / (1024 * 1024 * 1024.0));
        } else {
            return (length / (1024 * 1024 * 1024)) + "G";
        }
    }

    private final LoggerWrapper loggerWrapper;
    private final Function<HttpResult, String> logMapper;

    public AccessLogger() {
        this(LogFormat.BASIC);
    }

    public AccessLogger(LogFormat format) {
        this(StdoutLoggerWrapper.INSTANCE, format);
    }

    public AccessLogger(LoggerWrapper loggerWrapper, LogFormat format) {
        this(loggerWrapper, Objects.requireNonNull(format, "format must not be null").pattern);
    }

    public AccessLogger(String pattern) {
        this(StdoutLoggerWrapper.INSTANCE, pattern);
    }

    public AccessLogger(LoggerWrapper loggerWrapper, String pattern) {
        this(loggerWrapper, generateMapperFromPattern(pattern));
    }

    private AccessLogger(LoggerWrapper loggerWrapper, Function<HttpResult, String> logMapper) {
        this.loggerWrapper = loggerWrapper;
        this.logMapper = logMapper;
    }

    String mapLog(HttpResult result) {
        return logMapper.apply(result);
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        return next.doNext(ctx).whenComplete((r, e) -> {
            if (e == null && loggerWrapper.isEnabled()) {
                loggerWrapper.log(mapLog(r));
            }
        });
    }

}
